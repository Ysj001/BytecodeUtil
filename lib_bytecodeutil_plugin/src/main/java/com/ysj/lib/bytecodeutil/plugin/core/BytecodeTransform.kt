package com.ysj.lib.bytecodeutil.plugin.core

import com.android.Version
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.ysj.lib.bytecodeutil.modifier.IModifier
import com.ysj.lib.bytecodeutil.modifier.ModifierManager
import com.ysj.lib.bytecodeutil.modifier.exec
import com.ysj.lib.bytecodeutil.plugin.core.logger.YLogger
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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

    private val modifierManager by lazy(LazyThreadSafetyMode.NONE) { ModifierManager(this) }

    override fun getName(): String = PLUGIN_NAME

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental(): Boolean = false

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
            val jarItems = LinkedList<JarProcessItem>()
            // ?????????
            inputs.forEach { process(it.jarInputs, it.directoryInputs, outputProvider, dirItems, jarItems) }
            logger.lifecycle(">>> pre process time???${System.currentTimeMillis() - oldTime}")
            // ????????????
            process(dirItems, jarItems)
        }
    }

    private fun process(
        jis: Collection<JarInput>,
        dis: Collection<DirectoryInput>,
        output: TransformOutputProvider,
        dirItems: LinkedList<Pair<File, ProcessItem>>,
        jarItems: LinkedList<JarProcessItem>,
    ) {
        jis.forEach { input ->
            val src = input.file
            val dest = output.getContentLocation(
                input.name,
                input.contentTypes,
                input.scopes,
                Format.JAR
            )
            val notNeeds = LinkedList<JarEntry>()
            val needs = LinkedList<Pair<JarEntry, ProcessItem>>()
            JarFile(src).use { jf ->
                val entries = jf.entries()
                entries.forEach {
                    /*
                        ????????? transform ?????????????????? transform
                        ????????? transform ??????????????????????????? transform ?????????
                        ?????????????????????????????????????????????????????????????????? input.file.name ??????
                     */
                    if (notNeedJarEntries()) notNeeds.push(this) else {
                        logger.verbose("process jar file --> $name")
                        needs.push(this to jf.getInputStream(this).visit())
                    }
                }
            }
            if (needs.isEmpty()) FileUtils.copyFile(src, dest)
            else jarItems.push(JarProcessItem(src, dest, notNeeds, needs))
        }
        dis.forEach { input ->
            val src = input.file
            val dest = output.getContentLocation(
                input.name,
                input.contentTypes,
                input.scopes,
                Format.DIRECTORY
            )
            FileUtils.copyDirectory(src, dest)
            dest.walk().forEach test@{
                if (!it.isNeedFile()) return@test
                logger.verbose("process dir file --> ${it.name}")
                dirItems.push(it to it.inputStream().visit())
            }
        }
    }

    private fun process(dirItems: LinkedList<Pair<File, ProcessItem>>, jarItems: LinkedList<JarProcessItem>) {
        modifierManager.modify()
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        var throwable: Throwable? = null
        val dirLatch = CountDownLatch(dirItems.size)
        dirItems.forEach { pair ->
            val dest = pair.first
            val item = pair.second
            executor.exec(dirLatch, onError = { throwable = it }) {
                val cw = ClassWriter(item.classReader, 0)
                item.classNode.accept(cw)
                FileOutputStream(dest).use { it.write(cw.toByteArray()) }
            }
        }
        dirLatch.await()
        throwable?.also { executor.shutdownNow();throw it }
        val jarLatch = CountDownLatch(jarItems.size)
        jarItems.forEach { jpi ->
            executor.exec(jarLatch, onError = { throwable = it }) {
                JarFile(jpi.src).use { jf ->
                    JarOutputStream(jpi.dest.outputStream()).use { jos ->
                        jpi.needs.forEach { need ->
                            val item = need.second
                            val cw = ClassWriter(item.classReader, 0)
                            item.classNode.accept(cw)
                            jos.putNextEntry(JarEntry(need.first.name))
                            jos.write(cw.toByteArray())
                            jos.closeEntry()
                        }
                        jpi.notNeeds.forEach { notNeed ->
                            jos.putNextEntry(JarEntry(notNeed.name))
                            jos.write(jf.getInputStream(notNeed).readBytes())
                            jos.closeEntry()
                        }
                    }
                }
            }
        }
        jarLatch.await()
        executor.shutdownNow()
        throwable?.also { throw it }
    }

    private fun InputStream.visit() = use {
        val cr = ClassReader(it)
        val cv = ClassNode()
        cr.accept(cv, 0)
        modifierManager.scan(cv)
        ProcessItem(cr, cv)
    }

    private fun JarEntry.notNeedJarEntries(): Boolean =
        name.endsWith(".class").not()
                || checkAndroidRFile(name)
                || name.startsWith("META-INF/")
                || name.startsWith("com/ysj/lib/bytecodeutil/")
                || extensions.notNeedJar?.invoke(name) ?: false

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
        block(
            transformInvocation.context,
            transformInvocation.inputs,
            transformInvocation.referencedInputs,
            transformInvocation.outputProvider,
            transformInvocation.isIncremental
        )
        logger.quiet(">>> total process time: ${System.currentTimeMillis() - startTime} ms")
        logger.quiet("=================== $PLUGIN_NAME transform end   ===================")
    }

    private class JarProcessItem(
        val src: File,
        val dest: File,
        val notNeeds: LinkedList<JarEntry>,
        val needs: LinkedList<Pair<JarEntry, ProcessItem>>
    )

    private class ProcessItem(
        val classReader: ClassReader,
        val classNode: ClassNode,
    )
}