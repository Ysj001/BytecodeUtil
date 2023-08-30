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
class ModifierManager(override val executor: Executor) : IModifier {

    override val allClassNode = HashMap<String, ClassNode>(400)

    private val modifiers = ArrayList<IModifier>()

    private val logger = YLogger.getLogger(javaClass)

    override fun initialize(project: Project) {
        super.initialize(project)
        var startTime: Long
        for (index in modifiers.indices) {
            startTime = System.currentTimeMillis()
            val modifier = modifiers[index]
            modifier.initialize(project)
            val time = System.currentTimeMillis() - startTime
            logger.lifecycle(">>> ${modifier.javaClass.simpleName} initialize time：$time ms")
        }
    }

    override fun scan(classNode: ClassNode) {
        synchronized(allClassNode) {
            allClassNode[classNode.name] = classNode
        }
        for (index in modifiers.indices) {
            modifiers[index].scan(classNode)
        }
    }

    override fun modify() {
        val iterator = modifiers.iterator()
        var startTime: Long
        while (iterator.hasNext()) {
            startTime = System.currentTimeMillis()
            val modifier = iterator.next()
            modifier.modify()
            // 用完就移除，节约内存，避免 OOM
            iterator.remove()
            val time = System.currentTimeMillis() - startTime
            logger.lifecycle(">>> ${modifier.javaClass.simpleName} process time：$time ms")
        }
    }

    fun addModifier(modifier: Class<out IModifier>) {
        val element = modifier
            .getConstructor(Executor::class.java, Map::class.java)
            .newInstance(executor, allClassNode)
        modifiers.add(element)
    }
}