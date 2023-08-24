package com.ysj.lib.bytecodeutil.plugin.core

import com.ysj.lib.bytecodeutil.api.logger.YLogger

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

    /** 用于确定哪些 Class 不用处理，返回 true 表示不需要处理 */
    var notNeed: ((entryName: String) -> Boolean) = { false }

    /** 附加的修改器 */
    var modifiers: Array<Class<*>>? = null

}