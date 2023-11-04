package com.ysj.lib.bytecodeutil.plugin.api

import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.Executor

/**
 * 修改器，用于改变字节码
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
interface IModifier {

    /**
     * 提供至少有 2 个线程的 [Executor]。
     */
    val executor: Executor

    /**
     *  每扫描到一个 class，就会往该集合中存一个。
     *  - key：[ClassNode.name]
     *  - value：[ClassNode]
     * */
    val allClassNode: Map<String, ClassNode>

    /**
     * 初始化
     */
    fun initialize(project: Project, variant: Variant) = Unit

    /**
     * 每扫到一个需要修改的类就会回调一次。
     * - 注意：该方法调用是并发的。
     */
    fun scan(classNode: ClassNode)

    /**
     * 开始修改 [scan] 到的类。
     */
    fun modify()

}