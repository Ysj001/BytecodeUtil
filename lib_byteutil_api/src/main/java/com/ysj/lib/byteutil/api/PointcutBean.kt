package com.ysj.lib.byteutil.api

import java.io.Serializable

/**
 * 描述切入点的实体
 *
 * @author Ysj
 */
class PointcutBean(
    /** 用于处理该切入点的切面类名 */
    val aspectClassName: String,
    /** 用于处理该切入点的切面方法名 */
    val aspectFunName: String,
    /** 用于处理该切入点的切面方法描述 */
    val aspectFunDesc: String,
    /** 切入点类名 */
    val target: String,
    /** 切入点类型（class，superClass，interface，annotation） */
    val targetType: String,
    /** 切入点方法名和描述 */
    val funName: String,
    /** 切入点方法描述 */
    val funDesc: String,
    /** 切入方法的执行位置 */
    val position: Int,
) : Serializable {

    override fun toString(): String {
        return """
                PointCut:
                aspectClassName='$aspectClassName'
                aspectFunName='$aspectFunName'
                aspectFunDesc='$aspectFunDesc'
                target='$target'
                targetType='$targetType'
                funName='$funName'
                funDesc='$funDesc'
                position=$position
                """.trimIndent()
    }
}