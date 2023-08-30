package com.ysj.lib.bcu.modifier.component.di.api

/**
 * 使用该注解标识一个 Component Programing Interface。
 *
 * - 默认配置下编译时会检查该注解是否被 [ComponentImpl] 注解的类实现。
 * - 如果想关闭编译时检查，可以在 gradle 的 project 的 properties 中添加 component.di.checkImpl=false。
 *
 * @author Ysj
 * Create time: 2023/8/28
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Component