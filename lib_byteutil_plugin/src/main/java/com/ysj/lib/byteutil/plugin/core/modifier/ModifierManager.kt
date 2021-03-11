package com.ysj.lib.byteutil.plugin.core.modifier

import com.android.build.api.transform.Transform
import com.ysj.lib.byteutil.plugin.core.modifier.impl.aspect.AspectModifier
import com.ysj.lib.byteutil.plugin.core.modifier.impl.di.DIModifier
import org.objectweb.asm.tree.ClassNode

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager(override val transform: Transform) : IModifier {

    override val allClassNode: HashMap<String, ClassNode> by lazy { HashMap() }

    private val modifiers: Collection<IModifier> by lazy {
        arrayListOf(
            AspectModifier(transform, allClassNode),
            DIModifier(transform, allClassNode),
        )
    }

    override fun scan(classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(classNode) }
    }

    override fun modify(classNode: ClassNode) = modifiers.forEach { it.modify(classNode) }
}