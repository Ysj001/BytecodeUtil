package com.ysj.lib.bytecodeutil.modifier.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.Reader

/*
 * 便捷的 Json 操作
 *
 * @author Ysj
 * Create time: 2022/1/2 23:35
 */

val GSON = GsonBuilder()
    .addSerializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipClass(clazz: Class<*>?) = false
        override fun shouldSkipField(fa: FieldAttributes?) =
            fa?.getAnnotation(Expose::class.java)?.run { !serialize } ?: false
    })
    .addDeserializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipClass(clazz: Class<*>?) = false
        override fun shouldSkipField(fa: FieldAttributes?) =
            fa?.getAnnotation(Expose::class.java)?.run { !deserialize } ?: false
    })
    .setPrettyPrinting()
    .create()

fun Any.toJson() = GSON.toJson(this)

fun Any.toJson(file: File) = file.bufferedWriter().use { GSON.toJson(this, it) }

inline fun <reified T> File.fromJson(): T = bufferedReader().use { it.fromJson() }
inline fun <reified T> Reader.fromJson(): T = GSON.fromJson(this, object : TypeToken<T>() {}.type)