package com.zzc.kbus

import java.util.logging.Level

interface ILogger {
    fun log(level: Level, msg: String) {}
    fun log(level: Level, msg: String, th: Throwable) {}
}