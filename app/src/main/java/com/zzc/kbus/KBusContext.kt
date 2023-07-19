package com.zzc.kbus

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KBusContext(
    val schedulerModel: Int = SchedulerModel.main,
    val delay: Long = 0,
    val sticky: Boolean = false
)

object SchedulerModel {

    /**
     * 主线程调用
     */
    const val main = 0

    /**
     * 子线程调用
     */
    const val io = 1

    /**
     * 当前线程同步调用
     */
    const val sync = 2
}