package com.example.module.demo1.api.demo1.launch

import androidx.activity.result.contract.ActivityResultContract

/**
 * 用于启动 Demo1 页面的协议。
 *
 * @author Ysj
 * Create time: 2023/9/6
 */
abstract class Contract : ActivityResultContract<Contract.Param, Contract.Result?>() {

    class Param(
        val num: Int,
    )

    class Result(
        val num: Int,
    )

}