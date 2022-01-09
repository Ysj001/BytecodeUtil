package com.ysj.lib.bytecodeutil.plugin.core.modifier.di

import com.android.build.api.transform.Transform
import com.ysj.lib.bytecodeutil.modifier.IModifier
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * 用于实现 DI（依赖注入）的修改器
 *
 * @author Ysj
 * Create time: 2021/3/11
 */
class DIModifier(
    override val transform: Transform,
    override val executor: ExecutorService,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    override fun modify() {
        TODO("Not yet implemented")
    }

    override fun scan(destClassFile: File, classNode: ClassNode) {
        TODO("Not yet implemented")
    }


}