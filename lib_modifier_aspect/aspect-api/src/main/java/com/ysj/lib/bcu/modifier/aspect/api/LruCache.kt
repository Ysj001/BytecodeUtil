package com.ysj.lib.bcu.modifier.aspect.api

import java.util.*

/**
 * 简单的 LruCache
 *
 * @author Ysj
 * Create time: 2021/9/13
 */
internal class LruCache<K, V>(private var maxSize: Int = 16) : LinkedHashMap<K, V>(0, 0.75f, true) {
    companion object {
        fun <K, V> sync(maxSize: Int = 16): MutableMap<K, V> =
            Collections.synchronizedMap(LruCache<K, V>(maxSize))
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
}