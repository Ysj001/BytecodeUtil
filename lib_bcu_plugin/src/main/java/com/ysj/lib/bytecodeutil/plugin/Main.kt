package com.ysj.lib.bytecodeutil.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 插件入口
 *
 * @author Ysj
 */
class Main : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create(BytecodeUtilExtensions.NAME, BytecodeUtilExtensions::class.java)
        project.extensions.configure(ApplicationAndroidComponentsExtension::class.java) { appExt ->
            appExt.onVariants { variant ->
                project
                    .extensions
                    .findByType(BytecodeUtilExtensions::class.java)
                    ?: return@onVariants
                val task = project.tasks.register(
                    "${variant.name}BCUTask",
                    BytecodeTransform::class.java,
                )
                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(task)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        BytecodeTransform::allJars,
                        BytecodeTransform::allDirectories,
                        BytecodeTransform::output
                    )
            }
        }
    }
}