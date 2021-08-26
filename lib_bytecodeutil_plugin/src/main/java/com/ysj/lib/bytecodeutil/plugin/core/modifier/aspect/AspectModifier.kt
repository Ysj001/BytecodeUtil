package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect

import com.android.build.api.transform.Transform
import com.ysj.lib.bytecodeutil.api.aspect.*
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.params
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.processor.MethodInnerProcessor
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.processor.MethodProxyProcessor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 用于处理 [Aspect] , [Pointcut] 来实现切面的修改器
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class AspectModifier(
    override val transform: Transform,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    val cache by lazy { HashMap<String, Any>() }

    private val logger = YLogger.getLogger(javaClass)

    private val methodInnerProcessor by lazy { MethodInnerProcessor(this) }

    private val methodProxyProcessor by lazy { MethodProxyProcessor(this) }

    override fun scan(classNode: ClassNode) {
        // 过滤所有没有 Aspect 注解的类
        if (Type.getDescriptor(Aspect::class.java) !in classNode.invisibleAnnotations?.map { it.desc } ?: Collections.EMPTY_LIST) return
        classNode.methods.forEach {
            // 过滤构造器和静态代码块
            if (it.name == "<init>" || it.name == "<clinit>") return@forEach
            // 查找 Pointcut 注解的方法
            val pointCutAnnotation = it.invisibleAnnotations
                ?.find { anode -> anode.desc == Type.getDescriptor(Pointcut::class.java) }
                ?: return@forEach
            // 收集 Pointcut 注解的参数
            val params = pointCutAnnotation.params()
            val orgTarget = params[Pointcut::target.name] as String
            val target = orgTarget.substringAfter(":")
            val targetType = orgTarget.substringBefore(":")
            val collection = when (targetType) {
                "class" -> methodInnerProcessor.targetClass
                "superClass" -> methodInnerProcessor.targetSuperClass
                "interface" -> methodInnerProcessor.targetInterface
                "annotation" -> methodInnerProcessor.targetAnnotation
                else -> throw RuntimeException("${Pointcut::class.java.simpleName} 中 target 前缀不合法：${orgTarget}")
            }
            val pointcutBean = PointcutBean(
                aspectClassName = classNode.name,
                aspectFunName = it.name,
                aspectFunDesc = it.desc,
                target = target,
                targetType = targetType,
                funName = params[Pointcut::funName.name] as String,
                funDesc = params[Pointcut::funDesc.name] as String,
                position = params[Pointcut::position.name] as Int,
            ).also { pcb ->
                logger.verbose("====== method: ${it.name} ======\n$pcb")
            }
            // 检查方法参数是否合法
            Type.getArgumentTypes(it.desc).forEach checkArgs@{ type ->
                val typeClass = type.className
                when (val p = pointcutBean.position) {
                    POSITION_START, POSITION_RETURN -> if (typeClass != JoinPoint::class.java.name) {
                        throw RuntimeException(
                            """
                            检测到 ${classNode.name} 中方法 ${it.name} ${it.desc} 的参数不合法
                            该 position: $p 支持的参数为：
                            1. ${JoinPoint::class.java.simpleName}
                            2. 无参数
                            """.trimIndent()
                        )
                    }
                    POSITION_CALL -> {
                        if (typeClass == JoinPoint::class.java.name) return@checkArgs
                        if (typeClass == CallingPoint::class.java.name) return@checkArgs
                        throw RuntimeException(
                            """
                            检测到 ${classNode.name} 中方法 ${it.name} ${it.desc} 的参数不合法
                            该 position: $p 支持的参数为：
                            1. ${JoinPoint::class.java.name}
                            2. ${CallingPoint::class.java.name}
                            3. 无参数
                            """.trimIndent()
                        )
                    }
                }
            }
            when (pointcutBean.position) {
                POSITION_START, POSITION_RETURN -> collection.add(pointcutBean)
                POSITION_CALL -> methodProxyProcessor.targetCallStart.add(pointcutBean)
            }
        }
    }

    override fun modify(classNode: ClassNode) {
        handleAspect(classNode)
        handlePointcut(classNode)
    }

    /**
     * 在 [Aspect] 注解的类中添加用于获取该类实例的静态成员
     */
    private fun handleAspect(classNode: ClassNode) {
        if (Type.getDescriptor(Aspect::class.java) !in classNode.invisibleAnnotations?.map { it.desc } ?: Collections.EMPTY_LIST) return
        val fieldInstance = FieldNode(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
            "instance",
            Type.getObjectType(classNode.name).descriptor,
            null,
            null
        )
        classNode.fields.add(fieldInstance)
        var clint = classNode.methods.find { it.name == "<clinit>" && it.desc == "()V" }
        if (clint == null) {
            clint = MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
            )
            classNode.methods.add(0, clint)
            // 刚创建出来的 instructions 是空的，必需要添加个 return
            clint.instructions.add(InsnNode(Opcodes.RETURN))
        }
        clint.instructions.insertBefore(clint.instructions.first, InsnList().apply {
            add(TypeInsnNode(Opcodes.NEW, classNode.name))
            add(InsnNode(Opcodes.DUP))
            add(MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                classNode.name,
                "<init>",
                "()V",
                false
            ))
            add(FieldInsnNode(
                Opcodes.PUTSTATIC,
                classNode.name,
                fieldInstance.name,
                fieldInstance.desc
            ))
        })
    }

    /**
     * 处理 [Pointcut] 收集的信息
     */
    private fun handlePointcut(classNode: ClassNode) {
        val targetClassPointcuts = methodInnerProcessor.findPointcuts(classNode)
        ArrayList(classNode.methods).forEach { methodNode ->
            methodProxyProcessor.process(classNode, methodNode)
            targetClassPointcuts.forEach targetClass@{ pointcut ->
                if (!Pattern.matches(pointcut.funName, methodNode.name)) return@targetClass
                if (!Pattern.matches(pointcut.funDesc, methodNode.desc)) return@targetClass
                methodInnerProcessor.process(pointcut, classNode, methodNode)
            }
        }
    }
}