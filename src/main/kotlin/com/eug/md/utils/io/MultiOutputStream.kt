package com.eug.md.utils.io

import java.io.OutputStream

class MultiOutputStream(private val outputStreams: List<OutputStream>) : OutputStream() {

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
}