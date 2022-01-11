package com.ysj.lib.bytecodeutil.modifier

import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.ysj.lib.bytecodeutil.modifier.cache.CacheStatus
import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * 修改器，用于改变字节码
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
interface IModifier {

    val transform: Transform

    /** 线程数量：[Runtime.availableProcessors] */
    val executor: ExecutorService

    /** 所有扫描到的 class。key：[ClassNode.name] */
    val allClassNode: Map<String, ClassNode>

    /**
     * 初始化
     */
    fun initialize(project: Project, transformInvocation: TransformInvocation) = Unit

    /**
     * 决定 Transform 是否增量
     *
     * @param classFile 需要检查该文件是否影响 Transform
     * @return 返回 ture 则能增量，返回 false 则整个 Transform 不增量
     */
    fun canIncremental(classFile: File, cacheStatus: CacheStatus): Boolean = false

    /**
     * 扫描类所有需要修改的类，每扫描到一个类会回调一次该方法
     *
     * @param destClassFile 将输出的 class 文件。此时内容是空的，在 [modifyEnd] 后才有内容
     */
    fun scan(destClassFile: File, classNode: ClassNode)

    /**
     * 开始修改 [scan] 到的类
     */
    fun modify(isIncremental: Boolean)

    /**
     * 修改结束，此时修改的字节码已经存到文件了
     */
    fun modifyEnd() = Unit
}