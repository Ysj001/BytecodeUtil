package com.ysj.lib.bytecodeutil.plugin.core.logger

interface ILogger {
    /**
     * Prints an error message.
     *
     * @param t is an optional [Throwable] or [Exception]. If non-null, its
     * message will be printed out.
     * @param msgFormat is an optional error format. If non-null, it will be printed
     * using a [Formatter] with the provided arguments.
     * @param args provides the arguments for errorFormat.
     */
    fun error(t: Throwable?, msgFormat: String?, vararg args: Any?)

    /**
     * Prints a warning message.
     *
     * @param msgFormat is a string format to be used with a [Formatter]. Cannot be null.
     * @param args provides the arguments for warningFormat.
     */
    fun warning(msgFormat: String?, vararg args: Any?)

    /**
     * Prints an important information message. Defaults to info, but mapped to quiet in the Android
     * Gradle Plugin.
     *
     *
     * See [Gradle Logging
 * Documentation.](https://docs.gradle.org/current/userguide/logging.html).
     *
     * @param msgFormat is a string format to be used with a [Formatter]. Cannot be null.
     * @param args provides the arguments for msgFormat.
     */
    fun quiet(msgFormat: String?, vararg args: Any?) {
        info(msgFormat, *args)
    }

    /**
     * Prints a progress information message. Defaults to info, but mapped to lifecycle in the
     * Android Gradle Plugin.
     *
     *
     * See [Gradle Logging
     * Documentation.](https://docs.gradle.org/current/userguide/logging.html).
     *
     * @param msgFormat is a string format to be used with a [Formatter]. Cannot be null.
     * @param args provides the arguments for msgFormat.
     */
    fun lifecycle(msgFormat: String?, vararg args: Any?) {
        info(msgFormat, *args)
    }

    /**
     * Prints an information message.
     *
     * @param msgFormat is a string format to be used with a [Formatter]. Cannot be null.
     * @param args provides the arguments for msgFormat.
     */
    fun info(msgFormat: String?, vararg args: Any?)

    /**
     * Prints a verbose message.
     *
     * @param msgFormat is a string format to be used with a [Formatter]. Cannot be null.
     * @param args provides the arguments for msgFormat.
     */
    fun verbose(msgFormat: String?, vararg args: Any?)
}