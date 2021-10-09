package com.ysj.lib.bytecodeutil.modifier

import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager(override val transform: Transform) : IModifier {

    override val allClassNode: HashMap<String, ClassNode> by lazy { HashMap() }

    private val modifiers: MutableCollection<IModifier> by lazy { ArrayList() }

    override fun scan(classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(classNode) }
    }

    override fun modify() {
        val iterator = modifiers.iterator()
        while (iterator.hasNext()) {
            iterator.next().modify()
            // 用完就移除，节约内存，避免 OOM
            iterator.remove()
        }
    }

    fun addModifier(modifier: Class<out IModifier>, project: Project, transformInvocation: TransformInvocation) {
        val constructor = modifier.getConstructor(Transform::class.java, Map::class.java)
        val element = constructor.newInstance(transform, allClassNode)
        (modifiers as ArrayList).add(element)
        element.initialize(project, transformInvocation)
    }
}