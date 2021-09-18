package com.ysj.lib.bytecodeutil.api.util

/*
 * 一些常用扩展
 *
 * @author Ysj
 * Create time: 2021/9/18
 */

/**
 * list 的遍历查找，不会创建迭代器
 */
inline fun <T> List<T>.listFind(block: (T) -> Boolean): T? {
    for (i in 0..lastIndex) {
        val get = get(i)
        if (block(get)) return get
    }
    return null
}