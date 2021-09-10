package com.ysj.lib.bytecodeutil.plugin.core

import java.security.MessageDigest

/*
 * 生成 MD5
 *
 * @author Ysj
 * Create time: 2021/8/15
 */

val String.MD5: String get() = toByteArray().MD5

val ByteArray.MD5: String
    get() = MessageDigest.getInstance("md5").digest(this).let { array ->
        val sb = StringBuilder()
        array.forEach { sb.append(String.format("%02x", it)); }
        sb.toString()
    }