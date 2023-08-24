package com.ysj.lib.bytecodeutil.plugin.api

/**
 * BytecodeUtil 插件编译后需要保留不能混淆的方法。
 * - 编译插件会自动对需要保留的目标标记该注解
 *
 * @author Ysj
 * Create time: 2021/9/24
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BCUKeep
