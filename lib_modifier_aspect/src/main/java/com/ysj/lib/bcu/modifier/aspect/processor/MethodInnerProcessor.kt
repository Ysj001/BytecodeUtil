package com.ysj.lib.bcu.modifier.aspect.processor

import com.ysj.lib.bcu.modifier.aspect.ASPECT_CLASS_INSTANCE
import com.ysj.lib.bcu.modifier.aspect.AspectModifier
import com.ysj.lib.bcu.modifier.aspect.PointcutBean
import com.ysj.lib.bcu.modifier.aspect.api.POSITION_RETURN
import com.ysj.lib.bcu.modifier.aspect.api.POSITION_START
import com.ysj.lib.bytecodeutil.plugin.api.firstNode
import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * 方法内部修改处理器
 *
 * @author Ysj
 * Create time: 2021/8/15
 */
class MethodInnerProcessor(globalCache: MutableMap<String, Any?>) : BaseMethodProcessor(globalCache) {

    private val logger = YLogger.getLogger(javaClass)

    fun process(pointcut: PointcutBean, classNode: ClassNode, methodNode: MethodNode) {
        if (pointcut.position != POSITION_RETURN && pointcut.position != POSITION_START) return
        val firstNode = methodNode.firstNode ?: return
        val insnList = methodNode.instructions
        // 切面方法的参数
        val hasJoinPoint = pointcut.aspectFunArgs.isNotEmpty()
        if (hasJoinPoint && !firstNode.beforeIsStoredJoinPoint) {
            insnList.insertBefore(firstNode, storeJoinPoint(classNode, methodNode))
        }
        // 将 Pointcut 和 JointPoint 连接 XXX.instance.xxxfun(jointPoint);
        val callAspectFun: (AbstractInsnNode) -> Unit = {
            insnList.insertBefore(it, InsnList().apply {
                add(FieldInsnNode(
                    Opcodes.GETSTATIC,
                    pointcut.aspectClassName,
                    ASPECT_CLASS_INSTANCE,
                    Type.getObjectType(pointcut.aspectClassName).descriptor
                ))
                if (hasJoinPoint) add(getJoinPoint(classNode, methodNode))
                add(MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    pointcut.aspectClassName,
                    pointcut.aspectFunName,
                    pointcut.aspectFunDesc,
                    false
                ))
            })
        }
        val isStoredJoinPoint = firstNode.beforeIsStoredJoinPoint
        val removedJoinPoint = isRemovedJoinPoint(classNode, methodNode)
        when (pointcut.position) {
            POSITION_START -> callAspectFun(firstNode)
            POSITION_RETURN -> for (insnNode in insnList) {
                if (insnNode.opcode !in Opcodes.IRETURN..Opcodes.RETURN) continue
                if (!isStoredJoinPoint) callAspectFun(insnNode)
                else if (!removedJoinPoint) callAspectFun(insnNode)
                else callAspectFun(insnNode.previous.previous)
            }
        }
        if (isStoredJoinPoint && !removedJoinPoint) for (insnNode in insnList) {
            if (insnNode.opcode !in Opcodes.IRETURN..Opcodes.RETURN) continue
            insnList.insertBefore(insnNode, removeJoinPoint(classNode, methodNode))
        }
        if (isStoredJoinPoint) cacheRemovedJoinPoint(classNode, methodNode)
        logger.info("Method Inner 插入 --> ${classNode.name}#${methodNode.name}${methodNode.desc}")
    }

}