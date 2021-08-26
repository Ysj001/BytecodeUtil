package com.ysj.demo.plugin

import com.android.build.api.transform.Transform
import com.ysj.lib.bytecodeutil.modifier.IModifier
import org.objectweb.asm.tree.ClassNode

/**
 *
 *
 * @author Ysj
 * Create time: 2021/8/25
 */
class TestModifier(
    override val transform: Transform,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    override fun modify() {
    }

    override fun scan(classNode: ClassNode) {
        println("demo-plugin --> ${classNode.name}")

    }
}