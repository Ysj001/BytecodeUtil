package com.ysj.demo.component

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.module.demo1.api.Demo1Component
import com.ysj.demo.databinding.ActivityComponentDiBinding
import com.ysj.lib.bcu.modifier.component.di.api.ComponentInject
import com.example.module.demo1.api.demo1.launch.Contract as Demo1LaunchContract

/**
 * 演示 [modifier-component-di]。
 *
 * @author Ysj
 * Create time: 2023/8/31
 */
class MainActivity : AppCompatActivity() {

    @ComponentInject
    private lateinit var demo1Component: Demo1Component

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vb = ActivityComponentDiBinding.inflate(layoutInflater)
        setContentView(vb.root)
        var num = 0
        val demo1Launcher = registerForActivityResult(demo1Component.launchDemo1Contract()) {
            it ?: return@registerForActivityResult
            num = it.num
            Toast.makeText(this, "demo1 返回了:$num", Toast.LENGTH_SHORT).show()
        }
        vb.btnGoDemo1.setOnClickListener {
            demo1Launcher.launch(Demo1LaunchContract.Param(num))
        }
    }

}