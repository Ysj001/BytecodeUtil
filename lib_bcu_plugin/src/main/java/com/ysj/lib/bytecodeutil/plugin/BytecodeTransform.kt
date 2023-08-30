package com.ysj.lib.bytecodeutil.plugin

import com.ysj.lib.bytecodeutil.plugin.api.IModifier
import com.ysj.lib.bytecodeutil.plugin.api.ModifierManager
import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.math.max
import kotlin.system.measureTimeMillis

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
        val modifiers = bcuExtra.modifiers
        if (modifiers == null) {
            logger.quiet("bcu not found modifier")
            return
        }
        logger.quiet("=================== transform start ===================")
        logger.quiet(">>> gradle version: ${project.gradle.gradleVersion}")
        logger.quiet(">>> loggerLevel: ${YLogger.LOGGER_LEVEL}")
        // 添加所有 modifier
        val useTime = measureTimeMillis {
            val nThreads = Runtime.getRuntime().availableProcessors()
            val executor = ThreadPoolExecutor(
                2,
                max(2, nThreads),
                60,
                TimeUnit.SECONDS,
                LinkedBlockingQueue()
            )
            try {
                val modifierManager = ModifierManager(executor)
                for (index in modifiers.indices) {
                    val clazz = modifiers[index]
                    @Suppress("UNCHECKED_CAST")
                    modifierManager.addModifier(clazz as Class<out IModifier>)
                    logger.quiet(">>> apply modifier: ${clazz.name}")
                }
                modifierManager.initialize(project)
                transform(Transform(bcuExtra, modifierManager, executor))
            } finally {
                executor.shutdownNow()
            }
        }
        logger.quiet(">>> total process time: $useTime ms")
        logger.quiet("=================== transform end   ===================")
    }

    private fun transform(transform: Transform) {
        val outputFile = output.get().asFile
        outputFile.outputStream().use { fos ->
            JarOutputStream(fos).use { jos ->
                var startTime = System.currentTimeMillis()

                // 扫描所有 class
                val items = scanAll(transform, jos)
                logger.quiet(">>> bcu scan time: ${System.currentTimeMillis() - startTime} ms")

                // 处理所有字节码
                startTime = System.currentTimeMillis()
                transform.modifierManager.modify()
                logger.quiet(">>> bcu modify time: ${System.currentTimeMillis() - startTime} ms")

                // 把所有字节码写到 output jar
                startTime = System.currentTimeMillis()
                process(items, transform.executor, jos)
                logger.quiet(">>> bcu output time: ${System.currentTimeMillis() - startTime} ms")
            }
        }
        logger.quiet(">>> bcu out put $outputFile")
    }

    private fun scanAll(transform: Transform, jos: JarOutputStream): LinkedList<ProcessItem> {
        val needs = LinkedList<ProcessItem>()
        val jars = allJars.get()
        val dirs = allDirectories.get()
        val latch = CountDownLatch(jars.size + dirs.size)
        val throwable = AtomicReference<Throwable>()
        // 处理 jar
        jars.forEach { rf ->
            val file = rf.asFile
            transform.executor.exec(latch, throwable) {
                JarFile(file).use { jf ->
                    jf.entries().iterator().forEach entry@{ entry ->
                        if (entry.isDirectory || entry.name.startsWith("META-INF")) {
                            return@entry
                        }
                        if (entry.name.notNeedEntries(transform.extensions.notNeed)) {
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
                                .use { it.visit(entry.name, transform.modifierManager) }
                            synchronized(needs) {
                                needs.push(item)
                            }
                        }
                    }
                }
            }
        }
        // 处理 dir
        dirs.forEach { dir ->
            val rootDir = dir.asFile
            val rootUri = rootDir.toURI()
            transform.executor.exec(latch, throwable) {
                rootDir.walk()
                    .filter { it.name != "META-INF" }
                    .filter { it.isFile }
                    .forEach { file ->
                        val entryName = rootUri
                            .relativize(file.toURI()).path
                            .replace(File.separatorChar, '/')
                        if (entryName.notNeedEntries(transform.extensions.notNeed)) {
                            val entry = JarEntry(entryName)
                            val bytes = file.readBytes()
                            synchronized(jos) {
                                jos.putNextEntry(entry)
                                jos.write(bytes)
                                jos.closeEntry()
                            }
                        } else {
                            logger.verbose("process dir file --> $entryName")
                            val item = file
                                .inputStream()
                                .use { it.visit(entryName, transform.modifierManager) }
                            synchronized(needs) {
                                needs.push(item)
                            }
                        }
                    }
            }
        }
        latch.await()
        throwable.get()?.also { throw it }
        return needs
    }

    private fun process(items: LinkedList<ProcessItem>, executor: Executor, jos: JarOutputStream) {
        val latch = CountDownLatch(items.size)
        val throwable = AtomicReference<Throwable>()
        while (items.isNotEmpty()) {
            val item = items.pop()
            executor.exec(latch, throwable) {
                val cw = ClassWriter(item.classReader, ClassWriter.COMPUTE_MAXS)
                item.classNode.accept(cw)
                val bytes = cw.toByteArray()
                val entry = JarEntry(item.entryName)
                synchronized(jos) {
                    jos.putNextEntry(entry)
                    jos.write(bytes)
                    jos.closeEntry()
                }
            }
        }
        latch.await()
        throwable.get()?.also { throw it }
    }

    private fun InputStream.visit(entryName: String, modifierManager: ModifierManager) = use {
        val cr = ClassReader(it)
        val cv = ClassNode()
        cr.accept(cv, 0)
        modifierManager.scan(cv)
        ProcessItem(entryName, cr, cv)
    }

    private fun String.notNeedEntries(notNeed: ((entryName: String) -> Boolean)): Boolean =
        endsWith(".class").not()
            || checkAndroidRFile(this)
            || startsWith("com/ysj/lib/bytecodeutil/")
            || notNeed.invoke(this)

    private fun checkAndroidRFile(fileName: String) =
        fileName.endsWith("R.class")
            || fileName.endsWith("R\$raw.class")
            || fileName.endsWith("R\$styleable.class")
            || fileName.endsWith("R\$layout.class")
            || fileName.endsWith("R\$xml.class")
            || fileName.endsWith("R\$attr.class")
            || fileName.endsWith("R\$color.class")
            || fileName.endsWith("R\$bool.class")
            || fileName.endsWith("R\$mipmap.class")
            || fileName.endsWith("R\$dimen.class")
            || fileName.endsWith("R\$interpolator.class")
            || fileName.endsWith("R\$plurals.class")
            || fileName.endsWith("R\$style.class")
            || fileName.endsWith("R\$integer.class")
            || fileName.endsWith("R\$id.class")
            || fileName.endsWith("R\$animator.class")
            || fileName.endsWith("R\$string.class")
            || fileName.endsWith("R\$drawable.class")
            || fileName.endsWith("R\$anim.class")

    private fun Executor.exec(latch: CountDownLatch, t: AtomicReference<Throwable>, block: () -> Unit) {
        t.get()?.also { throw it }
        execute {
            if (latch.count == 0L) {
                return@execute
            }
            try {
                block()
                latch.countDown()
            } catch (e: Throwable) {
                t.set(e)
                while (latch.count > 0) {
                    latch.countDown()
                }
            }
        }
    }

    private class ProcessItem(
        val entryName: String,
        val classReader: ClassReader,
        val classNode: ClassNode,
    )

    private class Transform(
        val extensions: BytecodeUtilExtensions,
        val modifierManager: ModifierManager,
        val executor: Executor,
    )

}