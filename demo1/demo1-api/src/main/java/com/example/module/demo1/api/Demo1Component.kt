package com.example.module.demo1.api

import com.example.module.demo1.api.demo1.launch.Contract as Demo1LaunchContract
import com.ysj.lib.bcu.modifier.component.di.api.Component

/**
 * 定义 demo1 组件对外接口。
 *
 * @author Ysj
 * Create time: 2023/9/6
 */
@Component
interface Demo1Component {

    fun launchDemo1Contract(): Demo1LaunchContract

}