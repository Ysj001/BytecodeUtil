package com.ysj.lib.bytecodeutil.plugin.core

import com.ysj.lib.bytecodeutil.api.util.BCUKeep
import org.objectweb.asm.Type

/*
 * 定义常量
 *
 * @author Ysj
 * Create time: 2021/3/5
 */

val BCU_KEEP_DESC by lazy { Type.getType(BCUKeep::class.java).descriptor }

/**
 * 校验是否是 Android R 文件
 */
fun checkAndroidRFile(fileName: String) =
    fileName.endsWith("R.class")
            || fileName.endsWith("R\$raw.class")
            || fileName.endsWith("R\$styleable.class")
            || fileName.endsWith("R\$layout.class")
            || fileName.endsWith("R\$xml.class")
            || fileName.endsWith("R\$attr.class")
            || fileName.endsWith("R\$color.class")
            || fileName.endsWith("R\$bool.class")
            || fileName.endsWith("R\$mipmap.class")
            || fileName.endsWith("R\$dimen.class")
            || fileName.endsWith("R\$interpolator.class")
            || fileName.endsWith("R\$plurals.class")
            || fileName.endsWith("R\$style.class")
            || fileName.endsWith("R\$integer.class")
            || fileName.endsWith("R\$id.class")
            || fileName.endsWith("R\$animator.class")
            || fileName.endsWith("R\$string.class")
            || fileName.endsWith("R\$drawable.class")
            || fileName.endsWith("R\$anim.class")