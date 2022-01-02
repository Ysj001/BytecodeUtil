package com.ysj.lib.bytecodeutil.plugin.core

import com.android.Version
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.ModifierManager
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.modifier.lock
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
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

    override fun getName(): String = PLUGIN_NAME

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental(): Boolean = true

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        doTransform(transformInvocation) { context,
                                           inputs,
                                           referencedInputs,
                                           outputProvider,
                                           isIncremental ->
            extensions.modifiers?.forEach {
                modifierManager.addModifier(it as Class<out IModifier>, project, transformInvocation)
            }
            if (!isIncremental) outputProvider.deleteAll()
            val oldTime = System.currentTimeMillis()
            val dirItems = LinkedList<Pair<File, ProcessItem>>()
            // 预处理
            inputs.forEach { process(isIncremental, it.jarInputs, it.directoryInputs, outputProvider, dirItems) }
            logger.lifecycle(">>> pre process time：${System.currentTimeMillis() - oldTime}")
            // 正式处理
            process(dirItems)
        }
    }

    private fun process(
        isIncremental: Boolean,
        jis: Collection<JarInput>,
        dis: Collection<DirectoryInput>,
        output: TransformOutputProvider,
        dirItems: LinkedList<Pair<File, ProcessItem>>,
    ) {
        var throwable: Throwable? = null
        val jisLatch = CountDownLatch(jis.size)
        jis.forEach { input ->
            executor.exec(jisLatch, onError = { throwable = it }) {
                val src = input.file
                val dest = output.getContentLocation(
                    input.name,
                    input.contentTypes,
                    input.scopes,
                    Format.DIRECTORY
                )
                if (isIncremental && input.status == Status.NOTCHANGED) {
                    logger.info("no change file -- ${src.nameWithoutExtension} , ${dest.nameWithoutExtension}")
                    val startIndex = dest.absolutePath.length + 1
                    dest.walk().forEach file@{
                        if (it.isDirectory) return@file
                        val fileName = it.absolutePath.substring(startIndex).replace("\\", "/")
                        if (fileName.notNeedJarEntries()) return@file
                        logger.verbose("process cache jar file --> ${it.name}")
                        dirItems.lock { add(it to it.inputStream().visit()) }
                    }
                    return@exec
                }
                JarFile(src).use { jf ->
                    jf.entries().forEach entry@{
                        if (isDirectory) return@entry
                        val file = File(dest, name)
                        file.parentFile.also { if (!it.exists()) it.mkdirs() }
                        if (file.exists()) file.delete()
                        file.createNewFile()
                        jf.getInputStream(this).use { jis ->
                            if (notNeedJarEntries()) {
                                file.outputStream().use { fos ->
                                    jis.copyTo(fos)
                                }
                            } else {
                                logger.verbose("process jar2dir file --> ${file.name}")
                                dirItems.lock { add(file to jis.visit()) }
                            }
                        }
                    }
                }
            }
        }
        jisLatch.await()
        throwable?.also { throw it }
        val disLatch = CountDownLatch(dis.size)
        dis.forEach { input ->
            executor.exec(disLatch, onError = { throwable = it }) {
                val src = input.file
                val dest = output.getContentLocation(
                    input.name,
                    input.contentTypes,
                    input.scopes,
                    Format.DIRECTORY
                )
                if (isIncremental && input.changedFiles.isNullOrEmpty()) {
                    logger.info("no change file -- ${src.nameWithoutExtension} , ${dest.nameWithoutExtension}")
                    dest.walk().forEach file@{
                        if (!it.isNeedFile()) return@file
                        logger.verbose("process cache dir file --> ${it.name}")
                        dirItems.lock { add(it to it.inputStream().visit()) }
                    }
                    return@exec
                }
                FileUtils.copyDirectory(src, dest)
                dest.walk().forEach file@{
                    if (!it.isNeedFile()) return@file
                    logger.verbose("process dir file --> ${it.name}")
                    dirItems.lock { add(it to it.inputStream().visit()) }
                }
            }
        }
        disLatch.await()
        throwable?.also { throw it }
    }

    private fun process(dirItems: LinkedList<Pair<File, ProcessItem>>) {
        modifierManager.modify()
        var throwable: Throwable? = null
        val dirLatch = CountDownLatch(dirItems.size)
        dirItems.forEach { pair ->
            val (dest, item) = pair
            executor.exec(dirLatch, onError = { throwable = it }) {
                val cw = ClassWriter(item.classReader, 0)
                item.classNode.accept(cw)
                dest.outputStream().use { it.write(cw.toByteArray()) }
            }
        }
        dirLatch.await()
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

    private fun File.isNeedFile(): Boolean = isFile && extension == "class"

    private inline fun <T> Enumeration<T>.forEach(block: T.() -> Unit) {
        while (hasMoreElements()) nextElement().block()
    }

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
}