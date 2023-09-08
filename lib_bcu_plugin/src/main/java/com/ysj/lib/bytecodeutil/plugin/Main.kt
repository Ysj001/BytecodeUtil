package com.ysj.lib.bytecodeutil.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * 插件入口
 *
 * @author Ysj
 */
class Main : Plugin<Project> {

    override fun apply(project: Project) {
        val bucRootDir = File(project.buildDir, "bcu")
        project.extensions.create(BytecodeUtilExtensions.NAME, BytecodeUtilExtensions::class.java)
        project.extensions.configure(AndroidComponentsExtension::class.java) { appExt ->
            appExt.onVariants { variant ->
                project
                    .extensions
                    .findByType(BytecodeUtilExtensions::class.java)
                    ?: return@onVariants
                val transformTask = project.tasks.register(
                    "${variant.name}BCUTransformTask",
                    TransformTask::class.java,
                )
                transformTask.configure {
                    val notNeedDir = File(bucRootDir, "${transformTask.name}NotNeed")
                    if (!notNeedDir.isDirectory) {
                        notNeedDir.mkdirs()
                    }
                    it.notNeedOutput.set(notNeedDir)
                }
                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(transformTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        TransformTask::allJars,
                        TransformTask::allDirectories,
                        TransformTask::output
                    )
                val appendTask = project.tasks.register(
                    "${variant.name}BCUAppendTask",
                    AppendTask::class.java,
                )
                appendTask.configure {
                    it.input.set(transformTask.get().notNeedOutput)
                }
                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(appendTask)
                    .toAppend(
                        ScopedArtifact.CLASSES,
                        AppendTask::output
                    )
            }
        }
    }
}