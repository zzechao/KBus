package main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.kbus.R
import com.zzc.kbus.ILogger
import com.zzc.kbus.KBus
import com.zzc.kbus.KBusContext
import com.zzc.kbus.KInitializer
import com.zzc.kbus.SchedulerModel
import java.util.logging.Level

class MainActivity : AppCompatActivity() {

    private val testDo by lazy {
        TestDo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KInitializer.init(this.application, object : ILogger {
            override fun log(level: Level, tag: String, msg: String) {
                Log.i("ttt", "${Thread.currentThread().name} msg:$msg")
            }
        })

        KBus.postStickyEvent(TestEvent("test"))
        KBus.subscribe(this)
        testDo.subscribe()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.bt_do).setOnClickListener {
            testDo.subscribe()
            //KBus.postStickyEvent(TestEvent("test"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        KBus.unSubscribe(this)
    }

    @KBusContext(
        schedulerModel = SchedulerModel.Async,
        sticky = true
    )
    fun testMsg(event: TestEvent) {
        Log.i("ttt", "${Thread.currentThread().name} testMsg:${event.test}")
    }
}