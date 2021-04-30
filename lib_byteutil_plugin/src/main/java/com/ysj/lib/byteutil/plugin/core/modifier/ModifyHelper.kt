package com.ysj.lib.byteutil.plugin.core.modifier

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

/**
 * 获取方法参数列表的对象集合
 *
 * ```java
 *   public void t1(String a1, int a2, long a3, Object a4) {
 *       Object[] args = new Object[]{a1, a2, a3, a4, };
 *   }
 * ```
 *
 * @param indexBlock 方法的参数列表最后一个变量的索引
 */
inline fun MethodNode.argsInsnList(indexBlock: (Int) -> Unit) = InsnList().apply {
    // 当前方法的参数列表的类型列表
    val argumentTypes = Type.getArgumentTypes(desc)
    // 局部变量索引
    var localVarIndex = 1
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
            val wrapperType = Type.getType(Class.forName(
                ClassHelper.getWrapper(ClassHelper.make(t.className)).name
            ))
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
    indexBlock(localVarIndex - 1)
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

/**
 * 若是 load 系列的 opcode 则返回 true
 */
fun Int.opcodeIsLoad() = this in Opcodes.ILOAD..Opcodes.SALOAD

/**
 * 若是 store 相关的 opcode 则返回 true
 */
fun Int.opcodeIsStore() = this in Opcodes.ISTORE..Opcodes.SASTORE