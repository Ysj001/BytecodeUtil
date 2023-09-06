package com.example.module.demo1

import com.example.module.demo1.api.Demo1Component
import com.example.module.demo1.api.demo1.launch.Contract
import com.ysj.lib.bcu.modifier.component.di.api.ComponentImpl

/**
 * [Demo1Component] 的实现。
 *
 * @author Ysj
 * Create time: 2023/9/6
 */
@ComponentImpl
class Demo1ComponentImpl : Demo1Component {

    override fun launchDemo1Contract(): Contract {
        return Demo1LaunchContract()
    }

}