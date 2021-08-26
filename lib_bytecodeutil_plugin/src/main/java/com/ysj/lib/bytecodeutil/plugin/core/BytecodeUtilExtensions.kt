package com.ysj.lib.bytecodeutil.plugin.core

import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import java.util.jar.JarEntry

/**
 * Gradle 扩展属性
 *
 * @author Ysj
 * Create time: 2021/8/23
 */
open class BytecodeUtilExtensions {

    companion object {
        const val NAME = "bytecodeUtil"
    }

    /** 设置日志等级 [YLogger]（verbose:0 ~ error:5） */
    var loggerLevel: Int = 0

    /** 用于确定哪些 [JarEntry] 不用处理，返回 true 表示不需要处理 */
    var notNeedJar: ((entryName: String) -> Boolean)? = null

    /** 附加的修改器 */
    var modifiers: Array<Class<*>>? = null

}