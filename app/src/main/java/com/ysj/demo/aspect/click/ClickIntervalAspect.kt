package com.ysj.demo.aspect.click

import android.util.Log
import androidx.annotation.MainThread
import com.ysj.lib.bcu.modifier.aspect.api.Aspect
import com.ysj.lib.bcu.modifier.aspect.api.CallingPoint
import com.ysj.lib.bcu.modifier.aspect.api.POSITION_CALL
import com.ysj.lib.bcu.modifier.aspect.api.Pointcut
import java.lang.reflect.Method

/**
 * 演示对被 [ClickInterval] 注解的方法进行切面处理。
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
@Aspect
object ClickIntervalAspect {

    private const val TAG = "ClickIntervalAspect"

    private val recorder = HashMap<Method, Long>()

    @Pointcut(
        target = "annotation:L.*/click/ClickInterval;",
        position = POSITION_CALL,
    )
    @MainThread
    fun onClicked(cp: CallingPoint) {
        val clickInterval = requireNotNull(cp.annotation(ClickInterval::class.java))
        val current = System.currentTimeMillis()
        val method = cp.method
        val before = recorder[method] ?: 0
        if (current - before < clickInterval.intervalMs) {
            Log.i(TAG, "禁止快速点击: ${cp.method}")
            return
        }
        recorder[method] = current
        cp.call()
    }

}