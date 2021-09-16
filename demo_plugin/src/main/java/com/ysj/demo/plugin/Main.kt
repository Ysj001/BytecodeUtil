package com.ysj.demo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 *
 * @author Ysj
 * Create time: 2021/8/25
 */
class Main : Plugin<Project> {

    override fun apply(target: Project) {
        println("不用 apply 我，可以挂在到 bytecodeutil-plugin 上")
    }

}