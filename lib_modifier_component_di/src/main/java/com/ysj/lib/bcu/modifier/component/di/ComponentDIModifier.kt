package com.ysj.lib.bcu.modifier.component.di

import com.ysj.lib.bcu.modifier.component.di.api.Component
import com.ysj.lib.bcu.modifier.component.di.api.ComponentImpl
import com.ysj.lib.bcu.modifier.component.di.api.ComponentInject
import com.ysj.lib.bytecodeutil.plugin.api.IModifier
import com.ysj.lib.bytecodeutil.plugin.api.firstNode
import com.ysj.lib.bytecodeutil.plugin.api.isStatic
import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
import org.gradle.api.Project
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.util.LinkedList
import java.util.concurrent.Executor

/**
 * 用于处理组件注入的 [IModifier] 实现。
 *
 * @author Ysj
 * Create time: 2023/8/28
 */
class ComponentDIModifier(
    override val executor: Executor,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    private val logger = YLogger.getLogger(javaClass)

    /**
     * 被 [Component] 注解的类信息与类名的映射。
     */
    private val componentInfoMap = HashMap<String, ComponentInfo>()

    /**
     * 被 [ComponentImpl] 注解的类的集合。
     */
    private val componentImplList = LinkedList<ClassNode>()

    /**
     * 被 [ComponentInject] 注解的 field 的集合。
     */
    private val componentInjectInfoMap = HashMap<ClassNode, LinkedList<FieldNode>>()

    private val findInjectTargetCache = HashMap<String, InjectTarget>()

    private var checkImpl = true

    override fun initialize(project: Project) {
        super.initialize(project)
        checkImpl = project.properties["component.di.checkImpl"] as Boolean? ?: checkImpl
        logger.lifecycle("component.di.checkImpl=$checkImpl")
    }

    override fun scan(classNode: ClassNode) {
        classNode.invisibleAnnotations?.forEach { ann ->
            when (ann.desc) {
                COMPONENT_DES -> {
                    val componentInfo = ComponentInfo(classNode, ann)
                    synchronized(componentInfoMap) {
                        componentInfoMap[classNode.name] = componentInfo
                    }
                }
                COMPONENT_IMPL_DES -> {
                    synchronized(componentImplList) {
                        componentImplList += classNode
                    }
                }
            }
        }
        var fieldNodes = synchronized(componentInjectInfoMap) {
            componentInjectInfoMap[classNode]
        }
        for (index in classNode.fields.indices) {
            val fn = classNode.fields[index]
            val componentInject = fn
                .invisibleAnnotations
                ?.find { it.desc == COMPONENT_INJECT_DES }
                ?: continue
            if (fieldNodes == null) {
                fieldNodes = LinkedList()
                synchronized(componentInjectInfoMap) {
                    componentInjectInfoMap[classNode] = fieldNodes
                }
            }
            fieldNodes += fn
        }
    }

    override fun modify() {
        for (entry in componentInjectInfoMap) {
            val (classNode, fieldNodes) = entry
            for (index in fieldNodes.indices) {
                inject(classNode, findInjectTarget(classNode, fieldNodes[index]))
            }
        }
    }

    private fun inject(classNode: ClassNode, target: InjectTarget) {
        val isStatic = target.fieldNode.isStatic
        val constructors = if (isStatic) {
            listOf(classNode.getStaticConstructor())
        } else {
            classNode.findMainConstructors()
        }
        requireNotNull(constructors) {
            "在 ${classNode.name} 中没有找到可用的构造器"
        }
        val targetClassNode = target.classNode
        if (targetClassNode == null && checkImpl) {
            throw IllegalArgumentException("没有找到 ${target.componentInfo.classNode.name} 的实现")
        }
        val componentType = Type.getObjectType(target.componentInfo.classNode.name)
        val staticInstanceField = targetClassNode?.findStaticInstanceField()
        for (constructor in constructors) {
            val list = InsnList()
            constructor.instructions.insertBefore(constructor.firstNode, list)
            if (!isStatic) {
                list.add(VarInsnNode(Opcodes.ALOAD, 0))
            }
            when {
                targetClassNode == null -> {
                    list.add(LdcInsnNode(componentType))
                    list.add(MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        UTILS_INTERNAL_NAME,
                        "cpiProxy",
                        "(Ljava/lang/Class;)Ljava/lang/Object;",
                        false
                    ))
                }
                staticInstanceField != null -> list.add(MethodInsnNode(
                    Opcodes.GETSTATIC,
                    targetClassNode.name,
                    staticInstanceField.name,
                    staticInstanceField.desc,
                    false
                ))
                else -> {
                    list.add(TypeInsnNode(Opcodes.NEW, targetClassNode.name))
                    list.add(InsnNode(Opcodes.DUP))
                    list.add(MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        targetClassNode.name,
                        "<init>",
                        "()V",
                        false
                    ))
                }
            }
            list.add(FieldInsnNode(
                if (isStatic) Opcodes.PUTSTATIC else Opcodes.PUTFIELD,
                classNode.name,
                target.fieldNode.name,
                target.fieldNode.desc
            ))
        }
    }

    private fun findInjectTarget(classNode: ClassNode, fieldNode: FieldNode): InjectTarget {
        var target = findInjectTargetCache[fieldNode.desc]
        if (target != null) {
            return target
        }
        val typeName = Type.getType(fieldNode.desc).internalName
        // 根据注解的 field 的类型找到组件接口
        val componentInfo = requireNotNull(componentInfoMap[typeName]) {
            "${classNode.name}.${fieldNode.name} 字段的类型没有使用 @${Component::class.java.simpleName}"
        }
        // 查找组件接口的实现类
        var resultImplNode: ClassNode? = null
        for (componentImplNode in componentImplList) {
            if (!componentImplNode.findInterface(componentInfo.classNode.name)) {
                continue
            }
            if (resultImplNode != null) {
                throw IllegalArgumentException(
                    """
                    | 发现 ${componentInfo.classNode.name} 有重复实现：
                    | 1. ${resultImplNode.name}
                    | 2. ${componentImplNode.name}
                    """.trimMargin()
                )
            }
            resultImplNode = componentImplNode
        }
        target = InjectTarget(
            fieldNode = fieldNode,
            classNode = resultImplNode,
            componentInfo = componentInfo,
        )
        findInjectTargetCache[fieldNode.desc] = target
        return target
    }

    private fun ClassNode.getStaticConstructor(): MethodNode {
        for (index in methods.indices) {
            if (methods[index].run { name == "<clinit>" && desc == "()V" }) {
                return methods[index]
            }
        }
        val constructor = MethodNode(
            Opcodes.ACC_STATIC,
            "<clinit>",
            "()V",
            null,
            null
        )
        // 刚创建出来的 instructions 是空的，必需要添加个 return
        constructor.instructions.add(InsnNode(Opcodes.RETURN))
        methods.add(0, constructor)
        return constructor
    }

    private fun ClassNode.findMainConstructors(): Collection<MethodNode>? {
        var constructors: HashMap<String, MethodNode>? = null
        for (index in methods.indices) {
            val methodNode = methods[index]
            if (methodNode.name != "<init>") {
                continue
            }
            var node: AbstractInsnNode? = methodNode.instructions.first
            while (node != null && node.opcode != Opcodes.INVOKESPECIAL) {
                node = node.next
            }
            if (node !is MethodInsnNode) {
                continue
            }
            if (constructors == null) {
                constructors = HashMap()
            } else if ("${node.owner}${node.name}$${node.desc}" in constructors) {
                continue
            }
            constructors["${name}${methodNode.name}${methodNode.desc}"] = methodNode
        }
        return constructors?.values
    }

    private fun ClassNode.findInterface(targetInterfaceName: String): Boolean {
        for (index in interfaces.indices) {
            if (interfaces[index] == targetInterfaceName) {
                return true
            }
        }
        for (index in interfaces.indices) {
            val classNode = allClassNode[interfaces[index]] ?: continue
            if (classNode.findInterface(targetInterfaceName)) {
                return true
            }
        }
        return false
    }

    // 查找 public static final <Class> INSTANCE;
    private fun ClassNode.findStaticInstanceField() = fields.find {
        it.access == Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_STATIC
            && it.name == "INSTANCE"
            && it.desc == Type.getObjectType(name).descriptor
    }

    private class InjectTarget(
        /**
         * 要注入的 field。
         */
        val fieldNode: FieldNode,
        /**
         * field 需要的实例 class。
         */
        val classNode: ClassNode?,
        /**
         * [ComponentInfo]。
         */
        val componentInfo: ComponentInfo,
    )

    private class ComponentInfo(
        /**
         * 被 [Component] 注解的类。
         */
        val classNode: ClassNode,
        /**
         * [Component] 注解。
         */
        val componentAnnotationNode: AnnotationNode,
    )

}