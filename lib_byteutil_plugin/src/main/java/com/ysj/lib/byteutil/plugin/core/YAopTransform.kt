package com.ysj.lib.byteutil.plugin.core

import com.android.Version
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.ysj.lib.byteutil.plugin.core.logger.YLogger
import com.ysj.lib.byteutil.plugin.core.modifier.ModifierManager
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 *
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
class YAopTransform(private val project: Project) : Transform() {

    companion object {
        const val PLUGIN_NAME = "BytecodeUtilPlugin"
    }

    private val logger = YLogger.getLogger(javaClass)

    /** 不需要的 [JarEntry] 用于提升处理速度 */
    private val notNeedJarEntriesCache by lazy(LazyThreadSafetyMode.NONE) { HashSet<String>() }

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
            if (!isIncremental) outputProvider.deleteAll()
            // 预处理，用于提前获取信息
            inputs.forEach { prePrecess(it.jarInputs, it.directoryInputs) }
            inputs.forEach {
                // 处理 jar
                it.jarInputs.forEach { input -> processJar(input, outputProvider) }
                // 处理源码
                it.directoryInputs.forEach { input -> processDir(input, outputProvider) }
            }
        }
    }

    private fun processJar(input: JarInput, output: TransformOutputProvider) {
        val src = input.file
        val dest = output.getContentLocation(
            input.name,
            input.contentTypes,
            input.scopes,
            Format.JAR
        )
        if (notNeedJarEntriesCache.contains(src.name)) {
            FileUtils.copyFile(src, dest)
            return
        }
        JarFile(src).use { jf ->
            JarOutputStream(dest.outputStream()).use { jos ->
                jf.entries().toList()
                    .filter { true }
                    .forEach {
                        jf.getInputStream(it).use { ips ->
                            val zipEntry = ZipEntry(it.name)
                            if (it.name.endsWith(".class")) {
                                //                    logger.quiet("process jar element --> ${element.name}")
                                val cr = ClassReader(ips)
                                val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
                                val cv = ClassNode()
                                cr.accept(cv, 0)
                                modifierManager.modify(cv)
                                cv.accept(cw)
                                jos.putNextEntry(zipEntry)
                                jos.write(cw.toByteArray())
                            } else {
                                jos.putNextEntry(zipEntry)
                                jos.write(ips.readBytes())
                            }
                            jos.closeEntry()
                        }
                    }
            }
        }
    }

    private fun processDir(input: DirectoryInput, output: TransformOutputProvider) {
        val src = input.file
        val dest = output.getContentLocation(
            input.name,
            input.contentTypes,
            input.scopes,
            Format.DIRECTORY
        )
        FileUtils.copyDirectory(src, dest)
        dest.walk()
            .filter { isNeedFile(it) }
            .forEach {
//                logger.quiet("process file --> ${it.name}")
                it.inputStream().use { fis ->
                    val cr = ClassReader(fis)
                    val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
                    val cv = ClassNode()
                    cr.accept(cv, 0)
                    modifierManager.modify(cv)
                    cv.accept(cw)
                    FileOutputStream(it).use { fos ->
                        fos.write(cw.toByteArray())
                    }
                }
            }
    }

    private fun prePrecess(jis: Collection<JarInput>, dis: Collection<DirectoryInput>) {
        jis.forEach { input ->
            JarFile(input.file).use { jf ->
                val entries = jf.entries().toList()
                /*
                    由于该 transform 可能后于其他 transform
                    此时该 transform 的输入源会变为其他 transform 的输出
                    此时输入源的名称会发生变化，因此不能简单通过 input.file.name 过滤
                 */
                if (notNeedJarEntries(entries)) {
                    notNeedJarEntriesCache.add(input.file.name)
                    return@forEach
                }
                entries.toList()
                    .filter { it.name.endsWith(".class") }
                    .forEach {
                        logger.verbose("need process in jar --> ${it.name}")
                        preVisitor(jf.getInputStream(it))
                    }
            }
        }
        dis.forEach { input ->
            input.file.walk()
                .filter { isNeedFile(it) }
                .forEach test@{
                    logger.verbose("need process in dir --> ${it.name}")
                    preVisitor(it.inputStream())
                }
        }
    }

    private fun preVisitor(inputStream: InputStream) {
        inputStream.use {
            val cr = ClassReader(it)
            val cv = ClassNode()
            cr.accept(cv, 0)
            modifierManager.scan(cv)
        }
    }

    private fun notNeedJarEntries(entries: List<JarEntry>): Boolean {
        entries.forEach {
            if (it.name.startsWith("kotlin/")
                || it.name.startsWith("kotlinx/")
                || it.name.startsWith("javax/")
                || it.name.startsWith("org/intellij/")
                || it.name.startsWith("org/jetbrains/")
                || it.name.startsWith("org/junit/")
                || it.name.startsWith("org/hamcrest/")
                || it.name.startsWith("com/squareup/")
                || it.name.startsWith("androidx/")
//                || it.name.startsWith("android/")
                || it.name.startsWith("com/google/android/")
                || it.name.startsWith("com/ysj/lib/aop/annotation/")
            ) return true
        }
        return false
    }

    private fun isNeedFile(file: File): Boolean =
        file.isFile && file.extension == "class" && file.name !in listOf("BuildConfig.class")

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
        logger.quiet(">>> process time: ${System.currentTimeMillis() - startTime} ms")
        logger.quiet("=================== $PLUGIN_NAME transform end   ===================")
    }

}