package com.ysj.lib.bytecodeutil.plugin

import com.android.build.api.variant.Variant
import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger

/*
 * bcu 相关的扩展
 *
 * @author Ysj
 * Create time: 2021/8/23
 */

const val EXTENSION_NAME = "bcu"

interface BCUExtension {
    /**
     * 针对每个 [variant] 生成 [Config]。
     */
    fun config(callback: Config.(Variant) -> Unit)

    /**
     * 用于确定哪些 Class 不用处理，返回 true 表示不需要处理。
     */
    fun filterNot(callback: (Variant, String) -> Boolean)
}

class Config {

    /**
     * 设置日志等级 [YLogger]（verbose:0 ~ error:5）
     */
    var loggerLevel: Int = 0

    /**
     * 附加的修改器。
     */
    var modifiers: Array<Class<*>>? = null

}

internal open class BCUExtensionImpl : BCUExtension {

    lateinit var config: Config.(Variant) -> Unit
    lateinit var filterNot: (Variant, String) -> Boolean

    override fun config(callback: Config.(Variant) -> Unit) {
        config = callback
    }

    override fun filterNot(callback: (Variant, String) -> Boolean) {
        filterNot = callback
    }

}