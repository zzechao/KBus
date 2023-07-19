package com.zzc.kbus

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.whenStateAtLeast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.logging.Level

class KCore : ViewModel() {

    //正常事件
    private val eventFlows: HashMap<String, MutableSharedFlow<Any>> = HashMap()

    //粘性事件
    private val stickyEventFlows: HashMap<String, MutableSharedFlow<Any>> = HashMap()

    private fun getEventFlow(eventName: String, isSticky: Boolean): MutableSharedFlow<Any> {
        return if (isSticky) {
            stickyEventFlows[eventName]
        } else {
            eventFlows[eventName]
        } ?: MutableSharedFlow<Any>(
            replay = if (isSticky) 1 else 0,
            extraBufferCapacity = Int.MAX_VALUE
        ).also {
            if (isSticky) {
                stickyEventFlows[eventName] = it
            } else {
                eventFlows[eventName] = it
            }
        }
    }

    fun <T : Any> observeEvent(
        lifecycleOwner: LifecycleOwner,
        eventName: String,
        minState: Lifecycle.State,
        dispatcher: CoroutineDispatcher,
        isSticky: Boolean,
        onReceived: (T) -> Unit
    ): Job {
        KInitializer.logger?.log(Level.WARNING, "observe Event:$eventName")
        return lifecycleOwner.lifecycleScope.launch{
            lifecycleOwner.lifecycle.whenStateAtLeast(minState) {
                getEventFlow(eventName, isSticky).collect { value ->
                    this.launch(dispatcher) {
                        invokeReceived(value, onReceived)
                    }
                }
            }
        }
    }

    fun <T : Any> observeWithoutLifecycle(
        coroutineScope: CoroutineScope,
        eventName: String,
        dispatcher: CoroutineDispatcher,
        isSticky: Boolean,
        delayTime: Long,
        onReceived: (T) -> Unit
    ): Job {
        return coroutineScope.launch {
            getEventFlow(eventName, isSticky).collect { value ->
                this.launch(dispatcher) {
                    delay(delayTime)
                    invokeReceived(value, onReceived)
                }
            }
        }
    }

    suspend fun <T : Any> observeWithoutLifecycle(
        eventName: String,
        isSticky: Boolean,
        onReceived: (T) -> Unit
    ) {
        getEventFlow(eventName, isSticky).collect { value ->
            invokeReceived(value, onReceived)
        }
    }

    fun postEvent(eventName: String, value: Any, timeMillis: Long = 0, isSticky: Boolean = false) {
        KInitializer.logger?.log(Level.WARNING, "post Event:$eventName, $isSticky")
        viewModelScope.launch {
            delay(timeMillis)
            getEventFlow(eventName, isSticky).emit(value)
        }
    }

    fun removeStickEvent(eventName: String) {
        stickyEventFlows.remove(eventName)
    }

    fun clearStickEvent(eventName: String) {
        stickyEventFlows[eventName]?.resetReplayCache()
    }

    private fun <T : Any> invokeReceived(value: Any, onReceived: (T) -> Unit) {
        try {
            onReceived.invoke(value as T)
        } catch (e: ClassCastException) {
            KInitializer.logger?.log(
                Level.WARNING,
                "class cast error on message received: $value",
                e
            )
        } catch (e: Exception) {
            KInitializer.logger?.log(
                Level.WARNING,
                "error on message received: $value",
                e
            )
        }
    }

    fun getEventObserverCount(eventName: String): Int {
        val stickyObserverCount = stickyEventFlows[eventName]?.subscriptionCount?.value ?: 0
        val normalObserverCount = eventFlows[eventName]?.subscriptionCount?.value ?: 0
        return stickyObserverCount + normalObserverCount
    }
}