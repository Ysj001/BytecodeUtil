package com.ysj.lib.bytecodeutil.modifier

import com.android.build.api.transform.Transform
import org.objectweb.asm.tree.ClassNode

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager(override val transform: Transform) : IModifier {

    override val allClassNode: HashMap<String, ClassNode> by lazy { HashMap() }

    private val modifiers: Collection<IModifier> by lazy { ArrayList() }

    override fun scan(classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(classNode) }
    }

    override fun modify(classNode: ClassNode) = modifiers.forEach { it.modify(classNode) }

    fun addModifier(modifier: Class<out IModifier>) {
        val constructor = modifier.getConstructor(Transform::class.java, Map::class.java)
        (modifiers as ArrayList).add(constructor.newInstance(transform, allClassNode))
    }
}