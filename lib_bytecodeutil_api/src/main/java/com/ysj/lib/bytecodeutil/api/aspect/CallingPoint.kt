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
    /** 方法调用对象 */
    val caller: Any?,
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
            if (isStatic) null else caller,
            (if (isStatic) caller as Class<*> else caller.javaClass).getMethod(funName, *parameterTypes),
            args
        )
    }

    /**
     * 调用原方法
     */
    fun call() = method.invoke(caller, *args)
}