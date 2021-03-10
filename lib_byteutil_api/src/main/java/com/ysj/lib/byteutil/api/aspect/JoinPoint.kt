package com.ysj.lib.byteutil.api.aspect

import java.io.Serializable

/**
 * 方法连接点
 *
 * @author Ysj
 * Create time: 2021/3/8
 */
class JoinPoint(
    /** 切入点的 this 获取的对象 */
    val target: Any,
    /** 切入点方法的参数 */
    val args: Array<Any?>,
) : Serializable