package com.ysj.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), Runnable {

    companion object {
        private const val TAG = "MainActivity"
    }

    @LogPositionReturn
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        run()
        test2("1111")
        findViewById<View>(R.id.test).setOnClickListener {
            test3()
        }
    }

    @LogPositionCall
    private fun test2(str: String) {
        Log.i(TAG, "test2: $str")
    }

    @IntervalTrigger(500)
    private fun test3() {
        Log.i(TAG, "test3")
    }

    @LogPositionCall
    override fun run() {
        Log.i(TAG, "run")
    }

}