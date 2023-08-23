package com.ysj.lib.bytecodeutil.plugin.core

import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.ModifierManager
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.modifier.logger.YLogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * 主 Task。
 *
 * @author Ysj
 * Create time: 2023/8/22
 */
abstract class BytecodeTransform : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val logger = YLogger.getLogger(javaClass)

    @TaskAction
    fun taskAction() {
        val bcuExtra = project.extensions.getByType(BytecodeUtilExtensions::class.java)
        YLogger.LOGGER_LEVEL = bcuExtra.loggerLevel
        logger.quiet("=================== transform start ===================")
        logger.quiet(">>> gradle version: ${project.gradle.gradleVersion}")
        logger.quiet(">>> loggerLevel: ${YLogger.LOGGER_LEVEL}")
        val startTime = System.currentTimeMillis()
        transform(bcuExtra)
        logger.quiet(">>> total process time: ${System.currentTimeMillis() - startTime} ms")
        logger.quiet("=================== transform end   ===================")
    }

    private fun transform(bcuExtra: BytecodeUtilExtensions) {
        val modifierManager = ModifierManager()
        val modifiers = bcuExtra.modifiers
        if (modifiers == null) {
            logger.quiet("not found modifier")
            return
        }
        val nThreads = Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(nThreads)

        // 添加所有 modifier
        for (index in modifiers.indices) {
            val clazz = modifiers[index]
            @Suppress("UNCHECKED_CAST")
            modifierManager.addModifier(project, clazz as Class<out IModifier>)
            logger.quiet("apply modifier: $clazz")
        }

        val outputFile = output.get().asFile
        logger.quiet("bcu out put >>> $outputFile")
        outputFile.outputStream().use { fos ->
            JarOutputStream(fos).use { jos ->
                var startTime = System.currentTimeMillis()

                // 扫描所有 class
                val items = scanAll(bcuExtra, modifierManager, executor, jos)
                logger.quiet(">>> bcu scan time: ${System.currentTimeMillis() - startTime} ms")

                // 处理所有字节码
                startTime = System.currentTimeMillis()
                modifierManager.modify(executor)
                logger.quiet(">>> bcu scan time: ${System.currentTimeMillis() - startTime} ms")

                process(items, executor, jos)
            }
        }
        executor.shutdownNow()
    }

    private fun scanAll(
        bcuExtra: BytecodeUtilExtensions,
        modifierManager: ModifierManager,
        executor: Executor,
        jos: JarOutputStream,
    ): LinkedList<ProcessItem> {
        val needs = LinkedList<ProcessItem>()
        val throwable = AtomicReference<Throwable>()
        // 处理 jar
        val files = allJars.get().map { it.asFile }
        var latch = CountDownLatch(files.size)
        files.forEach { file ->
            executor.exec(latch, { throwable.set(it) }) {
                JarFile(file).use { jf ->
                    jf.entries().iterator().forEach { entry ->
                        if (entry.notNeedJarEntries(bcuExtra.notNeedJar)) {
                            val bytes = jf.getInputStream(entry).use { it.readBytes() }
                            synchronized(jos) {
                                jos.putNextEntry(JarEntry(entry.name))
                                jos.write(bytes)
                                jos.closeEntry()
                            }
                        } else {
                            logger.verbose("process jar file --> ${entry.name}")
                            val item = jf
                                .getInputStream(entry)
                                .visit(entry.name, modifierManager)
                            synchronized(needs) {
                                needs.push(item)
                            }
                        }
                    }
                }
            }
        }
        latch.await()
        var error: Throwable? = throwable.get()
        if (error != null) {
            throw error
        }
        // 处理 dir
        val directories = allDirectories.get()
        latch = CountDownLatch(directories.size)
        directories.forEach { dir ->
            val rootDir = dir.asFile
            val rootUri = rootDir.toURI()
            rootDir.walk().filter { it.isFile }.forEach { file ->
                executor.exec(latch, { throwable.set(it) }) {
                    logger.verbose("process dir file --> ${file.name}")
                    val entryName = rootUri
                        .relativize(file.toURI()).path
                        .replace(File.separatorChar, '/')
                    synchronized(needs) {
                        needs.push(file.inputStream().visit(entryName, modifierManager))
                    }
                }
            }
        }
        latch.await()
        error = throwable.get()
        if (error != null) {
            throw error
        }
        return needs
    }

    private fun process(items: LinkedList<ProcessItem>, executor: Executor, jos: JarOutputStream) {
        val throwable = AtomicReference<Throwable>()
        // 处理 jar
        val latch = CountDownLatch(items.size)
        items.forEach { item ->
            executor.exec(latch, { throwable.set(it) }) {
                val cw = ClassWriter(item.classReader, 0)
                item.classNode.accept(cw)
                synchronized(jos) {
                    jos.putNextEntry(JarEntry(item.entryName))
                    jos.write(cw.toByteArray())
                    jos.closeEntry()
                }
            }
        }
        latch.await()
        val error: Throwable? = throwable.get()
        if (error != null) {
            throw error
        }
    }

    private fun InputStream.visit(entryName: String, modifierManager: ModifierManager) = use {
        val cr = ClassReader(it)
        val cv = ClassNode()
        cr.accept(cv, 0)
        modifierManager.scan(cv)
        ProcessItem(entryName, cr, cv)
    }

    private fun JarEntry.notNeedJarEntries(notNeedJar: ((entryName: String) -> Boolean)?): Boolean =
        name.endsWith(".class").not()
            || checkAndroidRFile(name)
            || name.startsWith("META-INF/")
            || name.startsWith("com/ysj/lib/bytecodeutil/")
            || notNeedJar?.invoke(name) ?: false

    private class ProcessItem(
        val entryName: String,
        val classReader: ClassReader,
        val classNode: ClassNode,
    )

}