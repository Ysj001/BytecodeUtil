package com.ysj.lib.bytecodeutil.plugin

import com.android.build.gradle.AppExtension
import com.ysj.lib.bytecodeutil.plugin.core.BytecodeTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 插件入口
 *
 * @author Ysj
 */
class Main : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.getByType(AppExtension::class.java).also { appExt ->
            appExt.registerTransform(BytecodeTransform(project))
        }
    }
}