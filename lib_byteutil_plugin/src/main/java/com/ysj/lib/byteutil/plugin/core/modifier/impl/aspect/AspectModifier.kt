package com.ysj.lib.byteutil.plugin.core.modifier.impl.aspect

import com.android.build.api.transform.Transform
import com.ysj.lib.byteutil.api.aspect.Aspect
import com.ysj.lib.byteutil.api.aspect.JoinPoint
import com.ysj.lib.byteutil.api.aspect.Pointcut
import com.ysj.lib.byteutil.plugin.core.logger.YLogger
import com.ysj.lib.byteutil.plugin.core.modifier.IModifier
import org.codehaus.groovy.ast.ClassHelper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.regex.Pattern

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

    private val logger = YLogger.getLogger(javaClass)

    private val targetClass by lazy { HashSet<PointcutBean>() }

    private val targetSuperClass by lazy { HashSet<PointcutBean>() }

    private val targetInterface by lazy { HashSet<PointcutBean>() }

    private val targetAnnotation by lazy { HashSet<PointcutBean>() }

    override fun scan(classNode: ClassNode) {
        // 过滤所有没有 Aspect 注解的类
        if (Type.getDescriptor(Aspect::class.java) !in classNode.invisibleAnnotations?.map { it.desc } ?: listOf()) return
        classNode.methods.forEach {
            // 过滤构造器和静态代码块
            if (it.name == "<init>" || it.name == "<clinit>") return@forEach
            // 查找 Pointcut 注解的方法
            val pointCutAnnotation = it.invisibleAnnotations
                ?.find { anode -> anode.desc == Type.getDescriptor(Pointcut::class.java) }
                ?: return@forEach
            // 检查方法参数是否合法
            val argumentTypes = Type.getArgumentTypes(it.desc)
            if (argumentTypes.size > 1
                || (argumentTypes.size == 1 && argumentTypes[0].className != JoinPoint::class.java.name)
            ) throw RuntimeException(
                """
                检测到不合法的参数: ${argumentTypes.map { t -> t.internalName }}    
                ${Pointcut::class.java.simpleName} 注解的方法参数只能如下
                1. ${JoinPoint::class.java.simpleName}
                2. 无参数
            """.trimIndent()
            )
            // 收集 Pointcut 注解的参数
            val params = HashMap<String, Any>()
            for (i in 0 until pointCutAnnotation.values.size step 2) {
                params[pointCutAnnotation.values[i] as String] = pointCutAnnotation.values[i + 1]
            }
            val orgTarget = params[Pointcut::target.name] as String
            val target = orgTarget.substringAfter(":")
            val targetType = orgTarget.substringBefore(":")
            when (targetType) {
                "class" -> targetClass
                "superClass" -> targetSuperClass
                "interface" -> targetInterface
                "annotation" -> targetAnnotation
                else -> throw RuntimeException("${Pointcut::class.java.simpleName} 中 target 前缀不合法：${orgTarget}")
            }.add(PointcutBean(
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
            })
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
        if (Type.getDescriptor(Aspect::class.java) !in classNode.invisibleAnnotations?.map { it.desc } ?: listOf()) return
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
            add(
                MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    classNode.name,
                    "<init>",
                    "()V",
                    false
                )
            )
            add(
                FieldInsnNode(
                    Opcodes.PUTSTATIC,
                    classNode.name,
                    fieldInstance.name,
                    fieldInstance.desc
                )
            )
        })
    }

    /**
     * 处理 [Pointcut] 收集的信息
     */
    private fun handlePointcut(classNode: ClassNode) =
        findPointcuts(classNode).forEach { pointcut ->
            classNode.methods
                .filter { methodNode ->
                    Pattern.matches(pointcut.funName, methodNode.name)
                            && Pattern.matches(pointcut.funDesc, methodNode.desc)
                }
                .forEach { methodNode ->
                    // 切面方法的参数
                    val aspectFunArgs = Type.getArgumentTypes(pointcut.aspectFunDesc)
                    // 当前方法的参数
                    val argumentTypes = Type.getArgumentTypes(methodNode.desc)
                    val instructions = methodNode.instructions
                    instructions.insertBefore(
                        if (pointcut.position == -1) instructions.get(instructions.size() - 2)
                        else instructions.get(pointcut.position),
                        InsnList().apply {
                            // 操作数
                            var opNum = 1
                            if (aspectFunArgs.isNotEmpty()) {
                                // 1.将方法中的参数存到数组中 Object[] args = {arg1, arg2, arg3, ...};
                                add(IntInsnNode(Opcodes.BIPUSH, argumentTypes.size))
                                add(
                                    TypeInsnNode(
                                        Opcodes.ANEWARRAY,
                                        Type.getType(Any::class.java).internalName
                                    )
                                )
                                argumentTypes.forEachIndexed { i, t ->
                                    add(InsnNode(Opcodes.DUP))
                                    add(IntInsnNode(Opcodes.BIPUSH, i))
                                    add(
                                        VarInsnNode(
                                            when (t.sort) {
                                                Type.VOID,
                                                Type.ARRAY,
                                                Type.OBJECT,
                                                Type.METHOD -> Opcodes.ALOAD
                                                Type.FLOAT -> Opcodes.FLOAD
                                                Type.LONG -> Opcodes.LLOAD
                                                Type.DOUBLE -> Opcodes.DLOAD
                                                else -> Opcodes.ILOAD
                                            },
                                            opNum
                                        )
                                    )
                                    if (t.sort in Type.BOOLEAN..Type.DOUBLE) {
                                        // primitive types must be boxed
                                        val wrapperType = Type.getType(
                                            Class.forName(
                                                ClassHelper.getWrapper(ClassHelper.make(t.className)).name
                                            )
                                        )
                                        add(
                                            MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                wrapperType.internalName,
                                                "valueOf",
                                                "(${t.descriptor})${wrapperType.descriptor}",
                                                false
                                            )
                                        )
                                    }
                                    add(InsnNode(Opcodes.AASTORE))
                                    // 计算操作数
                                    opNum += if (t.sort == Type.LONG || t.sort == Type.DOUBLE) 2 else 1
                                }
                                add(VarInsnNode(Opcodes.ASTORE, opNum))
                                // 2.构建 JointPoint 实体 JointPoint jointPoint = new JointPoint(this, args);
                                val jointPointInternalName =
                                    Type.getInternalName(JoinPoint::class.java)
                                val jointPointConstructor = JoinPoint::class.java.getConstructor(
                                    Any::class.java,
                                    Array<Any?>::class.java,
                                )
                                add(TypeInsnNode(Opcodes.NEW, jointPointInternalName))
                                add(InsnNode(Opcodes.DUP))
                                add(VarInsnNode(Opcodes.ALOAD, 0))
                                add(VarInsnNode(Opcodes.ALOAD, opNum))
                                add(
                                    MethodInsnNode(
                                        Opcodes.INVOKESPECIAL,
                                        jointPointInternalName,
                                        "<init>",
                                        Type.getType(jointPointConstructor).descriptor,
                                        false
                                    )
                                )
                                add(VarInsnNode(Opcodes.ASTORE, ++opNum))
                            }
                            // 3.将 Pointcut 和 JointPoint 连接 XXX.instance.xxxfun(jointPoint);
                            add(
                                FieldInsnNode(
                                    Opcodes.GETSTATIC,
                                    pointcut.aspectClassName,
                                    "instance",
                                    Type.getObjectType(pointcut.aspectClassName).descriptor
                                )
                            )
                            if (aspectFunArgs.isNotEmpty()) {
                                add(VarInsnNode(Opcodes.ALOAD, opNum))
                            }
                            add(
                                MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL,
                                    pointcut.aspectClassName,
                                    pointcut.aspectFunName,
                                    pointcut.aspectFunDesc,
                                    false
                                )
                            )
                        }
                    )
                    logger.info("插入 --> ${classNode.name}#${methodNode.name + methodNode.desc}")
                }
        }

    /**
     * 查找类中所有的切入点
     */
    private fun findPointcuts(classNode: ClassNode): ArrayList<PointcutBean> {
        val pointcuts = ArrayList<PointcutBean>()
        // 查找类中的
        targetClass.forEach { if (Pattern.matches(it.target, classNode.name)) pointcuts.add(it) }
        // 查找父类中的
        for (it in targetSuperClass) {
            fun findSuperClass(superName: String?) {
                superName ?: return
                if (Pattern.matches(it.target, superName)) pointcuts.add(it)
                else findSuperClass(allClassNode[superName]?.superName)
            }
            findSuperClass(classNode.superName)
        }
        // 查找接口中的

        // 查找注解中的

        return pointcuts
    }
}