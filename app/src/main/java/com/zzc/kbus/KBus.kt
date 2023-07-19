package com.zzc.kbus

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

object KBus {

    private const val TAG = "KBus"

    private val mSlyCenter: ConcurrentHashMap<Class<*>, MutableList<Job>> = ConcurrentHashMap()
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
                    val job =
                        ApplicationScopeViewModelProvider.getApplicationScopeViewModel(KCore::class.java)
                            .observeWithoutLifecycle<KMessage>(
                                CoroutineScope(Dispatchers.Main),
                                it.parameterTypes[0].name,
                                annotation.schedulerModel,
                                annotation.sticky,
                                annotation.delay
                            ) { event ->
                                Log.i(
                                    TAG,
                                    "event = $event， $it, ${annotation.delay}, ${annotation.schedulerModel}"
                                )
                                it.invoke(observer, event)
                            }
                    val jobList = mSlyCenter[observer.javaClass] ?: mutableListOf<Job>()
                    jobList.add(job)
                    mSlyCenter[observer.javaClass] = jobList
                    Log.i(TAG, "job = $job, ${observer.javaClass}, ${it.parameterTypes[0].name}")
                }
            }
        return true
    }

    fun unSubscribe(observer: Any): Boolean {
        mSlyCenter[observer.javaClass]?.forEach {
            Log.i(TAG, "${observer.javaClass.name} cancel job = $it")
            it.cancel()
        }
        mSlyCenter.remove(observer.javaClass)
        return true
    }

    fun setSubscribeInterceptor(interceptor: SubscribeInterceptor) {
        mInterceptor = interceptor
    }

    fun postMessage(message: KMessage) {
        postEvent(message.javaClass.name, message)
    }

    private fun postEvent(eventName: String, value: Any, timeMillis: Long = 0) {
        KInitializer.logger?.log(Level.INFO, TAG, "postEvent = $eventName，$value, $timeMillis")
        ApplicationScopeViewModelProvider.getApplicationScopeViewModel(KCore::class.java)
            .postEvent(eventName, value, timeMillis)
    }

    fun postStickyEvent(eventName: String, value: Any, timeMillis: Long = 0) {
        Log.i(TAG, "postStickyEvent = $eventName，$value, $timeMillis")
        ApplicationScopeViewModelProvider.getApplicationScopeViewModel(KCore::class.java)
            .postEvent(eventName, value, timeMillis, true)
    }
}