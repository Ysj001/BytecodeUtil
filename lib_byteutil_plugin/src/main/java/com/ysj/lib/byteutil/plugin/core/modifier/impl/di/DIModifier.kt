package com.ysj.lib.byteutil.plugin.core.modifier.impl.di

import com.android.build.api.transform.Transform
import com.ysj.lib.byteutil.plugin.core.modifier.IModifier
import org.objectweb.asm.tree.ClassNode

/**
 * 用于实现 DI（依赖注入）的修改器
 *
 * @author Ysj
 * Create time: 2021/3/11
 */
class DIModifier(
    override val transform: Transform,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    override fun scan(classNode: ClassNode) {
        // TODO
    }

    override fun modify(classNode: ClassNode) {
        // TODO
    }
}