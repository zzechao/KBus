package com.zzc.kbus

import android.app.Application

object KInitializer {
    lateinit var application: Application

    var logger: ILogger? = null

    fun init(application: Application, logger: ILogger? = null) {
        KInitializer.application = application
        this.logger = logger
    }
}