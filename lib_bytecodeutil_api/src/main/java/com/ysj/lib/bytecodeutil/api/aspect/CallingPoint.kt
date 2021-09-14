package com.ysj.lib.bytecodeutil.api.aspect

import com.ysj.lib.bytecodeutil.api.util.LruCache
import java.io.Serializable
import java.lang.reflect.Method

/**
 * 方法调用点。该对象会复用，不要储存
 *
 * @author Ysj
 * Create time: 2021/7/21
 */
class CallingPoint private constructor(
    caller: Any,
    /** 是否是静态方法 */
    val isStatic: Boolean,
    /** 调用的方法 */
    val method: Method,
    args: Array<Any?>,
) : Serializable {

    companion object {
        private val DEFAULT_CALLER = Any()

        private val CACHE = LruCache.sync<Int, CallingPoint>()

        @JvmStatic
        fun newInstance(
            caller: Any,
            isStatic: Boolean,
            funName: String,
            parameterTypes: Array<Class<*>>,
            args: Array<Any?>,
        ): CallingPoint = (if (isStatic) caller as Class<*> else caller.javaClass).run {
            var cacheKey = hashCode()
            cacheKey = 31 * cacheKey + isStatic.hashCode()
            cacheKey = 31 * cacheKey + funName.hashCode()
            cacheKey = 31 * cacheKey + parameterTypes.contentDeepHashCode()
            CACHE[cacheKey]?.let {
                val method = it.method
                if (funName != method.name || !method.parameterTypes.contentEquals(parameterTypes)) null
                else {
                    it.caller = caller
                    it.args = args
                    it
                }
            } ?: CallingPoint(
                caller,
                isStatic,
                try {
                    getMethod(funName, *parameterTypes)
                } catch (e: Exception) {
                    getDeclaredMethod(funName, *parameterTypes).apply { isAccessible = true }
                },
                args
            ).also { CACHE[cacheKey] = it }
        }
    }

    /** 调用方法的对象，若是静态方法，则是 [Class] 类型 */
    var caller: Any = caller
        private set

    /** 调用该方法的参数 */
    var args: Array<Any?> = args
        private set

    /**
     * 调用原方法
     */
    fun call() = method.invoke(if (isStatic) null else caller, *args)

    /**
     * 释放引用。会自动调用，不用手动调
     */
    fun release() {
        caller = DEFAULT_CALLER
        for (i in 0..args.lastIndex) args[i] = null
    }
}