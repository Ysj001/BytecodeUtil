package com.ysj.lib.bcu.modifier.aspect.api

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * 方法连接点。该对象会复用，不要储存
 *
 * @author Ysj
 * Create time: 2021/3/8
 */
class JoinPoint(
    target: Any?,
    args: Array<Any?>,
) : Serializable, Cloneable {
    companion object {
        private val STORE = ConcurrentHashMap<String, JoinPoint>()

        private val sPool = SynchronizedPool<JoinPoint>(50)

        @JvmStatic
        fun put(key: String, target: Any?, args: Array<Any?>) {
            STORE["${Thread.currentThread().id}-$key"] = sPool.obtain()?.also {
                it.target = target
                it.args = args
            } ?: JoinPoint(target, args)
        }

        @JvmStatic
        fun remove(key: String) {
            STORE.remove("${Thread.currentThread().id}-$key")?.run {
                target = null
                args = EMPTY_ARRAY
                sPool.recycle(this)
            }
        }

        @JvmStatic
        fun get(key: String) = STORE["${Thread.currentThread().id}-$key"]
    }

    /** 切入点的 this 获取的对象，若切入点在静态方法内，则为 null */
    var target: Any? = target
        private set

    /** 切入点方法的参数 */
    var args: Array<Any?> = args
        private set

    public override fun clone(): JoinPoint = super.clone() as JoinPoint
}