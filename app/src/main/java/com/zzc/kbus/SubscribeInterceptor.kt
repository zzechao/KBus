package com.zzc.kbus

interface SubscribeInterceptor {
    fun interceptor(observer: Any): Boolean
}