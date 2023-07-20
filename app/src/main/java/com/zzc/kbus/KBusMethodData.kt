package com.zzc.kbus

import java.lang.reflect.Method

data class KBusMethodData(
    val event: Class<out KMessage>,
    val clazz: Class<*>, val invokeMethod: Method,
    val context: KBusContextData
)