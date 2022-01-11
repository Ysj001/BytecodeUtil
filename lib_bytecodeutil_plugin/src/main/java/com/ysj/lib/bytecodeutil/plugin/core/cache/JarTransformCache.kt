package com.ysj.lib.bytecodeutil.plugin.core.cache

import com.android.build.api.transform.TransformInput
import com.ysj.lib.bytecodeutil.modifier.cache.CacheStatus
import com.ysj.lib.bytecodeutil.modifier.utils.fromJson
import com.ysj.lib.bytecodeutil.modifier.utils.toJson
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Jar Transform 缓存
 *
 * @author Ysj
 * Create time: 2022/1/6 20:42
 */
class JarTransformCache(cacheDir: File) {

    private val cacheFile: File = File(cacheDir, "jar-cache-mapping.json")

    val cache: ConcurrentHashMap<String, CacheInfo> = if (cacheFile.exists()) cacheFile.fromJson() else ConcurrentHashMap()

    val hasCache get() = cache.isNotEmpty()

    operator fun set(key: String, value: CacheInfo) {
        cache[key] = value
    }

    /**
     * 获取 [CacheStatus]，不会有 [CacheStatus.REMOVED]。
     *
     * 处理 [CacheStatus.REMOVED] 需调用 [processRemoved]
     *
     * @param md5 jar 文件 md5
     */
    fun state(key: String, md5: String): CacheStatus {
        val beforeInfo = cache[key]
        return when {
            beforeInfo == null -> CacheStatus.ADDED
            beforeInfo.md5 != md5 -> CacheStatus.CHANGED
            else -> CacheStatus.NOT_CHANGED
        }
    }

    fun processRemoved(transformInput: Collection<TransformInput>, block: (File) -> Unit): Unit =
        cache.forEach { (key, value) ->
            if (transformInput.find { it.jarInputs.find { ji -> key == ji.name } != null } != null) return@forEach
            val dest = File(value.cachePath)
            if (!dest.exists()) return@forEach
            if (dest.isDirectory) dest.walkBottomUp().forEach { block(it);it.delete() }
            dest.delete()
        }

    fun saveCache() {
        cacheFile.delete()
        cache.toJson(cacheFile)
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