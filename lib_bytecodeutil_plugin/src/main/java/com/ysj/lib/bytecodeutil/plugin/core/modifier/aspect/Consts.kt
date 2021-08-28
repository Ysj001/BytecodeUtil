package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect

import com.ysj.lib.bytecodeutil.api.aspect.Aspect
import com.ysj.lib.bytecodeutil.api.aspect.CallingPoint
import com.ysj.lib.bytecodeutil.api.aspect.JoinPoint
import com.ysj.lib.bytecodeutil.api.aspect.Pointcut
import org.objectweb.asm.Type

/*
 * 定义常量
 *
 * @author Ysj
 * Create time: 2021/8/28
 */

// =============== @Aspect =================

val ANNOTATION_ASPECT by lazy { Type.getType(Aspect::class.java) }
val ANNOTATION_ASPECT_DESC by lazy { ANNOTATION_ASPECT.descriptor }

// =============== @Pointcut =================

val ANNOTATION_POINTCUT by lazy { Type.getType(Pointcut::class.java) }
val ANNOTATION_POINTCUT_DESC by lazy { ANNOTATION_POINTCUT.descriptor }

// =============== JoinPoint =================

val joinPointType by lazy { Type.getType(JoinPoint::class.java) }

// =============== CallingPoint =================

val callingPointType by lazy { Type.getType(CallingPoint::class.java) }