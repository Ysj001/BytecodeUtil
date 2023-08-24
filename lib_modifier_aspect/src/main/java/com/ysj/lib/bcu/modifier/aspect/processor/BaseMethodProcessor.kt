package com.ysj.lib.bcu.modifier.aspect.processor

import com.ysj.lib.bcu.modifier.aspect.api.JoinPoint
import com.ysj.lib.bytecodeutil.plugin.api.argsInsnList
import com.ysj.lib.bytecodeutil.plugin.api.isStatic
import com.ysj.lib.bcu.modifier.aspect.AspectModifier
import com.ysj.lib.bcu.modifier.aspect.joinPointDesc
import com.ysj.lib.bcu.modifier.aspect.joinPointInternalName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * 基础处理方法的处理器
 *
 * @author Ysj
 * Create time: 2021/8/15
 */
open class BaseMethodProcessor(val aspectModifier: AspectModifier) {

    private val joinPointRemovedCache by lazy {
        var cache = aspectModifier.cache["joinPointRemovedCache"]
        if (cache == null) cache = HashSet<String>().also { aspectModifier.cache["joinPointRemovedCache"] = it }
        cache as HashSet<String>
    }

    /**
     * 判断指定 node 前是否已经存了 [JoinPoint]
     */
    val AbstractInsnNode.beforeIsStoredJoinPoint: Boolean
        get() {
            var node: AbstractInsnNode? = this
            while (node != null) {
                if (node is MethodInsnNode
                    && node.opcode == Opcodes.INVOKESTATIC
                    && node.owner == joinPointInternalName
                    && node.name == "put"
                ) return true
                node = node.previous
            }
            return false
        }

    /**
     * 生成 [JoinPoint] 并缓存起来
     * JointPoint.put("{className}-{methodName}{methodDesc}", this, args);
     */
    fun storeJoinPoint(classNode: ClassNode, methodNode: MethodNode): InsnList = InsnList().apply {
        add(LdcInsnNode("${classNode.name}-${methodNode.name}${methodNode.desc}"))
        add(if (methodNode.isStatic) InsnNode(Opcodes.ACONST_NULL) else VarInsnNode(Opcodes.ALOAD, 0))
        add(methodNode.argsInsnList()).apply { remove(last) }
        add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            joinPointInternalName,
            "put",
            "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V",
            false
        ))
    }

    /**
     * 从缓存中移除 [JoinPoint]
     * ```
     * JoinPoint.remove("{className}-{methodName}{methodDesc}")
     * ```
     */
    fun removeJoinPoint(classNode: ClassNode, methodNode: MethodNode): InsnList = InsnList().apply {
        add(LdcInsnNode("${classNode.name}-${methodNode.name}${methodNode.desc}"))
        add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            joinPointInternalName,
            "remove",
            "(Ljava/lang/String;)V",
            false
        ))
    }

    /**
     * 缓存某方法中已经移除了 [JoinPoint]
     */
    fun cacheRemovedJoinPoint(classNode: ClassNode, methodNode: MethodNode) {
        joinPointRemovedCache.add("${classNode.name}-${methodNode.name}-${methodNode.desc}")
    }

    /**
     * 若某方法已经移除 [JoinPoint] 则为 true
     */
    fun isRemovedJoinPoint(classNode: ClassNode, methodNode: MethodNode): Boolean =
        joinPointRemovedCache.contains("${classNode.name}-${methodNode.name}-${methodNode.desc}")

    /**
     * 从缓存中取出 [JoinPoint]
     * JointPoint.get("{className}-{methodName}{methodDesc}");
     */
    fun getJoinPoint(classNode: ClassNode, methodNode: MethodNode): InsnList = InsnList().apply {
        add(LdcInsnNode("${classNode.name}-${methodNode.name}${methodNode.desc}"))
        add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            joinPointInternalName,
            "get",
            "(Ljava/lang/String;)$joinPointDesc",
            false
        ))
    }

}