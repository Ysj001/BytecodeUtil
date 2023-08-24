package com.ysj.lib.bcu.modifier.aspect.api

import java.io.Serializable
import java.lang.reflect.Method

/**
 * 方法调用点。该对象会复用，不要储存
 *
 * @author Ysj
 * Create time: 2021/7/21
 */
class CallingPoint(
    caller: Any,
    /** 是否是静态方法 */
    val isStatic: Boolean,
    /** 调用的方法 */
    val method: Method,
    args: Array<Any?>,
) : Serializable, Cloneable {

    companion object {
        /** 最大缓存大小，注意只有使用前设置有效 */
        @Volatile
        var MAX_CACHE_SIZE = 16

        private val sThreadLocal = object : ThreadLocal<LruCache<Int, CallingPoint>>() {
            override fun initialValue() = LruCache<Int, CallingPoint>(MAX_CACHE_SIZE)
        }

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
            cacheKey = 31 * cacheKey + parameterTypes.contentHashCode()
            val cache = sThreadLocal.get()
            cache[cacheKey]?.let {
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
                    var clazz: Class<out Any>? = this
                    var method: Method? = null
                    while (clazz != null && method == null) try {
                        method = clazz.getDeclaredMethod(funName, *parameterTypes)
                    } catch (e: Exception) {
                        clazz = clazz.superclass
                    }
                    method!!.apply { isAccessible = true }
                },
                args
            ).also { cache[cacheKey] = it }
        }
    }

    /** 调用方法的对象，若是静态方法，则是 [Class] 类型 */
    var caller: Any = caller
        private set

    /** 调用该方法的参数 */
    var args: Array<Any?> = args
        private set

    /** 多级代理嵌套时的最终代理目标的 [CallingPoint] */
    @JvmField
    var orgCallingPoint: CallingPoint? = null

    public override fun clone(): CallingPoint = super.clone() as CallingPoint

    /**
     * 调用代理的方法
     */
    fun call() = method.invoke(if (isStatic) null else caller, *args)

    /**
     * 调用多级代理嵌套时的最终代理目标的方法
     */
    fun callOrg() = orgCallingPoint?.call()

    /**
     * 获取代理的最终目标方法上的注解
     */
    fun <T : Annotation> annotation(clazz: Class<T>): T? =
        orgCallingPoint?.annotation(clazz) ?: method.getAnnotation(clazz)

    /**
     * 释放引用。会自动调用，不用手动调
     */
    fun release() {
        caller = EMPTY_OBJ
        args = EMPTY_ARRAY
        orgCallingPoint = null
    }
}