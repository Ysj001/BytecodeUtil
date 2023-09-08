package com.ysj.lib.bytecodeutil.plugin

import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.jar.JarFile
import javax.inject.Inject

/**
 * 用于处理 [TransformTask.notNeedOutput]。
 *
 * @author Ysj
 * Create time：2023/9/7
 */
abstract class AppendTask : DefaultTask() {

    @get:Incremental
    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Inject
    abstract val worker: WorkerExecutor

    private val logger = YLogger.getLogger(javaClass)

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val inputDir = input.get().asFile
        val outputDir = output.get().asFile
        logger.quiet(">>> BCU append input ---> $inputDir")
        logger.quiet(">>> BCU append output ---> $outputDir")
        logger.quiet(">>> BCU append isIncremental ---> ${inputChanges.isIncremental}")
        val startTime = System.currentTimeMillis()
        // delete: "build/intermediates/project_dex_archive" can fix generate dex increment error
        val projectDexArchive = File(File(project.buildDir, "intermediates"), "project_dex_archive")
        if (!inputChanges.isIncremental) {
            projectDexArchive.deleteRecursively()
            outputDir.deleteRecursively()
        }
        val workQueue = worker.noIsolation()
        inputChanges
            .getFileChanges(input)
            .asSequence()
            .filter { it.fileType == FileType.FILE }
            .filterNot {
                when (it.changeType) {
                    ChangeType.REMOVED -> {
                        projectDexArchive.deleteRecursively()
                        File(outputDir, it.file.nameWithoutExtension).deleteRecursively()
                        true
                    }
                    ChangeType.MODIFIED -> {
                        projectDexArchive.deleteRecursively()
                        File(outputDir, it.file.nameWithoutExtension).deleteRecursively()
                        false
                    }
                    else -> false
                }
            }
            .forEach { change ->
                workQueue.submit(Action::class.java) {
                    it.file.set(change.file)
                    it.outputDir.set(outputDir)
                }
            }
        workQueue.await()
        val endTime = System.currentTimeMillis()
        logger.quiet(">>> BCU append time: ${endTime - startTime} ms")
    }

    abstract class Action : WorkAction<Action.Param> {

        override fun execute() {
            val file = parameters.file.get()
            val outputDir = parameters.outputDir.get()
            val jarOutputDir = File(outputDir, file.nameWithoutExtension)
            JarFile(file).use { jf ->
                jf.entries().iterator().forEach entry@{ entry ->
                    if (entry.isDirectory) {
                        return@entry
                    }
                    val entryFile = File(jarOutputDir, entry.name)
                    val parentFile = entryFile.parentFile
                    if (!parentFile.exists()) {
                        parentFile.mkdirs()
                    }
                    if (entryFile.exists()) {
                        entryFile.delete()
                    }
                    entryFile.createNewFile()
                    entryFile.outputStream().use { fos ->
                        jf.getInputStream(entry).use { it.copyTo(fos) }
                    }
                }
            }
        }

        abstract class Param : WorkParameters {
            abstract val file: Property<File>
            abstract val outputDir: Property<File>
        }
    }

}