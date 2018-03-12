package com.eug.md.utils.io

import com.google.common.util.concurrent.RateLimiter
import java.io.InputStream

class BandwidthThrottlingInputStream(
        private val wrappedInputStream: InputStream,
        bytesPerSecondLimit: Double)
    : InputStream() {

    private val bytesPerSecondLimiter = RateLimiter.create(bytesPerSecondLimit)

    override fun read(): Int {
        bytesPerSecondLimiter.acquire()
        return wrappedInputStream.read()
    }

    override fun read(buffer: ByteArray): Int {
        val readLen = wrappedInputStream.read(buffer)
        if (readLen != -1) {
            bytesPerSecondLimiter.acquire(readLen)
        }
        return readLen
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val readLen = wrappedInputStream.read(buffer, offset, length)
        if (readLen != -1) {
            bytesPerSecondLimiter.acquire(readLen)
        }
        return readLen
    }

    override fun skip(bytesNumberToSkip: Long): Long = wrappedInputStream.skip(bytesNumberToSkip)

    override fun available(): Int = wrappedInputStream.available()

    override fun reset() = wrappedInputStream.reset()

    override fun close() = wrappedInputStream.close()

    override fun mark(readlimit: Int) = wrappedInputStream.mark(readlimit)

    override fun markSupported(): Boolean = wrappedInputStream.markSupported()
}