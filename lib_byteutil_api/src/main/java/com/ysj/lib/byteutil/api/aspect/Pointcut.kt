package com.ysj.lib.byteutil.api.aspect

import java.util.regex.Pattern

/**
 * 定义切入点
 * 该注解注释的方法将会在该注解的参数所标识的位置调用
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Pointcut(
    /**
     * 目标切入点
     *
     * 不同的前缀表达不同的含义：
     * - "class:" --> 类
     * - "superClass:" --> 父类
     * - "interface:" --> 接口
     * - "annotation:" --> 注解
     */
    val target: String,
    /**
     * 切入点方法名，用于确定切入的方法
     *
     * 采用正则表达式 [Pattern.matches] 来匹配
     */
    val funName: String,
    /**
     * 切入点方法描述，用于确定切入的具体方法
     *
     * 采用正则表达式 [Pattern.matches] 来匹配
     */
    val funDesc: String,
    /**
     * 切入点的具体执行位置
     * - -1 表示插入方法末尾
     * - 0 表示插入方法开头
     * - 其它 表示插入到方法中任意位置
     */
    val position: Int,
)