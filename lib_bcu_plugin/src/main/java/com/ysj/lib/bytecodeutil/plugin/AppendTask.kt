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

    private val logger = YLogger.getLogger(javaClass)

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val inputDir = input.get().asFile
        val outputDir = output.get().asFile
        logger.quiet(">>> BCU append input ---> $inputDir")
        logger.quiet(">>> BCU append output ---> $outputDir")
        logger.quiet(">>> BCU append isIncremental ---> ${inputChanges.isIncremental}")
        val inputUri = inputDir.toURI()
        val startTime = System.currentTimeMillis()
        // delete: "build/intermediates/project_dex_archive" can fix generate dex increment error
        val projectDexArchive = File(File(project.buildDir, "intermediates"), "project_dex_archive")
        if (!inputChanges.isIncremental) {
            projectDexArchive.deleteRecursively()
            outputDir
                .walkBottomUp()
                .filterNot { it.name.endsWith(".jar") }
                .forEach { it.delete() }
        }
        inputChanges
            .getFileChanges(input)
            .asSequence()
            .filter { it.fileType == FileType.FILE }
            .forEach { change ->
                val path = inputUri
                    .relativize(change.file.toURI())
                    .path
                    .substringBeforeLast("-crc")
                when (change.changeType) {
                    ChangeType.REMOVED -> {
                        projectDexArchive.deleteRecursively()
                        File(outputDir, path).delete()
                    }
                    ChangeType.MODIFIED -> {
                        projectDexArchive.deleteRecursively()
                        change.file.copyTo(File(outputDir, path), true)
                    }
                    else -> {
                        change.file.copyTo(File(outputDir, path))
                    }
                }
            }
        val endTime = System.currentTimeMillis()
        logger.quiet(">>> BCU append time: ${endTime - startTime} ms")
    }

}