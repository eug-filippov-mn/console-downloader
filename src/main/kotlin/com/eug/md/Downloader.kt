package com.eug.md

import com.eug.md.utils.http.RedirectStrategy
import com.eug.md.utils.io.BandwidthThrottlingInputStream
import com.eug.md.utils.io.MultiOutputStream
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

data class DownloaderConfig(
        val outDirPath: Path,
        val speedLimitInBytes: Double,
        val maxKeepAliveConnectionTotal: Int,
        val perRouteKeepAliveConnectionLimit: Int)

class Downloader(private val config: DownloaderConfig): Closeable {

    companion object {
        private val log = LoggerFactory.getLogger(Downloader::class.java)
    }

    private val httpClient =
            HttpClientBuilder
                    .create()
                    .setMaxConnTotal(config.maxKeepAliveConnectionTotal)
                    .setMaxConnPerRoute(config.perRouteKeepAliveConnectionLimit)
                    .setRedirectStrategy(RedirectStrategy())
                    .build()

    fun download(downloadTask: DownloadTask): DownloadResult {
        val taskUrl = downloadTask.url

        try {
            MDC.put("uuid", UUID.randomUUID().toString())
            log.debug("Start processing downloadTask {}", downloadTask)

            val httpGet = HttpGet(taskUrl)

            httpClient.execute(httpGet).use { response ->
                val statusLine = response.statusLine
                if (statusLine.statusCode != HttpStatus.SC_OK) {
                    throw UnexpectedResponseStatusException(statusLine.statusCode)
                }
                val savedBytesCount = download(response.entity.content, downloadTask.fileNames)
                val successResult = SuccessResult(downloadTask.number, taskUrl, savedBytesCount)

                log.debug("DownloadTask processing result {}", successResult)
                return successResult
            }

        } catch (e: Exception) {
            log.error("DownloadTask processing exception", e)

            val failedResult = FailedResult(downloadTask.number, taskUrl, e)
            log.debug("DownloadTask processing result {}", failedResult)
            return failedResult
        } finally {
            MDC.clear()
        }
    }

    private fun download(connectionInputStream: InputStream, fileNames: List<String>): Long {
        val bandwidthThrottlingInputStream =
                BandwidthThrottlingInputStream(connectionInputStream.buffered(), config.speedLimitInBytes)

        val outputFilePaths = fileNames.map(config.outDirPath::resolve)
        outputFilePaths.map(Path::getParent).forEach { parentPath ->
            if (!Files.exists(parentPath)) {
                log.debug("Creating directories for path {}", parentPath)
                Files.createDirectories(parentPath)
            }
        }

        val outputStreams = outputFilePaths.map { path ->
            Files.newOutputStream(path, StandardOpenOption.CREATE).buffered()
        }
        return MultiOutputStream(outputStreams).use { bandwidthThrottlingInputStream.copyTo(it) }
    }

    override fun close() {
        httpClient.close()
    }
}

enum class Status {
    FAILED,
    SUCCESS
}

sealed class DownloadResult {
    abstract val taskNumber: Int
    abstract val savedBytesCount: Long
    abstract val url: String
    abstract val status: Status
}

data class SuccessResult(
        override val taskNumber: Int,
        override val url: String,
        override val savedBytesCount: Long)
    : DownloadResult() {

    override val status: Status
        get() = Status.SUCCESS
}

data class FailedResult(
        override val taskNumber: Int,
        override val url: String,
        val exception: Exception)
    : DownloadResult() {

    override val savedBytesCount: Long
        get() = 0
    override val status: Status
        get() = Status.FAILED
}