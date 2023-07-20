package com.zzc.kbus

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KBusContext(
    val schedulerModel: SchedulerModel = SchedulerModel.Origin,
    val delay: Long = 0,
    val sticky: Boolean = false
)

data class KBusContextData(
    var schedulerModel: SchedulerModel = SchedulerModel.Origin,
    var delay: Long = 0,
    var sticky: Boolean = false
)

enum class SchedulerModel {
    /**
     * 主线程执行，如果当前在主线程则马上执行，否则post回主线程执行
     */
    Main,

    /**
     * post回主线程执行，如果当前在主线程则会延迟到下一次looper才执行
     */
    MainPost,

    /**
     * 异步线程并发执行
     */
    Async,

    /**
     * 异步线程有序执行
     */
    AsyncOrder,

    /**
     * 发通知的当前线程执行 若制定有生命周期则忽略制定的生命周期阶段声明
     */
    Origin
}