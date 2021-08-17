package com.ysj.lib.bytecodeutil.api.aspect

import java.io.Serializable
import java.lang.reflect.Method

/**
 * 方法调用点
 *
 * @author Ysj
 * Create time: 2021/7/21
 */
class CallingPoint(
    /** 调用方法的对象，若是静态方法，则是 [Class] 类型 */
    val caller: Any,
    /** 是否是静态方法 */
    val isStatic: Boolean,
    /** 调用的方法 */
    val method: Method,
    /** 调用该方法的参数 */
    val args: Array<Any?>,
) : Serializable {
    companion object {
        @JvmStatic
        fun newInstance(
            caller: Any,
            isStatic: Boolean,
            funName: String,
            parameterTypes: Array<Class<*>>,
            args: Array<Any?>,
        ) = CallingPoint(
            caller,
            isStatic,
            (if (isStatic) caller as Class<*> else caller.javaClass).getMethod(funName, *parameterTypes),
            args
        )
    }

    /**
     * 调用原方法
     */
    fun call() = method.invoke(if (isStatic) null else caller, *args)
}