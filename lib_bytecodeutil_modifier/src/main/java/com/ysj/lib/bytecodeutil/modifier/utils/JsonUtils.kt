package com.ysj.lib.bytecodeutil.modifier.utils

import com.google.gson.GsonBuilder
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
    .setPrettyPrinting()
    .create()

fun Any.toJson() = GSON.toJson(this)

fun Any.toJson(file: File) = file.bufferedWriter().use { GSON.toJson(this, it) }

inline fun <reified T> File.fromJson(): MutableMap<String, T> = bufferedReader().use { it.fromJson() }

inline fun <reified T> Reader.fromJson(): MutableMap<String, T> =
    GSON.fromJson(this, TypeToken.getParameterized(Map::class.java, String::class.java, T::class.java).type)
