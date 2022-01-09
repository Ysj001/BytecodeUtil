package com.ysj.lib.bytecodeutil.plugin.core

import com.android.Version
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.ModifierManager
import com.ysj.lib.bytecodeutil.modifier.cache.CacheStatus
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.modifier.lock
import com.ysj.lib.bytecodeutil.modifier.utils.*
import com.ysj.lib.bytecodeutil.plugin.core.cache.JarTransformCache
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
import kotlin.collections.ArrayList

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

    private lateinit var jarTransformCache: JarTransformCache

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
            val items = object : LinkedList<ProcessItem>() {
                override fun add(element: ProcessItem): Boolean {
                    modifierManager.scan(element.classNode)
                    return super.add(element)
                }
            }
            jarTransformCache = JarTransformCache(context.temporaryDir, logger)
            logger.lifecycle(">>> load file md5 time：${System.currentTimeMillis() - oldTime}")
            oldTime = System.currentTimeMillis()
            // 预处理
            inputs.forEach { process(it.jarInputs, it.directoryInputs, outputProvider, items) }
            jarTransformCache.processRemoved {
                // todo
            }
            logger.lifecycle(">>> pre process time：${System.currentTimeMillis() - oldTime}")
            oldTime = System.currentTimeMillis()
            // 正式处理
            process(items)
            logger.lifecycle(">>> process time：${System.currentTimeMillis() - oldTime}")
            jarTransformCache.saveCache()
        }
    }

    private fun process(
        jis: Collection<JarInput>,
        dis: Collection<DirectoryInput>,
        output: TransformOutputProvider,
        items: MutableCollection<ProcessItem>,
    ) {
        var throwable: Throwable? = null
        val latch = CountDownLatch(jis.size + dis.size)
        jis.forEach { input ->
            executor.exec(latch, onError = { throwable = it }) {
                val src = input.file
                val currentMd5 = src.inputStream().use { it.MD5_LOWER }
                val noIncrementalProcessJar: () -> File = {
                    JarFile(src).use { jf ->
                        var needProcessJar = false
                        val dest = lazy(LazyThreadSafetyMode.NONE) {
                            if (needProcessJar) output.getContentLocation(
                                input.name,
                                input.contentTypes,
                                input.scopes,
                                Format.DIRECTORY
                            ) else output.getContentLocation(
                                input.name,
                                input.contentTypes,
                                input.scopes,
                                Format.JAR
                            )
                        }
                        val file: (JarEntry) -> File = { entry ->
                            File(dest.value, entry.name).also {
                                it.delete()
                                it.parentFile.mkdirs()
                                it.createNewFile()
                            }
                        }
                        val notNeeds = ArrayList<() -> Unit>(jf.size())
                        for (entry in jf.entries()) {
                            if (entry.isDirectory) continue
                            if (entry.notNeedJarEntries()) {
                                if (needProcessJar) jf.getInputStream(entry).use { jis ->
                                    file(entry).outputStream().use { jis.copyTo(it) }
                                } else notNeeds.add {
                                    jf.getInputStream(entry).use { jis ->
                                        file(entry).outputStream().use { jis.copyTo(it) }
                                    }
                                }
                                continue
                            }
                            needProcessJar = true
                            jf.getInputStream(entry).use { jis ->
                                val item = jis.visit(file(entry))
                                items.lock { add(item) }
                            }
                        }
                        if (needProcessJar) notNeeds.forEach { it() }
                        else {
                            jarTransformCache.beforeValue(input.name)?.also {
                                val oldDest = File(it.cachePath)
                                if (oldDest.isDirectory) oldDest.deleteRecursively()
                                oldDest.delete()
                            }
                            dest.value.delete()
                            src.copyTo(dest.value)
                        }
                        dest.value
                    }
                }
                val state = jarTransformCache.state(input.name, currentMd5)
                val dest: File = when (state) {
                    CacheStatus.ADDED,
                    CacheStatus.CHANGED -> noIncrementalProcessJar()
                    else -> {
                        val destDir = output.getContentLocation(
                            input.name,
                            input.contentTypes,
                            input.scopes,
                            Format.DIRECTORY
                        )
                        destDir.walk().forEach file@{
                            if (it.isDirectory) return@file
                            val jarEntryName = it.toRelativeString(destDir).replace("\\", "/")
                            if (jarEntryName.notNeedJarEntries()) return@file
                            val item = it.inputStream().visit(it)
                            items.lock { add(item) }
                        }
                        destDir
                    }
                }
                logger.verbose("$state file -- ${src.name} , ${dest.name}")
                jarTransformCache[input.name] = JarTransformCache.CacheInfo(currentMd5, dest.absolutePath)
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
                    val item = it.inputStream().visit(it)
                    items.lock { add(item) }
                }
            }
        }
        latch.await()
        throwable?.also { throw it }
    }

    private fun process(items: Collection<ProcessItem>) {
        modifierManager.modify()
        var throwable: Throwable? = null
        val latch = CountDownLatch(items.size)
        items.forEach { item ->
            executor.exec(latch, onError = { throwable = it }) {
                val cw = ClassWriter(item.classReader, 0)
                item.classNode.accept(cw)
                item.dest.outputStream().use { it.write(cw.toByteArray()) }
            }
        }
        latch.await()
        throwable?.also { throw it }
    }

    private fun InputStream.visit(dest: File) = use {
        val cr = ClassReader(it)
        val cv = ClassNode()
        cr.accept(cv, 0)
        ProcessItem(dest, cr, cv)
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
        val dest: File,
        val classReader: ClassReader,
        val classNode: ClassNode,
    )

}