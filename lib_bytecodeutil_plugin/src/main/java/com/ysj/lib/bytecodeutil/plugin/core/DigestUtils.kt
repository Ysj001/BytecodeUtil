package com.ysj.lib.bytecodeutil.plugin.core

import org.apache.commons.codec.digest.MessageDigestAlgorithms
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/*
 * 简化 MessageDigest 操作
 *
 * @author Ysj
 * Create time: 2021/8/15
 */

private const val STREAM_BUFFER_LENGTH = 2048

/** Used to build output as Hex */
private val DIGITS_LOWER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/** Used to build output as Hex */
private val DIGITS_UPPER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

val String.MD5_UPPER: String get() = toByteArray().MD5_UPPER

val String.MD5_LOWER: String get() = toByteArray().MD5_LOWER

val ByteArray.MD5_LOWER: String
    get() = MessageDigest.getInstance(MessageDigestAlgorithms.MD5)
        .digest(this)
        .encodeHex(DIGITS_LOWER)
        .let { String(it) }

val ByteArray.MD5_UPPER: String
    get() = MessageDigest.getInstance(MessageDigestAlgorithms.MD5)
        .digest(this)
        .encodeHex(DIGITS_UPPER)
        .let { String(it) }

val InputStream.MD5_LOWER: String
    get() = MessageDigest.getInstance(MessageDigestAlgorithms.MD5)
        .update(this)
        .digest()
        .encodeHex(DIGITS_LOWER)
        .let { String(it) }

val InputStream.MD5_UPPER: String
    get() = MessageDigest.getInstance(MessageDigestAlgorithms.MD5)
        .update(this)
        .digest()
        .encodeHex(DIGITS_UPPER)
        .let { String(it) }

@kotlin.jvm.Throws(IOException::class)
fun MessageDigest.update(data: InputStream): MessageDigest = apply {
    val buffer = ByteArray(STREAM_BUFFER_LENGTH)
    var read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
    while (read > -1) {
        update(buffer, 0, read)
        read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
    }
}

fun ByteArray.encodeHex(toDigits: CharArray): CharArray {
    val l = size
    val out = CharArray(l shl 1)
    // two characters form the hex value.
    var i = 0
    var j = 0
    while (i < l) {
        out[j++] = toDigits[0xF0 and get(i).toInt() ushr 4]
        out[j++] = toDigits[0x0F and get(i).toInt()]
        i++
    }
    return out
}