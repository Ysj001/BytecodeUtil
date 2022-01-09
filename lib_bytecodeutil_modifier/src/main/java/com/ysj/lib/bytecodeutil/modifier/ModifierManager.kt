package com.ysj.lib.bytecodeutil.modifier

import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.ysj.lib.bytecodeutil.modifier.cache.CacheStatus
import org.gradle.api.Project
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * 修改器的管理类
 *
 * @author Ysj
 * Create time: 2021/3/6
 */
class ModifierManager(override val transform: Transform, override val executor: ExecutorService) : IModifier {

    override val allClassNode = HashMap<String, ClassNode>(800)

    private val modifiers = ArrayList<IModifier>()

    override fun canIncremental(classFile: File, cacheStatus: CacheStatus): Boolean {
        return modifiers.fold(true) { canIncremental, it ->
            canIncremental && it.canIncremental(classFile, cacheStatus)
        }
    }

    override fun scan(destClassFile: File, classNode: ClassNode) {
        allClassNode[classNode.name] = classNode
        modifiers.forEach { it.scan(destClassFile, classNode) }
    }

    override fun modify() {
        val iterator = modifiers.iterator()
        while (iterator.hasNext()) {
            iterator.next().modify()
            // 用完就移除，节约内存，避免 OOM
            iterator.remove()
        }
    }

    fun addModifier(modifier: Class<out IModifier>, project: Project, transformInvocation: TransformInvocation) {
        val constructor = modifier.getConstructor(Transform::class.java, ExecutorService::class.java, Map::class.java)
        val element = constructor.newInstance(transform, executor, allClassNode)
        modifiers.add(element)
        element.initialize(project, transformInvocation)
    }
}