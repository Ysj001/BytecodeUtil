package com.ysj.lib.bytecodeutil.plugin.api

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/*
 * 简化 MessageDigest 操作
 * Algorithms：https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
 *
 * @author Ysj
 * Create time: 2021/8/15
 */

/**
 * The MD5 message digest algorithm defined in [RFC 1321](https://tools.ietf.org/html/rfc1321).
 */
private const val MD5 = "MD5"

/**
 * Hash algorithms defined in the [FIPS PUB 180-4](https://csrc.nist.gov/publications/detail/fips/180/4/final).
 */
private const val SHA256 = "SHA-256"

private const val STREAM_BUFFER_LENGTH = 2048

/** Used to build output as Hex */
val DIGITS_LOWER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/** Used to build output as Hex */
val DIGITS_UPPER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

// ======================== MD5
fun String.md5(toDigits: CharArray = DIGITS_LOWER): String = toByteArray().md5(toDigits)
fun File.md5(toDigits: CharArray = DIGITS_LOWER): String = inputStream().use { it.md5(toDigits) }
fun ByteArray.md5(toDigits: CharArray = DIGITS_LOWER): String = digest(MD5, toDigits)
fun InputStream.md5(toDigits: CharArray = DIGITS_LOWER): String = digest(MD5, toDigits)

// ======================== SHA-256
fun String.sha256(toDigits: CharArray = DIGITS_LOWER): String = toByteArray().sha256(toDigits)
fun File.sha256(toDigits: CharArray = DIGITS_LOWER): String = inputStream().use { it.sha256(toDigits) }
fun ByteArray.sha256(toDigits: CharArray = DIGITS_LOWER): String = digest(SHA256, toDigits)
fun InputStream.sha256(toDigits: CharArray = DIGITS_LOWER): String = digest(SHA256, toDigits)

fun ByteArray.digest(algorithm: String, toDigits: CharArray = DIGITS_LOWER): String =
    MessageDigest.getInstance(algorithm)
        .digest(this)
        .encodeHex(toDigits)
        .let { String(it) }

fun InputStream.digest(algorithm: String, toDigits: CharArray = DIGITS_LOWER): String =
    MessageDigest.getInstance(algorithm)
        .update(this)
        .digest()
        .encodeHex(toDigits)
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