package com.ysj.lib.bcu.modifier.component.di

import com.ysj.lib.bcu.modifier.component.di.api.Component
import com.ysj.lib.bcu.modifier.component.di.api.ComponentImpl
import com.ysj.lib.bcu.modifier.component.di.api.ComponentInject
import org.objectweb.asm.Type

/*
 * 常量。
 *
 * @author Ysj
 * Create time: 2023/8/29
 */

// ============ @Component =============

val COMPONENT_TYPE = Type.getType(Component::class.java)

val COMPONENT_DES = COMPONENT_TYPE.descriptor

// ============ @ComponentImpl =============

val COMPONENT_IMPL_TYPE = Type.getType(ComponentImpl::class.java)

val COMPONENT_IMPL_DES = COMPONENT_IMPL_TYPE.descriptor

// ============ @ComponentInject =============

val COMPONENT_INJECT_TYPE = Type.getType(ComponentInject::class.java)

val COMPONENT_INJECT_DES = COMPONENT_INJECT_TYPE.descriptor

// ============ Utils =============

const val UTILS_INTERNAL_NAME = "com/ysj/lib/bcu/modifier/component/di/api/Utils"
const val UTILS_DES = "L${UTILS_INTERNAL_NAME};"