package com.zzc.kbus

import android.util.Log
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

object KBus {

    private val mSlyCenter: ConcurrentHashMap<Class<*>, MutableList<Job>> = ConcurrentHashMap()
    private var mInterceptor: SubscribeInterceptor? = null

    fun subscribe(observer: Any): Boolean {
       return true
    }
}