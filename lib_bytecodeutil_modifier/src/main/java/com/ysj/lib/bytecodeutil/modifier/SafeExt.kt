package com.ysj.lib.bytecodeutil.modifier

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

/*
 * 线程安全的修改操作
 *
 * @author Ysj
 * Create time: 2021/8/28
 */

fun <T : Any> T.lock(block: T.() -> Unit) = synchronized(this) { block() }

fun Executor.exec(latch: CountDownLatch, onError: (Throwable) -> Unit, block: () -> Unit) =
    execute {
        try {
            block()
            latch.countDown()
        } catch (e: Throwable) {
            onError(e)
            while (latch.count > 0) latch.countDown()
        }
    }