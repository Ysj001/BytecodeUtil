package com.ysj.lib.byteutil.plugin.core.modifier.impl.aspect

import com.android.build.api.transform.Transform
import com.ysj.lib.byteutil.api.aspect.Aspect
import com.ysj.lib.byteutil.api.aspect.JoinPoint
import com.ysj.lib.byteutil.api.aspect.Pointcut
import com.ysj.lib.byteutil.plugin.core.logger.YLogger
import com.ysj.lib.byteutil.plugin.core.modifier.*
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
            val params = pointCutAnnotation.params()
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
    private fun handlePointcut(classNode: ClassNode) =
        findPointcuts(classNode).forEach { pointcut ->
            classNode.methods
                .filter { methodNode ->
                    Pattern.matches(pointcut.funName, methodNode.name)
                            && Pattern.matches(pointcut.funDesc, methodNode.desc)
                }
                .forEach { methodNode ->
                    val insnList = methodNode.instructions
                    val orgInsn = insnList.toArray()
                    val firstLabel = orgInsn.find { it is LabelNode } as LabelNode
                    // 切面方法的参数
                    val aspectFunArgs = Type.getArgumentTypes(pointcut.aspectFunDesc)
                    // 局部变量索引
                    var localVarIndex = 1
                    // 连接点节点的尾部
                    var joinPointInsnLast: AbstractInsnNode? = null
                    if (aspectFunArgs.isNotEmpty()) {
                        insnList.insert(firstLabel, InsnList().apply {
                            // 1.将方法中的参数存到数组中 Object[] args = {arg1, arg2, arg3, ...};
                            add(methodNode.argsInsnList { localVarIndex = it })
                            add(VarInsnNode(Opcodes.ASTORE, ++localVarIndex))
                            // 2.构建 JointPoint 实体 JointPoint jointPoint = new JointPoint(this, args);
                            add(newObject(JoinPoint::class.java, linkedMapOf(
                                Any::class.java to InsnList().apply { add(VarInsnNode(Opcodes.ALOAD, 0)) },
                                Array<Any?>::class.java to InsnList().apply { add(VarInsnNode(Opcodes.ALOAD, localVarIndex)) },
                            )))
                            add(VarInsnNode(Opcodes.ASTORE, ++localVarIndex))
                            joinPointInsnLast = last
                        })
                        // 将原始方法体中所有非(方法参数列表的本地变量索引)的索引增加插入的本地变量所占的索引大小
                        orgInsn.forEach fixEach@{
                            if (it !is VarInsnNode || it.`var` < localVarIndex - 1) return@fixEach
                            if (!it.opcode.opcodeIsLoad() && !it.opcode.opcodeIsStore()) return@fixEach
                            it.`var` += 2
                        }
                    }
                    // 3.将 Pointcut 和 JointPoint 连接 XXX.instance.xxxfun(jointPoint);
                    val insertCallAspectFuncInsn: (AbstractInsnNode) -> Unit = {
                        insnList.insertBefore(it, InsnList().apply {
                            add(FieldInsnNode(
                                Opcodes.GETSTATIC,
                                pointcut.aspectClassName,
                                "instance",
                                Type.getObjectType(pointcut.aspectClassName).descriptor
                            ))
                            if (aspectFunArgs.isNotEmpty()) add(VarInsnNode(Opcodes.ALOAD, localVarIndex))
                            add(MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                pointcut.aspectClassName,
                                pointcut.aspectFunName,
                                pointcut.aspectFunDesc,
                                false
                            ))
                        })
                    }
                    if (pointcut.position == 0) {
                        insertCallAspectFuncInsn(joinPointInsnLast?.next ?: firstLabel.next)
                    } else if (pointcut.position == -1) {
                        for (insnNode in insnList) {
                            if (insnNode.opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                                insertCallAspectFuncInsn(insnNode)
                            }
                        }
                    }
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