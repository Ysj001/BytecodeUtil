package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.processor

import com.ysj.lib.bytecodeutil.api.aspect.JoinPoint
import com.ysj.lib.bytecodeutil.api.aspect.POSITION_CALL
import com.ysj.lib.bytecodeutil.modifier.*
import com.ysj.lib.bytecodeutil.plugin.core.MD5
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.AspectModifier
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.PointcutBean
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.callingPointType
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.joinPointType
import org.codehaus.groovy.ast.ClassHelper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*
import java.util.regex.Pattern

/**
 * 方法调用代理处理器
 *
 * @author Ysj
 * Create time: 2021/8/15
 */
class MethodProxyProcessor(aspectModifier: AspectModifier) : BaseMethodProcessor(aspectModifier) {

    companion object {
        /** 代理方法的前缀 */
        const val PREFIX_PROXY_METHOD = "bcu_proxy_"
    }

    private val logger = YLogger.getLogger(javaClass)

    // 记录代理替换的节点。key：代理的节点 value：源节点
    private val recordProxyNode by lazy { HashMap<MethodInsnNode, MethodInsnNode>() }

    fun process(pointcutBean: PointcutBean, classNode: ClassNode, methodNode: MethodNode) {
        if (pointcutBean.position != POSITION_CALL) return
        val firstNode = methodNode.firstNode ?: return
        val insnList = methodNode.instructions
        val insnNodes = insnList.toArray()
        insnNodes.forEach node@{ node ->
            if (node !is MethodInsnNode) return@node
            val proxy = recordProxyNode[node]
            val realNode = proxy ?: node
            pointcutBean.takeIf {
                isAnnotationTarget(it, realNode) || Pattern.matches(it.target, realNode.owner)
                        && Pattern.matches(it.funName, realNode.name)
                        && Pattern.matches(it.funDesc, realNode.desc)
            } ?: return@node
            // 切面方法的参数
            val aspectFunArgs = pointcutBean.aspectFunArgs
            val hasJoinPoint = aspectFunArgs.indexOfFirst { it.className == JoinPoint::class.java.name } >= 0
            if (hasJoinPoint && !firstNode.beforeIsStoredJoinPoint) {
                insnList.insertBefore(firstNode, storeJoinPoint(classNode, methodNode))
            }
            val proxyMethod = classNode.generateProxyMethod(pointcutBean, node, hasJoinPoint)
            val proxyNode = MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, proxyMethod.name, proxyMethod.desc, false)
            // 把源调用替换成代理调用
            insnList.insertBefore(node, proxyNode)
            insnList.remove(node)
            if (proxy != null) recordProxyNode.remove(proxy)
            recordProxyNode[proxyNode] = realNode
            // 插入参数
            if (hasJoinPoint) insnList.insertBefore(proxyNode, getJoinPoint(classNode, methodNode))
            logger.info("Method Call 插入 --> ${classNode.name}#${methodNode.name}${methodNode.desc}")
        }
        if (firstNode.beforeIsStoredJoinPoint && !isRemovedJoinPoint(classNode, methodNode)) {
            for (insnNode in insnList) {
                if (insnNode.opcode !in Opcodes.IRETURN..Opcodes.RETURN) continue
                insnList.insertBefore(insnNode, removeJoinPoint(classNode, methodNode))
            }
            cacheRemovedJoinPoint(classNode, methodNode)
        }
    }

    /**
     * 在指定类中生成代理方法
     * ```
     * static {returnType} {proxyMethodName}(caller, ...args, JoinPoint) {
     *     CallingPoint callingPoint = CallingPoint.newInstance(caller, isStatic, funName, new Class[]{...argTypes}, new Object[]{...args});
     *     {returnType} result = ({returnType}){AspectClass}.instance.{aspectFun}(JoinPoint, callingPoint);
     *     cp.release();
     *     return result;
     * }
     * ```
     */
    private fun ClassNode.generateProxyMethod(pointcut: PointcutBean, calling: MethodInsnNode, hasJoinPoint: Boolean): MethodNode {
        val proxyName = "${calling.owner}${calling.name}${calling.desc}".MD5
        var find: MethodNode? = null
        // 代理方法都添加到了 methods 的后面，从后面查比较快
        for (i in methods.lastIndex downTo 0) {
            val method = methods[i]
            // 代理方法都有前缀，没有前缀说明没生成过直接 break
            if (!method.name.startsWith(PREFIX_PROXY_METHOD)) break
            if (method.name.endsWith(proxyName)) {
                find = method
                break
            }
        }
        if (find != null) return find
        val callerType = Type.getObjectType(calling.owner)
        val callerDesc = if (calling.isStatic) "" else callerType.descriptor
        val args = Type.getArgumentTypes(calling.desc)
        val argsDesc = args.map { it.descriptor }.toTypedArray().contentToString().run {
            substring(1 until lastIndex).replace(", ", "")
        }
        val returnType = Type.getReturnType(calling.desc)
        val joinPointDesc = if (hasJoinPoint) joinPointType.descriptor else ""
        val method = MethodNode(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            "$PREFIX_PROXY_METHOD$proxyName",
            "($callerDesc$argsDesc$joinPointDesc)${returnType.descriptor}",
            null,
            null
        )
        method.instructions.apply {
            // caller, ...args 的下一个参数的索引
            val argsNextIndex = args.size + if (calling.isStatic) 0 else 1
            // caller
            add(if (!calling.isStatic) VarInsnNode(Opcodes.ALOAD, 0) else callerType.classInsnNode)
            // isStatic
            add(InsnNode(if (calling.isStatic) Opcodes.ICONST_1 else Opcodes.ICONST_0))
            // funName
            add(LdcInsnNode(calling.name))
            // new Class[]{...argTypes}
            add(args.argTypesArray())
            // new Object[]{...args}
            add(args.argsArray(if (calling.isStatic) 0 else 1))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                callingPointType.internalName,
                "newInstance",
                "(Ljava/lang/Object;ZLjava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)${callingPointType.descriptor}",
                false
            ))
            add(VarInsnNode(Opcodes.ASTORE, argsNextIndex + 1))
            // {AspectClass}.instance
            add(FieldInsnNode(
                Opcodes.GETSTATIC,
                pointcut.aspectClassName,
                "instance",
                Type.getObjectType(pointcut.aspectClassName).descriptor
            ))
            // joinPoint
            if (hasJoinPoint) add(VarInsnNode(Opcodes.ALOAD, argsNextIndex))
            // callingPoint
            add(VarInsnNode(Opcodes.ALOAD, argsNextIndex + 1))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                pointcut.aspectClassName,
                pointcut.aspectFunName,
                pointcut.aspectFunDesc,
                false
            ))
            add(cast(Type.getReturnType(pointcut.aspectFunDesc), returnType))
            if (returnType.sort != Type.METHOD && returnType.sort != Type.VOID) {
                add(VarInsnNode(returnType.getOpcode(Opcodes.ISTORE), argsNextIndex + 2))
            }
            // callingPoint.release()
            add(VarInsnNode(Opcodes.ALOAD, argsNextIndex + 1))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                callingPointType.internalName,
                "release",
                "()V",
                false
            ))
            if (returnType.sort != Type.METHOD && returnType.sort != Type.VOID) {
                add(VarInsnNode(returnType.getOpcode(Opcodes.ILOAD), argsNextIndex + 2))
            }
            add(InsnNode(returnType.getOpcode(Opcodes.IRETURN)))
        }
        methods.add(method)
        return method
    }

    private fun Array<Type>.argTypesArray() = InsnList().apply {
        add(IntInsnNode(Opcodes.BIPUSH, size))
        add(TypeInsnNode(Opcodes.ANEWARRAY, CLASS_TYPE.internalName))
        forEachIndexed { i, t ->
            add(InsnNode(Opcodes.DUP))
            add(IntInsnNode(Opcodes.BIPUSH, i))
            add(t.classInsnNode)
            add(InsnNode(Opcodes.AASTORE))
        }
    }

    private fun Array<Type>.argsArray(startLocalVarIndex: Int) = InsnList().apply {
        var localVarIndex = startLocalVarIndex
        add(IntInsnNode(Opcodes.BIPUSH, size))
        add(TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(Any::class.java).internalName))
        forEachIndexed { i, t ->
            add(InsnNode(Opcodes.DUP))
            add(IntInsnNode(Opcodes.BIPUSH, i))
            add(VarInsnNode(t.opcodeLoad(), localVarIndex))
            if (t.sort in Type.BOOLEAN..Type.DOUBLE) {
                // primitive types must be boxed
                val wrapperType = Type.getType(Class.forName(
                    ClassHelper.getWrapper(ClassHelper.make(t.className)).name
                ))
                add(MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    wrapperType.internalName,
                    "valueOf",
                    "(${t.descriptor})${wrapperType.descriptor}",
                    false
                ))
            }
            add(InsnNode(Opcodes.AASTORE))
            // 计算当前参数的索引
            localVarIndex += t.size
        }
    }

    private fun isAnnotationTarget(pointcutBean: PointcutBean, node: MethodInsnNode): Boolean {
        if (pointcutBean.targetType != PointcutBean.TARGET_ANNOTATION) return false
        val classNode = aspectModifier.allClassNode[node.owner] ?: return false
        val predicate: (AnnotationNode) -> Boolean = { Pattern.matches(pointcutBean.target, it.desc) }
        val methodNode = classNode.methods
            .find { it.visibleAnnotations?.find(predicate) != null || it.invisibleAnnotations?.find(predicate) != null }
            ?: return false
        return methodNode.name == node.name && methodNode.desc == node.desc
    }
}