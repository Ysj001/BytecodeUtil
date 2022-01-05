package com.ysj.lib.bytecodeutil.plugin.core

import com.android.Version
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.ModifierManager
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.modifier.lock
import com.ysj.lib.bytecodeutil.modifier.utils.*
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 *
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
class BytecodeTransform(private val project: Project) : Transform() {

    companion object {
        const val PLUGIN_NAME = "BytecodeUtilPlugin"
    }

    lateinit var extensions: BytecodeUtilExtensions

    private val logger = YLogger.getLogger(javaClass)

    private val modifierManager = ModifierManager(this)

    private lateinit var executor: ExecutorService

    private lateinit var jarCacheFile: File

    private lateinit var beforeJarCacheMap: Map<String, CacheInfo>

    private val currentJarCacheMap = ConcurrentHashMap<String, CacheInfo>()

    override fun getName(): String = PLUGIN_NAME

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    // 原始的增量策略过于暴力，只要有一个 jar 改变 TransformInvocation#isIncremental 直接为 false
    override fun isIncremental(): Boolean = true

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        doTransform(transformInvocation) { context,
                                           inputs,
                                           referencedInputs,
                                           outputProvider,
                                           isIncremental ->
            extensions.modifiers?.forEach {
                modifierManager.addModifier(
                    it as Class<out IModifier>,
                    project,
                    transformInvocation
                )
            }
            var oldTime = System.currentTimeMillis()
            val dirItems = LinkedList<Pair<File, ProcessItem>>()
            // 获取之前文件 cache 信息
            jarCacheFile = File(context.temporaryDir, "jar-cache-mapping.json")
            beforeJarCacheMap = if (jarCacheFile.exists()) jarCacheFile.fromJson() else emptyMap()
            logger.lifecycle(">>> load file md5 time：${System.currentTimeMillis() - oldTime}")
            oldTime = System.currentTimeMillis()
            // 预处理
            inputs.forEach { process(it.jarInputs, it.directoryInputs, outputProvider, dirItems) }
            beforeJarCacheMap.forEach { (key, value) ->
                if (currentJarCacheMap[key] != null) return@forEach
                val dest = File(value.cachePath)
                logger.verbose("remove file -- $key , ${dest.nameWithoutExtension}")
                if (dest.isDirectory) dest.deleteRecursively()
                dest.delete()
            }
            logger.lifecycle(">>> pre process time：${System.currentTimeMillis() - oldTime}")
            oldTime = System.currentTimeMillis()
            // 正式处理
            process(dirItems)
            logger.lifecycle(">>> process time：${System.currentTimeMillis() - oldTime}")
            // 重置文件 md5 信息
            jarCacheFile.delete()
            currentJarCacheMap.toJson(jarCacheFile)
        }
    }

    private fun process(
        jis: Collection<JarInput>,
        dis: Collection<DirectoryInput>,
        output: TransformOutputProvider,
        dirItems: LinkedList<Pair<File, ProcessItem>>,
    ) {
        var throwable: Throwable? = null
        val latch = CountDownLatch(jis.size + dis.size)
        jis.forEach { input ->
            executor.exec(latch, onError = { throwable = it }) {
                val src = input.file
                val beforeInfo = beforeJarCacheMap[input.name]
                val currentMd5 = src.inputStream().use { it.MD5_LOWER }
                val noIncrementalProcessJar: (String) -> String = { type ->
                    JarFile(src).use { jf ->
                        var needProcessJar = false
                        for (entry in jf.entries()) {
                            if (entry.notNeedJarEntries()) continue
                            needProcessJar = true
                            break
                        }
                        if (!needProcessJar) {
                            val destJar = output.getContentLocation(
                                input.name,
                                input.contentTypes,
                                input.scopes,
                                Format.JAR
                            )
                            destJar.delete()
                            src.copyTo(destJar)
                            logger.verbose("$type file -- ${src.name} , ${destJar.name}")
                            return@use destJar.absolutePath
                        }
                        val destDir = output.getContentLocation(
                            input.name,
                            input.contentTypes,
                            input.scopes,
                            Format.DIRECTORY
                        )
                        logger.verbose("$type file -- ${src.name} , ${destDir.name}")
                        for (entry in jf.entries()) {
                            if (entry.isDirectory) continue
                            val file = File(destDir, entry.name)
                            file.delete()
                            file.parentFile.mkdirs()
                            file.createNewFile()
                            jf.getInputStream(entry).use { jis ->
                                if (entry.notNeedJarEntries()) file.outputStream().use { fos ->
                                    jis.copyTo(fos)
                                } else {
                                    dirItems.lock { add(file to jis.visit()) }
                                }
                            }
                        }
                        destDir.absolutePath
                    }
                }
                val dest: String = when {
                    beforeInfo == null -> {
                        // 新增的
                        noIncrementalProcessJar("add")
                    }
                    beforeInfo.md5 != currentMd5 -> {
                        // 修改的
                        noIncrementalProcessJar("change")
                    }
                    else -> {
                        val destDir = output.getContentLocation(
                            input.name,
                            input.contentTypes,
                            input.scopes,
                            Format.DIRECTORY
                        )
                        logger.verbose("not change file -- ${src.name} , ${destDir.name}")
                        destDir.walk().forEach file@{
                            if (it.isDirectory) return@file
                            val jarEntryName = it.toRelativeString(destDir).replace("\\", "/")
                            if (jarEntryName.notNeedJarEntries()) return@file
                            dirItems.lock { add(it to it.inputStream().visit()) }
                        }
                        destDir.absolutePath
                    }
                }
                currentJarCacheMap[input.name] = CacheInfo(currentMd5, dest)
            }
        }
        throwable?.also { throw it }
        dis.forEach { input ->
            executor.exec(latch, onError = { throwable = it }) {
                val src = input.file
                val dest = output.getContentLocation(
                    input.name,
                    input.contentTypes,
                    input.scopes,
                    Format.DIRECTORY
                )
                dest.deleteRecursively()
                src.copyRecursively(dest)
                dest.walk().forEach file@{
                    if (!it.isFile || it.extension != "class") return@file
                    dirItems.lock { add(it to it.inputStream().visit()) }
                }
            }
        }
        latch.await()
        throwable?.also { throw it }
    }

    private fun process(dirItems: LinkedList<Pair<File, ProcessItem>>) {
        modifierManager.modify()
        var throwable: Throwable? = null
        val latch = CountDownLatch(dirItems.size)
        dirItems.forEach { pair ->
            val (dest, item) = pair
            executor.exec(latch, onError = { throwable = it }) {
                val cw = ClassWriter(item.classReader, 0)
                item.classNode.accept(cw)
                dest.outputStream().use { it.write(cw.toByteArray()) }
            }
        }
        latch.await()
        throwable?.also { throw it }
    }

    private fun InputStream.visit() = use {
        val cr = ClassReader(it)
        val cv = ClassNode()
        cr.accept(cv, 0)
        modifierManager.scan(cv)
        ProcessItem(cr, cv)
    }

    private fun JarEntry.notNeedJarEntries(): Boolean = name.notNeedJarEntries()

    private fun String.notNeedJarEntries(): Boolean =
        endsWith(".class").not()
            || checkAndroidRFile(this)
            || startsWith("META-INF/")
            || startsWith("com/ysj/lib/bytecodeutil/")
            || extensions.notNeedJar?.invoke(this) ?: false

    private inline fun doTransform(
        transformInvocation: TransformInvocation,
        block: (
            context: Context,
            inputs: Collection<TransformInput>,
            referencedInputs: Collection<TransformInput>,
            outputProvider: TransformOutputProvider,
            isIncremental: Boolean
        ) -> Unit
    ) {
        YLogger.LOGGER_LEVEL = extensions.loggerLevel
        logger.quiet("=================== $PLUGIN_NAME transform start ===================")
        logger.quiet(">>> gradle version: ${project.gradle.gradleVersion}")
        logger.quiet(">>> gradle plugin version: ${Version.ANDROID_GRADLE_PLUGIN_VERSION}")
        logger.quiet(">>> isIncremental: ${transformInvocation.isIncremental}")
        logger.quiet(">>> loggerLevel: ${YLogger.LOGGER_LEVEL}")
        val startTime = System.currentTimeMillis()
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        try {
            block(
                transformInvocation.context,
                transformInvocation.inputs,
                transformInvocation.referencedInputs,
                transformInvocation.outputProvider,
                transformInvocation.isIncremental
            )
        } finally {
            executor.shutdownNow()
        }
        logger.quiet(">>> total process time: ${System.currentTimeMillis() - startTime} ms")
        logger.quiet("=================== $PLUGIN_NAME transform end   ===================")
    }

    private class ProcessItem(
        val classReader: ClassReader,
        val classNode: ClassNode,
    )

    private class CacheInfo(
        /** 原始文件的 md5 */
        val md5: String,
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