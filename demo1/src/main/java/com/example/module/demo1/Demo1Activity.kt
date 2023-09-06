package com.example.module.demo1

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.module.demo1.Demo1LaunchContract.Companion.KEY_NUM

/**
 *
 *
 * @author Ysj
 * Create time: 2023/9/6
 */
class Demo1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val num = intent.getIntExtra(KEY_NUM, 0)
        setContentView(AppCompatButton(this).also {
            it.gravity = Gravity.CENTER
            it.text = "点击我，会将值 +1 后返回"
            it.setOnClickListener {
                val intent = Intent()
                intent.putExtra(KEY_NUM, num + 1)
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

}