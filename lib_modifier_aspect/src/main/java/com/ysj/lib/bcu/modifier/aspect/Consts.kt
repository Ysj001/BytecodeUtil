package com.ysj.lib.bcu.modifier.aspect

import com.ysj.lib.bcu.modifier.aspect.api.Aspect
import com.ysj.lib.bcu.modifier.aspect.api.CallingPoint
import com.ysj.lib.bcu.modifier.aspect.api.JoinPoint
import com.ysj.lib.bcu.modifier.aspect.api.Pointcut
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
val joinPointDesc by lazy { joinPointType.descriptor }
val joinPointInternalName by lazy { joinPointType.internalName }

// =============== CallingPoint =================

val callingPointType by lazy { Type.getType(CallingPoint::class.java) }
val callingPointDesc by lazy { callingPointType.descriptor }
val callingPointInternalName by lazy { callingPointType.internalName }

/** 切面类实例的 FIELD 名 */
const val ASPECT_CLASS_INSTANCE = "INSTANCE"

/** 代理方法的前缀 */
const val PREFIX_PROXY_METHOD = "bcu_proxy_"