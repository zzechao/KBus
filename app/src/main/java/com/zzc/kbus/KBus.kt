package com.zzc.kbus

import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object KBus {

    private const val TAG = "ttt"


    private val mSlyCenter: ConcurrentHashMap<Class<*>, MutableList<KBusMethodData>> =
        ConcurrentHashMap()
    private val eventObjectCenter: ConcurrentHashMap<Class<out KMessage>,
            MutableList<Class<*>>> = ConcurrentHashMap()
    private val mEventObjects = ConcurrentHashMap<Class<*>, Any>()

    private val stickyEvent = ConcurrentHashMap<Class<out KMessage>, KMessage>()

    private var mInterceptor: SubscribeInterceptor? = null

    fun subscribe(observer: Any): Boolean {
        if (mInterceptor?.interceptor(observer) == true) {
            return false
        }
        if (mSlyCenter.containsKey(observer.javaClass)) {
            return false
        }
        observer.javaClass.methods
            .filter { it.getAnnotation(KBusContext::class.java) != null }
            .forEach {
                val annotation = it.annotations.firstOrNull { annotation ->
                    annotation is KBusContext
                }
                if (it.parameterTypes.isNotEmpty() && annotation is KBusContext) {
                    val busContextData = KBusContextData(
                        annotation.schedulerModel,
                        annotation.delay,
                        annotation.sticky
                    )
                    val event = it.parameterTypes[0]
                    if (event.interfaces.firstOrNull { it == KMessage::class.java } != null) {
                        val observerClazz = observer.javaClass
                        val busMethodData =
                            KBusMethodData(
                                event as Class<out KMessage>,
                                observerClazz,
                                it,
                                busContextData
                            )
                        mSlyCenter[observerClazz]?.add(busMethodData) ?: kotlin.run {
                            mSlyCenter[observerClazz] = mutableListOf(busMethodData)
                        }
                        eventObjectCenter[event]?.add(observerClazz) ?: kotlin.run {
                            eventObjectCenter[event] = mutableListOf(observerClazz)
                        }
                        mEventObjects[observerClazz] = observer
                        Log.i(
                            TAG,
                            "event = $event, observerClazz:${observerClazz}, ${it.parameterTypes[0].name}"
                        )
                        if (annotation.sticky && stickyEvent[event] != null) {
                            stickyEvent[event]?.let { it1 -> postEvent(it1) }
                        }
                    }
                }
            }
        return true
    }

    fun unSubscribe(observer: Any): Boolean {
        mSlyCenter[observer.javaClass]?.forEach {
            Log.i(TAG, "unSubscribe event:${it.event} clazz:${observer.javaClass}")
            eventObjectCenter[it.event]?.remove(observer.javaClass)
        }
        mSlyCenter.remove(observer.javaClass)
        mEventObjects.remove(observer.javaClass)
        return true
    }

    fun setSubscribeInterceptor(interceptor: SubscribeInterceptor) {
        mInterceptor = interceptor
    }

    /**
     * 绑定粘性事件
     */
    fun postStickyEvent(message: KMessage) {
        postEvent(message)
        Log.i(TAG, "postStickyEvent ${message.javaClass}")
        stickyEvent[message.javaClass] = message
    }

    fun postEvent(message: KMessage) {
        eventObjectCenter[message::class.java]?.let {
            it.forEach {
                val observer = mEventObjects[it]
                val busContextData = mSlyCenter[it]?.firstOrNull { it.event == message::class.java }
                Log.i(TAG, "postMessage clazz:$it object:$observer busContext:$busContextData")
                if (observer != null && busContextData != null) {
                    val delay = busContextData.context.delay
                    val suspendBlock = suspend {
                        if (delay > 0) {
                            delay(delay)
                        }
                        busContextData.invokeMethod.invoke(observer, message)
                    }
                    val block = {
                        if (delay > 0) {
                            Thread.sleep(delay)
                        }
                        busContextData.invokeMethod.invoke(observer, message)
                    }
                    when (busContextData.context.schedulerModel) {
                        SchedulerModel.Async -> GlobalScope.launch(Dispatchers.IO) {
                            suspendBlock()
                        }

                        SchedulerModel.AsyncOrder -> GlobalScope.launch(asyncOrderDispatcher) {
                            suspendBlock()
                        }

                        SchedulerModel.Main -> {
                            if (isMainThread()) {
                                block()
                            } else {
                                GlobalScope.launch(Dispatchers.Main) {
                                    suspendBlock()
                                }
                            }
                        }

                        SchedulerModel.MainPost -> GlobalScope.launch(Dispatchers.Main) {
                            suspendBlock()
                        }

                        SchedulerModel.Origin -> {
                            block()
                        }
                    }
                }
            }
        }
    }

    /**
     * 清除粘性事件
     */
    fun clearStickyEvent(clazz: Class<out KMessage>) {
        stickyEvent.remove(clazz)
    }

    private val asyncOrderDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    private fun isMainThread(): Boolean {
        return Thread.currentThread() == Looper.getMainLooper().thread
    }
}