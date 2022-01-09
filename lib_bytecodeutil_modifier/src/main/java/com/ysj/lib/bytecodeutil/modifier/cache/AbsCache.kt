package com.ysj.lib.bytecodeutil.modifier.cache

import com.ysj.lib.bytecodeutil.modifier.utils.toJson
import java.io.File

/**
 * 定义抽象的缓存对象
 *
 * @author Ysj
 * Create time: 2022/1/9 12:10
 */
abstract class AbsCache<CacheValue> {

    protected abstract val cacheFile: File

    protected abstract var beforeCache: MutableMap<String, CacheValue>

    protected abstract val currentCache: MutableMap<String, CacheValue>

    /** 有缓存返回 true */
    open val hasCache: Boolean get() = beforeCache.isNotEmpty()

    /**
     * 从 [currentCache] 获取缓存
     */
    open operator fun get(key: String): CacheValue? = currentCache[key]

    /**
     * 添加一个缓存到 [currentCache]
     */
    open operator fun set(key: String, value: CacheValue) {
        currentCache[key] = value
    }

    /**
     * 从 [beforeCache] 获取缓存
     */
    open fun beforeValue(key: String): CacheValue? = beforeCache[key]

    /**
     * 储存缓存。
     */
    open fun saveCache() {
        cacheFile.delete()
        currentCache.toJson(cacheFile)
        beforeCache.clear()
        beforeCache.putAll(currentCache)
        currentCache.clear()
    }
}
