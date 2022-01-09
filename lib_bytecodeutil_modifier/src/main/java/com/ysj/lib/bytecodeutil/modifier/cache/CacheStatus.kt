package com.ysj.lib.bytecodeutil.modifier.cache

enum class CacheStatus {
    /**
     * The file was not changed since the last build.
     */
    NOT_CHANGED,

    /**
     * The file was added since the last build.
     */
    ADDED,

    /**
     * The file was modified since the last build.
     */
    CHANGED,

    /**
     * The file was removed since the last build.
     */
    REMOVED,
}