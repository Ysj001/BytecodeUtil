package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect

import com.ysj.lib.bytecodeutil.api.aspect.*
import org.objectweb.asm.Type
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

    companion object {
        const val TARGET_CLASS = "class"
        const val TARGET_SUPER_CLASS = "superClass"
        const val TARGET_INTERFACE = "interface"
        const val TARGET_ANNOTATION = "annotation"
    }

    /** 切面方法的参数 */
    val aspectFunArgs: Array<Type> = Type.getArgumentTypes(aspectFunDesc)

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

    /**
     * 检查 [position] 和 [aspectFunDesc] 是否合法
     */
    fun checkPositionAndMethodArgs() {
        val argumentTypes = Type.getArgumentTypes(aspectFunDesc)
        when (position) {
            POSITION_START, POSITION_RETURN -> {
                if (argumentTypes.isEmpty()) return
                if (argumentTypes[0].className == JoinPoint::class.java.name) return
                throw RuntimeException(
                    """
                    检测到 $aspectClassName 中方法 $aspectFunName$aspectFunDesc 的参数不合法
                    该 position: $position 支持的参数为：
                    1. 无参数
                    2. args index 0: ${JoinPoint::class.java.simpleName}
                    """.trimIndent()
                )
            }
            POSITION_CALL -> {
                if (argumentTypes.isEmpty()) return
                if (argumentTypes[0].className == JoinPoint::class.java.name &&
                    argumentTypes[1].className == CallingPoint::class.java.name) return
                throw RuntimeException(
                    """
                    检测到 $aspectClassName 中方法 $aspectFunName$aspectFunDesc 的参数不合法
                    该 position: $position 支持的参数为：
                    1. 无参数
                    2. args index 0: ${JoinPoint::class.java.name}
                    3. args index 1: ${CallingPoint::class.java.name}
                    """.trimIndent()
                )
            }
            else -> throw RuntimeException(
                """
                position 不合法：$position
                class name: $aspectClassName
                fun name: $aspectFunName
                """.trimIndent()
            )
        }
    }
}