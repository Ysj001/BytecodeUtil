package com.ysj.lib.bcu.modifier.aspect.api

/** 空对象 */
internal val EMPTY_OBJ = Any()

/** 空数组 */
internal val EMPTY_ARRAY: Array<Any?> = emptyArray()

/**
 * Interface for managing a pool of objects.
 *
 * @param <T> The pooled type.
 */
internal interface Pool<T> {
    /**
     * @return An instance from the pool if such, null otherwise.
     */
    fun obtain(): T?

    /**
     * Recycle an instance to the pool.
     *
     * @param instance The instance to release.
     * @return Whether the instance was put in the pool.
     * @throws IllegalStateException If the instance is already in the pool.
     */
    fun recycle(instance: T): Boolean
}

/**
 * Simple (non-synchronized) pool of objects.
 */
internal open class SimplePool<T>(maxPoolSize: Int) : Pool<T> {
    private val mPool: Array<Any?>
    private var mPoolSize = 0

    init {
        require(maxPoolSize > 0) { "The max pool size must be > 0" }
        mPool = arrayOfNulls(maxPoolSize)
    }

    override fun obtain(): T? {
        if (mPoolSize > 0) {
            val lastPooledIndex = mPoolSize - 1
            val instance: T? = mPool[lastPooledIndex] as T?
            mPool[lastPooledIndex] = null
            mPoolSize--
            return instance
        }
        return null
    }

    override fun recycle(instance: T): Boolean {
        if (isInPool(instance)) return true
        if (mPoolSize < mPool.size) {
            mPool[mPoolSize] = instance
            mPoolSize++
            return true
        }
        return false
    }

    private fun isInPool(instance: T): Boolean {
        for (i in 0 until mPoolSize) {
            if (mPool[i] !== instance) continue
            return true
        }
        return false
    }

}

/**
 * Synchronized pool of objects.
 */
internal class SynchronizedPool<T>(maxPoolSize: Int) : SimplePool<T>(maxPoolSize) {
    @Synchronized
    override fun obtain(): T? = super.obtain()

    @Synchronized
    override fun recycle(instance: T): Boolean = super.recycle(instance)
}