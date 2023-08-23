package com.ysj.lib.bytecodeutil.plugin.core.modifier.di

import com.ysj.lib.bytecodeutil.modifier.IModifier
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.Executor

/**
 * 用于实现 DI（依赖注入）的修改器
 *
 * @author Ysj
 * Create time: 2021/3/11
 */
class DIModifier(
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    override fun scan(classNode: ClassNode) {
        // TODO
    }

    override fun modify(executor: Executor) {
        // TODO
    }
}