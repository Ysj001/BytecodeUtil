package com.ysj.demo.aspect

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ysj.demo.aspect.click.ClickInterval
import com.ysj.demo.databinding.ActivityAspectBinding

/**
 * 演示 [modifier-aspect]。
 *
 * @author Ysj
 * Create time: 2023/8/31
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vb = ActivityAspectBinding.inflate(layoutInflater)
        setContentView(vb.root)
        vb.btnClickInterval.setOnClickListener {
            testClickInterval()
        }
    }

    @ClickInterval
    private fun testClickInterval() {
        Log.i(TAG, "testClickInterval")
    }

}