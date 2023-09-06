package com.ysj.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ysj.demo.databinding.ActivityMainBinding
import com.ysj.demo.aspect.MainActivity as AspectActivity
import com.ysj.demo.component.MainActivity as ComponentDiActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)
        vb.toAspectDemo.setOnClickListener {
            startActivity(Intent(this, AspectActivity::class.java))
        }
        vb.toComponentDiDemo.setOnClickListener {
            startActivity(Intent(this, ComponentDiActivity::class.java))
        }
    }

}