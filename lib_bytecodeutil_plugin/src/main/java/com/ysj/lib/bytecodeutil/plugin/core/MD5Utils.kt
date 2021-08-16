package com.ysj.lib.bytecodeutil.plugin.core

import java.math.BigInteger
import java.security.MessageDigest

/*
 * 生成 MD5
 *
 * @author Ysj
 * Create time: 2021/8/15
 */

val String.MD5: String get() = toByteArray().MD5

val ByteArray.MD5: String get() = BigInteger(MessageDigest.getInstance("md5").digest(this)).toString(16)