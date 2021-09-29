package com.ysj.lib.simpleaop

import android.util.Log
import com.ysj.lib.bytecodeutil.api.aspect.*

/**
 * 演示切面和切入点的定义
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
@Aspect
object AopTest {

    const val TAG = "AopTest"

    @Pointcut(
        target = "class:.*.",
        position = POSITION_START,
    )
    fun log2(jp: JoinPoint) {
        Log.i(TAG, "log2 --> jp-target:${jp.target}，jp-args:${jp.args.contentToString()}")
    }

    @Pointcut(
        target = "annotation:L.*LogPositionReturn;",
        position = POSITION_RETURN
    )
    fun log3(jp: JoinPoint) {
        Log.i(TAG, "log3 --> jp-target:${jp.target}，jp-args:${jp.args.contentToString()}")
    }

    var count = 1

    @Pointcut(
        target = "annotation:Lcom/ysj/lib/simpleaop/LogPositionCall;",
        position = POSITION_CALL,
    )
    fun log4(cp: CallingPoint) {
        Log.i(TAG, "log4: ${count++}")
        cp.call()
    }

    var oldTriggerTime = 0L

    @Pointcut(
        target = "annotation:L.*IntervalTrigger;",
        position = POSITION_CALL,
    )
    fun log5(jp: JoinPoint, cp: CallingPoint) {
        Log.i(TAG, "log3 --> jp-target:${jp.target}，jp-args:${jp.args.contentToString()}")
        val trigger = cp.annotation(IntervalTrigger::class.java) ?: return
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - oldTriggerTime < trigger.intervalMS) {
            Log.i(TAG, "log5: 禁止触发")
            return
        }
        oldTriggerTime = currentTimeMillis
        Log.i(TAG, "log5: 成功触发")
        cp.call()
    }
}