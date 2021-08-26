package com.ysj.lib.bytecodeutil.plugin

import com.android.build.gradle.AppExtension
import com.ysj.lib.bytecodeutil.plugin.core.BytecodeTransform
import com.ysj.lib.bytecodeutil.plugin.core.BytecodeUtilExtensions
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
        project.extensions.getByType(AppExtension::class.java).also { appExt ->
            val transform = BytecodeTransform(project)
            appExt.registerTransform(transform)
            project.afterEvaluate {
                transform.extensions = it.extensions.getByType(BytecodeUtilExtensions::class.java)
                transform.extensions.modifiers?.forEach {m->
                    project.logger.lifecycle("modifiers --> $m , ${m.methods.size}")
                }
            }
        }
    }
}