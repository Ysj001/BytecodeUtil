package com.ysj.lib.bytecodeutil.plugin.core.modifier.impl.aspect.processor

import com.ysj.lib.bytecodeutil.api.aspect.CallingPoint
import com.ysj.lib.bytecodeutil.api.aspect.JoinPoint
import com.ysj.lib.bytecodeutil.plugin.core.MD5
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import com.ysj.lib.bytecodeutil.plugin.core.modifier.cast
import com.ysj.lib.bytecodeutil.plugin.core.modifier.impl.aspect.AspectModifier
import com.ysj.lib.bytecodeutil.plugin.core.modifier.impl.aspect.PointcutBean
import com.ysj.lib.bytecodeutil.plugin.core.modifier.isStatic
import com.ysj.lib.bytecodeutil.plugin.core.modifier.opcodeLoad
import org.codehaus.groovy.ast.ClassHelper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashSet

/**
 * 方法调用代理处理器
 *
 * @author Ysj
 * Create time: 2021/8/15
 */
class MethodProxyProcessor(aspectModifier: AspectModifier) : BaseMethodProcessor(aspectModifier) {

    private val logger = YLogger.getLogger(javaClass)

    val targetCallStart by lazy { HashSet<PointcutBean>() }

    val callingPointType = Type.getType(CallingPoint::class.java)

    fun process(classNode: ClassNode, methodNode: MethodNode) {
        val insnList = methodNode.instructions
        val insnNodes = insnList.toArray()
        insnNodes.forEach node@{ node ->
            if (node !is MethodInsnNode) return@node
            val pointcutBean = targetCallStart.find {
                Pattern.matches(it.target, node.owner)
                        && Pattern.matches(it.funName, node.name)
                        && Pattern.matches(it.funDesc, node.desc)
            } ?: return@node
            val firstLabel = if (methodNode.name != "<init>") insnList.first else {
                var result: AbstractInsnNode? = null
                val iterator = insnList.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.opcode == Opcodes.INVOKESPECIAL) {
                        result = next.next
                        break
                    }
                }
                result
            } ?: return@node
            // 切面方法的参数
            val aspectFunArgs = pointcutBean.aspectFunArgs
            val hasJoinPoint = aspectFunArgs.indexOfFirst { it.className == JoinPoint::class.java.name } >= 0
            if (hasJoinPoint && !methodNode.isStoredJoinPoint) {
                insnList.insertBefore(firstLabel, storeJoinPoint(classNode, methodNode))
            }
            val proxyMethod = classNode.generateProxyMethod(pointcutBean, node, hasJoinPoint)
            val proxyNode = MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, proxyMethod.name, proxyMethod.desc, false)
            // 把源调用替换成代理调用
            insnList.insertBefore(node, proxyNode)
            insnList.remove(node)
            // 插入参数
            if (hasJoinPoint) insnList.insertBefore(proxyNode, getJoinPoint(classNode, methodNode))
            logger.info("Method Call 插入 --> ${classNode.name}#${methodNode.name}${methodNode.desc}")
        }
    }

    /**
     * 在指定类中生成代理方法
     * ```
     * static {returnType} {proxyMethodName}(caller, ...args, JoinPoint) {
     *     return ({returnType}){AspectClass}.instance.{aspectFun}(JoinPoint, CallingPoint.newInstance(caller, isStatic, funName, new Class[]{...argTypes}, new Object[]{...args}));
     * }
     * ```
     */
    private fun ClassNode.generateProxyMethod(pointcut: PointcutBean, calling: MethodInsnNode, hasJoinPoint: Boolean): MethodNode {
        val proxyName = calling.proxyName()
        val find = methods.find { it.access == Opcodes.ACC_STATIC && name == proxyName }
        if (find != null) return find
        val callerDesc = if (calling.isStatic) Type.getDescriptor(Class::class.java) else Type.getObjectType(calling.owner).descriptor
        val args = Type.getArgumentTypes(calling.desc)
        val argsDesc = args.map { it.descriptor }.toTypedArray().contentToString().run {
            substring(1 until lastIndex).replace(", ", "")
        }
        val returnType = Type.getReturnType(calling.desc)
        val joinPointDesc = if (hasJoinPoint) joinPointType.descriptor else ""
        val method = MethodNode(
            Opcodes.ACC_STATIC,
            proxyName,
            "($callerDesc$argsDesc$joinPointDesc)${returnType.descriptor}",
            null,
            null
        )
        method.instructions.apply {
            // {AspectClass}.instance
            add(FieldInsnNode(
                Opcodes.GETSTATIC,
                pointcut.aspectClassName,
                "instance",
                Type.getObjectType(pointcut.aspectClassName).descriptor
            ))
            // caller, ...args 的下一个参数的索引
            val argsNextIndex = args.size + 1
            // JoinPoint
            if (hasJoinPoint) add(VarInsnNode(Opcodes.ALOAD, argsNextIndex))
            // caller
            add(VarInsnNode(Opcodes.ALOAD, 0))
            // isStatic
            add(InsnNode(if (calling.isStatic) Opcodes.ICONST_1 else Opcodes.ICONST_0))
            // funName
            add(LdcInsnNode(calling.name))
            // new Class[]{...argTypes}
            add(args.argTypesArray())
            // new Object[]{...args}
            add(args.argsArray(1))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                callingPointType.internalName,
                "newInstance",
                "(Ljava/lang/Object;ZLjava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)${callingPointType.descriptor}",
                false
            ))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                pointcut.aspectClassName,
                pointcut.aspectFunName,
                pointcut.aspectFunDesc,
                false
            ))
            add(cast(Type.getReturnType(pointcut.aspectFunDesc), returnType))
            add(InsnNode(returnType.getOpcode(Opcodes.IRETURN)))
        }
        methods.add(method)
        return method
    }

    private fun Array<Type>.argTypesArray() = InsnList().apply {
        val classType = Type.getType(Class::class.java)
        add(IntInsnNode(Opcodes.BIPUSH, size))
        add(TypeInsnNode(Opcodes.ANEWARRAY, classType.internalName))
        forEachIndexed { i, t ->
            add(InsnNode(Opcodes.DUP))
            add(IntInsnNode(Opcodes.BIPUSH, i))
            add(LdcInsnNode(t.internalName.replace("/", ".")))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                classType.internalName,
                "forName",
                "(Ljava/lang/String;)${classType.descriptor}",
                false
            ))
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

    private fun MethodInsnNode.proxyName(): String = "bcu_proxy_${name}_${"$owner$name$desc".MD5}"
}