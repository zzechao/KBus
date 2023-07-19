package com.zzc.kbus

import java.util.logging.Level

interface ILogger {
    fun log(level: Level = Level.INFO, tag: String = "", msg: String) {}
    fun log(level: Level, tag: String = "", msg: String, th: Throwable) {}
}