package com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect

import com.ysj.lib.bytecodeutil.api.aspect.Aspect
import com.ysj.lib.bytecodeutil.api.aspect.CallingPoint
import com.ysj.lib.bytecodeutil.api.aspect.JoinPoint
import com.ysj.lib.bytecodeutil.api.aspect.Pointcut
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode

/*
 * 定义常量
 *
 * @author Ysj
 * Create time: 2021/8/28
 */

// =============== @Aspect =================

val ANNOTATION_ASPECT: Type by lazy { Type.getType(Aspect::class.java) }
val ANNOTATION_ASPECT_DESC: String by lazy { ANNOTATION_ASPECT.descriptor }

/** 检查 [ClassNode] 上是否有 [Aspect]，有则返回 true */
val ClassNode.hasAspectAnnotation get() = invisibleAnnotations?.find { it.desc == ANNOTATION_ASPECT_DESC } != null
// =============== @Pointcut =================

val ANNOTATION_POINTCUT: Type by lazy { Type.getType(Pointcut::class.java) }
val ANNOTATION_POINTCUT_DESC: String by lazy { ANNOTATION_POINTCUT.descriptor }

// =============== JoinPoint =================

val joinPointType: Type by lazy { Type.getType(JoinPoint::class.java) }
val joinPointDesc: String by lazy { joinPointType.descriptor }
val joinPointInternalName: String by lazy { joinPointType.internalName }

// =============== CallingPoint =================

val callingPointType: Type by lazy { Type.getType(CallingPoint::class.java) }
val callingPointDesc: String by lazy { callingPointType.descriptor }
val callingPointInternalName: String by lazy { callingPointType.internalName }

/** 切面类实例的 FIELD 名 */
const val ASPECT_CLASS_INSTANCE = "INSTANCE"

/** 代理方法的前缀 */
const val PREFIX_PROXY_METHOD = "bcu_proxy_"