package com.ysj.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ysj.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)
    }

}