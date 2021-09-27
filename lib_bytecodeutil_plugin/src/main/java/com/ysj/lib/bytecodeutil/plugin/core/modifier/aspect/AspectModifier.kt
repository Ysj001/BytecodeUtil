package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect

import com.android.build.api.transform.Transform
import com.ysj.lib.bytecodeutil.api.aspect.*
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.modifier.params
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.processor.MethodInnerProcessor
import com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.processor.MethodProxyProcessor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
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

    private val targetClass by lazy { LinkedList<PointcutBean>() }

    private val targetSuperClass by lazy { LinkedList<PointcutBean>() }

    private val targetInterface by lazy { LinkedList<PointcutBean>() }

    private val targetAnnotation by lazy { LinkedList<PointcutBean>() }

    private val methodInnerProcessor by lazy { MethodInnerProcessor(this) }

    private val methodProxyProcessor by lazy { MethodProxyProcessor(this) }

    override fun scan(classNode: ClassNode) {
        // 过滤所有没有 Aspect 注解的类
        if (classNode.invisibleAnnotations?.find { it.desc == ANNOTATION_ASPECT_DESC } == null) return
        classNode.methods.forEach {
            // 查找 Pointcut 注解的方法
            val pointCutAnnotation = it.invisibleAnnotations
                ?.find { anode -> anode.desc == ANNOTATION_POINTCUT_DESC }
                ?: return@forEach
            // 收集 Pointcut 注解的参数
            val params = pointCutAnnotation.params()
            val orgTarget = params[Pointcut::target.name] as String
            val target = orgTarget.substringAfter(":")
            val targetType = orgTarget.substringBefore(":")
            val collection = when (targetType) {
                PointcutBean.TARGET_CLASS -> targetClass
                PointcutBean.TARGET_SUPER_CLASS -> targetSuperClass
                PointcutBean.TARGET_INTERFACE -> targetInterface
                PointcutBean.TARGET_ANNOTATION -> targetAnnotation
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
            collection.add(pointcutBean)
        }
    }

    override fun modify() {
        val old = System.currentTimeMillis()
        var throwable: Throwable? = null
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val latch = CountDownLatch(allClassNode.size)
        allClassNode.forEach {
            // 注意这里没加锁，内部不要多线程修改
            executor.exec(latch, onError = { throwable = it }) {
                handleAspect(it.value)
                handlePointcut(it.value)
            }
        }
        latch.await()
        executor.shutdown()
        throwable?.also { throw it }
        logger.lifecycle(">>> ${javaClass.simpleName} process time：${System.currentTimeMillis() - old}")
    }

    /**
     * 在 [Aspect] 注解的类中添加用于获取该类实例的静态成员
     */
    private fun handleAspect(classNode: ClassNode) {
        // 过滤所有没有 Aspect 注解的类
        if (classNode.invisibleAnnotations?.find { it.desc == ANNOTATION_ASPECT_DESC } == null) return
        val access = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        val desc = Type.getObjectType(classNode.name).descriptor
        if (classNode.fields.find { it.access == access && it.name == ASPECT_CLASS_INSTANCE && it.desc == desc } != null) return
        val fieldInstance = FieldNode(access, ASPECT_CLASS_INSTANCE, desc, null, null)
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
        findPointcuts(classNode) { pointcut, methodNode ->
            methodProxyProcessor.process(pointcut, classNode, methodNode)
            methodInnerProcessor.process(pointcut, classNode, methodNode)
        }
    }

    /**
     * 查找类中所有的切入点
     */
    private inline fun findPointcuts(classNode: ClassNode, block: (PointcutBean, MethodNode) -> Unit) {
        ArrayList(classNode.methods).forEach { mn ->
            // 查找类中的
            targetClass.forEach target@{
                if (it.position == POSITION_CALL) block(it, mn)
                else {
                    if (!Pattern.matches(it.target, classNode.name)) return@target
                    if (!Pattern.matches(it.funName, mn.name)) return@target
                    if (!Pattern.matches(it.funDesc, mn.desc)) return@target
                    block(it, mn)
                }
            }
            // 查找父类中的
            for (it in targetSuperClass) {
                if (it.position == POSITION_CALL) {
                    block(it, mn)
                    continue
                }
                if (it.matchSuperClass(mn, classNode.superName)) block(it, mn)
            }
            // 查找接口中的
            for (it in targetInterface) {
                if (it.position == POSITION_CALL) {
                    block(it, mn)
                    continue
                }
                if (it.matchInterface(classNode, mn)) block(it, mn)
            }
            // 查找注解中的
            targetAnnotation.forEach pb@{ pb ->
                if (pb.position == POSITION_CALL) {
                    block(pb, mn)
                    return@pb
                }
                mn.visibleAnnotations?.forEach { if (Pattern.matches(pb.target, it.desc)) block(pb, mn) }
                mn.invisibleAnnotations?.forEach { if (Pattern.matches(pb.target, it.desc)) block(pb, mn) }
            }
        }
    }

    private fun PointcutBean.matchInterface(cn: ClassNode, mn: MethodNode): Boolean {
        return (matchInterface(target, cn.interfaces) &&
                Pattern.matches(funName, mn.name) &&
                Pattern.matches(funDesc, mn.desc)) ||
                matchInterface(allClassNode[cn.superName] ?: return false, mn)
    }

    private fun matchInterface(target: String, interfaces: List<String>?): Boolean {
        interfaces?.forEachIndexed { _, itf ->
            if (
                Pattern.matches(target, itf) ||
                matchInterface(target, allClassNode[itf]?.interfaces)
            ) return true
        }
        return false
    }

    private fun PointcutBean.matchSuperClass(mn: MethodNode, superName: String): Boolean {
        return (Pattern.matches(target, superName) &&
                Pattern.matches(funName, mn.name) &&
                Pattern.matches(funDesc, mn.desc)) ||
                matchSuperClass(mn, allClassNode[superName]?.superName ?: return false)
    }
}