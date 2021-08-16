package com.ysj.lib.bytecodeutil.plugin.core.logger

import com.android.build.gradle.internal.LoggerWrapper
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * 日志包装
 *
 * @author Ysj
 * Create time: 2020/10/23
 */
class YLogger private constructor(logger: Logger) : LoggerWrapper(logger) {

    companion object {
        var LOGGER_LEVEL: Int = 1

        fun getLogger(clazz: Class<*>) = YLogger(Logging.getLogger(clazz))
    }

    override fun error(throwable: Throwable?, s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 5) return
        super.error(throwable, s, *objects)
    }

    override fun warning(s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 4) return
        super.warning(s, *objects)
    }

    override fun quiet(s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 3) return
        super.quiet(s, *objects)
    }

    override fun lifecycle(s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 2) return
        super.lifecycle(s, *objects)
    }

    override fun info(s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 1) return
        super.lifecycle(s, *objects)
    }

    override fun verbose(s: String?, vararg objects: Any?) {
        if (LOGGER_LEVEL > 0) return
        super.lifecycle(s, *objects)
    }
}