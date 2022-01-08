package com.ysj.lib.bytecodeutil.plugin.core.cache

import com.ysj.lib.bytecodeutil.modifier.utils.fromJson
import com.ysj.lib.bytecodeutil.modifier.utils.toJson
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Jar Transform 缓存
 *
 * @author Ysj
 * Create time: 2022/1/6 20:42
 */
class JarTransformCache(cacheDir: File, private val logger: YLogger) {

    private val jarCacheFile: File = File(cacheDir, "jar-cache-mapping.json")

    private var beforeJarCacheMap: Map<String, CacheInfo> = if (jarCacheFile.exists()) jarCacheFile.fromJson() else emptyMap()

    private val currentJarCacheMap = ConcurrentHashMap<String, CacheInfo>()

    operator fun set(key: String, value: CacheInfo) {
        currentJarCacheMap[key] = value
    }

    operator fun get(key: String): CacheInfo? = currentJarCacheMap[key]

    fun beforeInfo(key: String): CacheInfo? = beforeJarCacheMap[key]

    /**
     * 获取 [CacheStatus]，不会有 [CacheStatus.REMOVED]。
     *
     * 处理 [CacheStatus.REMOVED] 需调用 [processRemoved]
     *
     * @param md5 jar 文件 md5
     */
    fun state(key: String, md5: String): CacheStatus {
        val beforeInfo = beforeInfo(key)
        return when {
            beforeInfo == null -> CacheStatus.ADDED
            beforeInfo.md5 != md5 -> CacheStatus.CHANGED
            else -> CacheStatus.NOT_CHANGED
        }
    }

    fun processRemoved(block: (CacheInfo) -> Unit): Unit = beforeJarCacheMap.forEach { (key, value) ->
        if (currentJarCacheMap[key] != null) return@forEach
        val dest = File(value.cachePath)
        logger.verbose("remove file -- $key , ${dest.nameWithoutExtension}")
        block(value)
        if (dest.isDirectory) dest.deleteRecursively()
        dest.delete()
    }

    /**
     * 将缓存设置成新的
     */
    fun refreshCache() {
        jarCacheFile.delete()
        currentJarCacheMap.toJson(jarCacheFile)
        beforeJarCacheMap = currentJarCacheMap
        currentJarCacheMap.clear()
    }

    class CacheInfo(
        /** 原始文件的 md5 */
        val md5: String,
        /** 缓存的 jar file，或 jar 解压后的 dir */
        val cachePath: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CacheInfo
            if (md5 != other.md5) return false
            return true
        }

        override fun hashCode(): Int {
            return md5.hashCode()
        }
    }
}