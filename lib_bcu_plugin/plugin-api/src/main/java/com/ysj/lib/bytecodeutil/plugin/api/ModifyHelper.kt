package com.ysj.lib.bytecodeutil.plugin.api

import org.codehaus.groovy.ast.ClassHelper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*


/*
 * 帮助修改字节码的一些方法
 *
 * @author Ysj
 * Create time: 2021/4/28
 */

/**
 * 获取注解的信息
 */
fun AnnotationNode.params() = HashMap<String, Any>().also {
    if (values.isNullOrEmpty()) return@also
    for (i in 0 until values.size step 2) {
        it[values[i] as String] = values[i + 1]
    }
}

/** 根据类型获取 Class 的指令 */
val Type.classInsnNode: AbstractInsnNode
    get() = when (sort) {
        Type.BOOLEAN -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", CLASS_TYPE_DESC)
        Type.CHAR -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", CLASS_TYPE_DESC)
        Type.BYTE -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", CLASS_TYPE_DESC)
        Type.SHORT -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", CLASS_TYPE_DESC)
        Type.INT -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", CLASS_TYPE_DESC)
        Type.FLOAT -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", CLASS_TYPE_DESC)
        Type.LONG -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", CLASS_TYPE_DESC)
        Type.DOUBLE -> FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", CLASS_TYPE_DESC)
        else -> LdcInsnNode(this)
    }

/** 方法的第一个可用 node */
val MethodNode.firstNode: AbstractInsnNode?
    get() = if (name == "<init>") {
        var result: AbstractInsnNode? = instructions.first
        while (result != null && result.opcode != Opcodes.INVOKESPECIAL) {
            result = result.next
        }
        result?.next ?: instructions.first
    } else {
        var result: AbstractInsnNode? = instructions.first
        while (result != null && result.opcode != -1) {
            result = result.next
        }
        result ?: instructions.first
    }

/**
 * 获取方法参数列表的对象集合
 *
 * ```java
 *   public void t1(String a1, int a2, long a3, Object a4) {
 *       Object[] args = new Object[]{a1, a2, a3, a4, };
 *   }
 * ```
 */
fun MethodNode.argsInsnList() = InsnList().apply {
    // 当前方法的参数列表的类型列表
    val argumentTypes = Type.getArgumentTypes(desc)
    // 局部变量索引
    var localVarIndex = if (isStatic) 0 else 1
    add(IntInsnNode(Opcodes.BIPUSH, argumentTypes.size))
    add(TypeInsnNode(
        Opcodes.ANEWARRAY,
        Type.getType(Any::class.java).internalName
    ))
    argumentTypes.forEachIndexed { i, t ->
        add(InsnNode(Opcodes.DUP))
        add(IntInsnNode(Opcodes.BIPUSH, i))
        add(VarInsnNode(t.opcodeLoad(), localVarIndex))
        if (t.sort in Type.BOOLEAN..Type.DOUBLE) {
            // primitive types must be boxed
            val wrapperType = t.wrapperType
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                wrapperType.internalName,
                "valueOf",
                "(${t.descriptor})${wrapperType.descriptor}",
                false
            ))
        }
        add(InsnNode(Opcodes.AASTORE))
        // 计算当前参数的索引
        localVarIndex += t.size
    }
    add(VarInsnNode(Opcodes.ASTORE, localVarIndex))
}

/**
 * 新建对象
 *
 * @param obj 要新建的对象
 * @param params 要新建的对象的构造器参数，key：参数类型，value：参数的具体构造
 */
fun newObject(obj: Class<*>, params: Map<Class<*>, InsnList>) = InsnList().apply {
    val typeInternalName = Type.getInternalName(obj)
    val paramTypes = params.keys.toTypedArray()
    val constructor = obj.getConstructor(*paramTypes)
    add(TypeInsnNode(Opcodes.NEW, typeInternalName))
    add(InsnNode(Opcodes.DUP))
    paramTypes.forEach { add(params[it]) }
    add(MethodInsnNode(
        Opcodes.INVOKESPECIAL,
        typeInternalName,
        "<init>",
        Type.getType(constructor).descriptor,
        false
    ))
}

/**
 * 类型强制转换
 */
fun cast(from: Type, to: Type): InsnList {
    val insnList = InsnList()
    if (from == to) return insnList
    if (from.sort < Type.BOOLEAN
        || from.sort > Type.DOUBLE
        || to.sort < Type.BOOLEAN
        || to.sort > Type.DOUBLE
    ) {
        return insnList
    }
    when {
        from === Type.DOUBLE_TYPE -> when {
            to === Type.FLOAT_TYPE -> insnList.add(InsnNode(Opcodes.D2F))
            to === Type.LONG_TYPE -> insnList.add(InsnNode(Opcodes.D2L))
            else -> {
                insnList.add(InsnNode(Opcodes.D2I))
                insnList.add(cast(Type.INT_TYPE, to))
            }
        }
        from === Type.FLOAT_TYPE -> when {
            to === Type.DOUBLE_TYPE -> insnList.add(InsnNode(Opcodes.F2D))
            to === Type.LONG_TYPE -> insnList.add(InsnNode(Opcodes.F2L))
            else -> {
                insnList.add(InsnNode(Opcodes.F2I))
                insnList.add(cast(Type.INT_TYPE, to))
            }
        }
        from === Type.LONG_TYPE -> when {
            to === Type.DOUBLE_TYPE -> insnList.add(InsnNode(Opcodes.L2D))
            to === Type.FLOAT_TYPE -> insnList.add(InsnNode(Opcodes.L2F))
            else -> {
                insnList.add(InsnNode(Opcodes.L2I))
                insnList.add(cast(Type.INT_TYPE, to))
            }
        }
        else -> when {
            to === Type.BYTE_TYPE -> insnList.add(InsnNode(Opcodes.I2B))
            to === Type.CHAR_TYPE -> insnList.add(InsnNode(Opcodes.I2C))
            to === Type.DOUBLE_TYPE -> insnList.add(InsnNode(Opcodes.I2D))
            to === Type.FLOAT_TYPE -> insnList.add(InsnNode(Opcodes.I2F))
            to === Type.LONG_TYPE -> insnList.add(InsnNode(Opcodes.I2L))
            to === Type.SHORT_TYPE -> insnList.add(InsnNode(Opcodes.I2S))
        }
    }
    return insnList
}

/**
 * 根据 Type 获取其包装类型，如果不是基本类型则返回其本身。
 */
val Type.wrapperType get() = Type.getType(Class.forName(ClassHelper.getWrapper(ClassHelper.make(className)).name))

/** 是静态方法则为 true */
val MethodInsnNode.isStatic: Boolean get() = opcode == Opcodes.INVOKESTATIC

/** 是静态方法则为 true */
val MethodNode.isStatic: Boolean get() = access and Opcodes.ACC_STATIC != 0

/** 静态 Field 则返回 true */
val FieldNode.isStatic: Boolean get() = access and Opcodes.ACC_STATIC != 0

/**
 * 获取 load 系列的 opcode
 */
fun Type.opcodeLoad() = when (sort) {
    Type.VOID,
    Type.ARRAY,
    Type.OBJECT,
    Type.METHOD -> Opcodes.ALOAD
    Type.FLOAT -> Opcodes.FLOAD
    Type.LONG -> Opcodes.LLOAD
    Type.DOUBLE -> Opcodes.DLOAD
    else -> Opcodes.ILOAD
}

val CLASS_TYPE by lazy { Type.getType(Class::class.java) }
val CLASS_TYPE_DESC by lazy { Type.getType(Class::class.java).descriptor }