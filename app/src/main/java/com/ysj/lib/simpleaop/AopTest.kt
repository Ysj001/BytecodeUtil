package com.ysj.lib.simpleaop

import android.util.Log
import com.ysj.lib.byteutil.api.aspect.Aspect
import com.ysj.lib.byteutil.api.aspect.JoinPoint
import com.ysj.lib.byteutil.api.aspect.Pointcut

/**
 *
 *
 * @author Ysj
 * Create time: 2021/3/5
 */
@Aspect
class AopTest {

    companion object {
        private const val TAG = "AopTest"
    }


    @Pointcut(
        target = "superClass:.*Activity",
        funName = "onCreate",
//        funDesc = "\\(Landroid/os/Bundle;\\)V",
//        funDesc = "\\(.*\\)V",
        funDesc = ".*.",
        position = 0,
    )
    fun log(joinPoint: JoinPoint) {
        Log.i(TAG, "捕获到: ${joinPoint.target} args:${joinPoint.args}")
    }

//    @Pointcut(
//        target = "annotation:com/ysj/lib/simpleaop/LogTargetClass",
//        funName = "onDestroy",
//        funDesc = ".*.",
//        position = -1,
//    )
//    fun log2() {
//        Log.i(TAG, "log2: ")
//    }

//    @Pointcut(
//        target = "annotation:com/ysj/lib/simpleaop/LogTargetFun",
//        funName = ".*.",
//        funDesc = ".*.",
//        position = -1,
//    )
//    fun log3(joinPoint: JoinPoint) {
//
//    }

}