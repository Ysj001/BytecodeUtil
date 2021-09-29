package com.ysj.lib.simpleaop

/**
 * 使用该注解标识的方法间隔指定时间后才能再次触发
 *
 * @author Ysj
 * Create time: 2021/9/18
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntervalTrigger(
    val intervalMS: Long = 1000
)
