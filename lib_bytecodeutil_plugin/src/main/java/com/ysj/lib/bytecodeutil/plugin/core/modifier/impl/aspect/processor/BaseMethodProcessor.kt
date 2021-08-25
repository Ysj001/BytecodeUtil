package com.ysj.lib.bytecodeutil.plugin.core.modifier.impl.aspect.processor

import com.ysj.lib.bytecodeutil.api.aspect.JoinPoint
import com.ysj.lib.bytecodeutil.plugin.core.modifier.argsInsnList
import com.ysj.lib.bytecodeutil.plugin.core.modifier.impl.aspect.AspectModifier
import com.ysj.lib.bytecodeutil.plugin.core.modifier.isStatic
import com.ysj.lib.bytecodeutil.plugin.core.modifier.newObject
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * 基础处理方法的处理器
 *
 * @author Ysj
 * Create time: 2021/8/15
 */
open class BaseMethodProcessor(val aspectModifier: AspectModifier) {

    val joinPointType = Type.getType(JoinPoint::class.java)

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
                    && node.owner == joinPointType.internalName
                    && node.name == "put"
                ) return true
                node = node.previous
            }
            return false
        }

    /**
     * 生成 [JoinPoint] 并缓存起来
     * JointPoint.put("{className}-{methodName}{methodDesc}", new JointPoint(this, args));
     */
    fun storeJoinPoint(classNode: ClassNode, methodNode: MethodNode): InsnList = InsnList().apply {
        add(LdcInsnNode("${classNode.name}-${methodNode.name}${methodNode.desc}"))
        add(newObject(
            JoinPoint::class.java,
            linkedMapOf(
                Any::class.java to InsnList().apply {
                    add(if (methodNode.isStatic) InsnNode(Opcodes.ACONST_NULL) else VarInsnNode(Opcodes.ALOAD, 0))
                },
                Array<Any?>::class.java to methodNode.argsInsnList().apply { remove(last) },
            )
        ))
        add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            joinPointType.internalName,
            "put",
            "(Ljava/lang/String;${joinPointType.descriptor})V",
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
            joinPointType.internalName,
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
            joinPointType.internalName,
            "get",
            "(Ljava/lang/String;)${joinPointType.descriptor}",
            false
        ))
    }

}