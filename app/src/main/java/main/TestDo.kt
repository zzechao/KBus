package main

import android.util.Log
import com.zzc.kbus.KBus
import com.zzc.kbus.KBusContext
import com.zzc.kbus.SchedulerModel

class TestDo {

    private var isSubscribe = false

    fun subscribe() {
        if (isSubscribe) {
            isSubscribe = false
            unSubscribe()
        } else {
            isSubscribe = true
            KBus.subscribe(this)
        }
    }

    private fun unSubscribe() {
        KBus.unSubscribe(this)
    }

    @KBusContext(
        schedulerModel = SchedulerModel.Main,
        sticky = true
    )
    fun testMsg(event: TestEvent) {
        Log.i("ttt", "TestDo ${Thread.currentThread().name} testMsg:${event.test}")
    }
}