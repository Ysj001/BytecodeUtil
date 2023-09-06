package com.example.module.demo1

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.module.demo1.api.demo1.launch.Contract

/**
 * 用于启动 [Demo1Activity] 的协议。
 *
 * @author Ysj
 * Create time: 2023/9/6
 */
class Demo1LaunchContract : Contract() {

    companion object {
        const val KEY_NUM = "KEY_NUM"
    }

    override fun createIntent(context: Context, input: Param): Intent {
        return Intent(context, Demo1Activity::class.java)
            .putExtra(KEY_NUM, input.num)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? {
        return when {
            resultCode != Activity.RESULT_OK -> null
            intent == null -> null
            else -> Result(intent.getIntExtra(KEY_NUM, 0))
        }
    }

}