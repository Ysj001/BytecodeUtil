package com.ysj.demo.aspect.click

/**
 * 使用该注解标识的方法间隔指定时间后才能再次触发
 *
 * @author Ysj
 * Create time: 2021/9/18
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClickInterval(
    /**
     * 默认间隔时间。
     */
    val intervalMs: Long = 1000,
)
