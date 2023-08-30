@file:JvmName("Utils")

package com.ysj.lib.bcu.modifier.component.di.api

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/*
 * 一些通用工具。
 *
 * @author Ysj
 * Create time: 2023/8/28
 */

private val cpiProxyHandler = InvocationHandler { _, _, _ ->
    throw IllegalStateException("your component interface not implementation")
}

@Suppress("UNCHECKED_CAST")
internal fun <T> cpiProxy(clazz: Class<T>): T = Proxy.newProxyInstance(
    clazz.classLoader,
    arrayOf(clazz),
    cpiProxyHandler,
) as T
