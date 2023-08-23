package com.ysj.lib.bytecodeutil.modifier

import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager : IModifier {

    override val allClassNode = ConcurrentHashMap<String, ClassNode>()

    private val modifiers = ArrayList<IModifier>()

    override fun scan(classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(classNode) }
    }

    override fun modify(executor: Executor) {
        val iterator = modifiers.iterator()
        while (iterator.hasNext()) {
            iterator.next().modify(executor)
            // 用完就移除，节约内存，避免 OOM
            iterator.remove()
        }
    }

    fun addModifier(project: Project, modifier: Class<out IModifier>) {
        val constructor = modifier.getConstructor(Map::class.java)
        val element = constructor.newInstance(allClassNode)
        modifiers.add(element)
        element.initialize(project)
    }
}