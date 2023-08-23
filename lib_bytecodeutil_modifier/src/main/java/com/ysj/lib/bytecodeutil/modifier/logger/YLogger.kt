package com.ysj.lib.bytecodeutil.modifier.logger

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * 日志包装
 *
 * @author Ysj
 * Create time: 2020/10/23
 */
class YLogger private constructor(private val logger: Logger) : ILogger {

    companion object {
        var LOGGER_LEVEL: Int = 1

        fun getLogger(clazz: Class<*>) = YLogger(Logging.getLogger(clazz))
    }

    override fun error(t: Throwable?, msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 5) return
        logger.log(LogLevel.ERROR, msgFormat, t)
    }

    override fun warning(msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 4) return
        logger.log(LogLevel.WARN, msgFormat, *args)
    }

    override fun quiet(msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 3) return
        logger.quiet(msgFormat, *args)
    }

    override fun lifecycle(msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 2) return
        logger.lifecycle(msgFormat, *args)
    }

    override fun info(msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 1) return
        logger.lifecycle(msgFormat, *args)
    }

    override fun verbose(msgFormat: String?, vararg args: Any?) {
        if (LOGGER_LEVEL > 0) return
        logger.lifecycle(msgFormat, *args)
    }
}