package com.ysj.lib.bcu.modifier.component.di.api

/**
 * 使用该注解标识一个 Component Implementation。
 *
 * - 注解的类必须实现了 [Component] 注解的接口。
 *
 * @author Ysj
 * Create time: 2023/8/28
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ComponentImpl
