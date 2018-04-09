package com.eug.md.utils.io

import java.io.InputStream
import java.io.OutputStream

class MultiOutputStream(private val outputStreams: Collection<OutputStream>) : OutputStream() {

    override fun write(byte: Int) {
        outputStreams.forEach { it.write(byte) }
    }

    override fun write(buffer: ByteArray?) {
        outputStreams.forEach { it.write(buffer) }
    }

    override fun write(buffer: ByteArray?, offset: Int, length: Int) {
        outputStreams.forEach { it.write(buffer, offset, length) }
    }

    override fun flush() {
        outputStreams.forEach { it.flush() }
    }

    override fun close() {
        outputStreams.forEach { it.close() }
    }

    fun drainFrom(inputStream: InputStream): Long {
        return use { multiOutputStream ->
            inputStream.use {
                it.copyTo(multiOutputStream)
            }
        }
    }
}

fun Collection<OutputStream>.toMultioutputStream(): MultiOutputStream = MultiOutputStream(this)