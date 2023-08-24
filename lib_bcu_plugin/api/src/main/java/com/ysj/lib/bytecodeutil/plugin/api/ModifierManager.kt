package com.ysj.lib.bytecodeutil.plugin.api

import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.Executor

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager : IModifier {

    override val allClassNode = HashMap<String, ClassNode>(400)

    private val modifiers = ArrayList<IModifier>()

    private val logger = YLogger.getLogger(javaClass)

    @Synchronized
    override fun scan(classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(classNode) }
    }

    override fun modify(executor: Executor) {
        val iterator = modifiers.iterator()
        var startTime: Long
        while (iterator.hasNext()) {
            startTime = System.currentTimeMillis()
            val modifier = iterator.next()
            modifier.modify(executor)
            // 用完就移除，节约内存，避免 OOM
            iterator.remove()
            val time = System.currentTimeMillis() - startTime
            logger.lifecycle(">>> ${modifier.javaClass.simpleName} process time：$time ms")
        }
    }

    fun addModifier(project: Project, modifier: Class<out IModifier>) {
        val constructor = modifier.getConstructor(Map::class.java)
        val element = constructor.newInstance(allClassNode)
        modifiers.add(element)
        element.initialize(project)
    }
}