package com.ysj.lib.bytecodeutil.api.util

import java.util.*

/**
 * įŽåį LruCache
 *
 * @author Ysj
 * Create time: 2021/9/13
 */
class LruCache<K, V>(private var maxSize: Int = 16) : LinkedHashMap<K, V>(0, 0.75f, true) {
    companion object {
        fun <K, V> sync(maxSize: Int = 16): MutableMap<K, V> =
            Collections.synchronizedMap(LruCache<K, V>(maxSize))
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
}