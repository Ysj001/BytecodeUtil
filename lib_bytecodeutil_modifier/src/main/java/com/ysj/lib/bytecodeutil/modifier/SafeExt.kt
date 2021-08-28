package com.ysj.lib.bytecodeutil.modifier

/*
 * 线程安全的修改操作
 *
 * @author Ysj
 * Create time: 2021/8/28
 */

fun <T : Any> T.lock(block: T.() -> Unit) = synchronized(this) { block() }
