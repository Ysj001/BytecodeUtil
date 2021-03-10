package com.ysj.lib.byteutil.plugin.core.modifier

import com.android.build.api.transform.Transform
import org.objectweb.asm.tree.ClassNode

/**
 * 修改器，用于改变字节码
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
interface IModifier {

    val transform: Transform

    /** 所有扫描到的 class。key：[ClassNode.name] */
    val allClassNode: Map<String, ClassNode>

    /**
     * 扫描类信息，用于确定要修改的类，该方法无法操作字节码
     */
    fun scan(classNode: ClassNode)

    /**
     * 开始修改
     */
    fun modify(classNode: ClassNode)
}