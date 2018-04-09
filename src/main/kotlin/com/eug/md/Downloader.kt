package com.eug.md

import com.eug.md.utils.LimitedKeepAliveStrategy
import com.eug.md.utils.PermanentRedirectSupportRedirectStrategy
import com.eug.md.utils.io.BandwidthThrottlingInputStream
import com.eug.md.utils.io.toMultioutputStream
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
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

    private val httpClient = buildHttpClient(config)

    fun download(downloadTask: DownloadTask): DownloadResult {
        val taskUrl = downloadTask.url

        try {
            MDC.put("uuid", UUID.randomUUID().toString())
            log.debug("Start processing download task {}", downloadTask)

            log.debug("Executing request for url {}", taskUrl)
            return httpClient.execute(HttpGet(taskUrl), HttpResponseHandler(downloadTask))

        } catch (e: Exception) {
            when(e) {
                is UnexpectedResponseStatusException -> log.warn("Download task processing exception {}", e.message)
                is InterruptedException -> Thread.currentThread().interrupt()
                else -> log.error("Download task processing exception", e)
            }

            val failedResult = FailedResult(downloadTask.number, taskUrl, e)
            log.debug("Download task processing result {}", failedResult)
            return failedResult
        } finally {
            MDC.clear()
        }
    }

    private inner class HttpResponseHandler(private val downloadTask: DownloadTask) : ResponseHandler<DownloadResult> {

        override fun handleResponse(response: HttpResponse): DownloadResult {
            log.debug("Receive response")
            val statusCode = response.statusLine.statusCode
            if (statusCode != HttpStatus.SC_OK) {
                throw UnexpectedResponseStatusException(statusCode)
            }

            log.debug("Starting download content")
            val savedBytesCount = download(response.entity.content, downloadTask.fileNames)
            val successResult = SuccessResult(downloadTask.number, downloadTask.url, savedBytesCount)

            log.debug("Download task processing result {}", successResult)
            return successResult
        }

        private fun download(connectionInputStream: InputStream, fileNames: List<String>): Long {
            val outputFilePaths = fileNames.map(config.outDirPath::resolve)
            createDirectoriesIfNotExist(outputFilePaths)

            val multiOutputStream =
                    outputFilePaths
                            .map { path -> Files.newOutputStream(path, StandardOpenOption.CREATE).buffered() }
                            .toMultioutputStream()

            val bandwidthThrottlingInputStream =
                    BandwidthThrottlingInputStream(connectionInputStream.buffered(), config.speedLimitInBytes)

            return multiOutputStream.drainFrom(bandwidthThrottlingInputStream)
        }

        private fun createDirectoriesIfNotExist(outputFilePaths: Collection<Path>) {
            outputFilePaths.map(Path::getParent).forEach { parentPath ->
                if (!Files.exists(parentPath)) {
                    log.debug("Creating directories for path {}", parentPath)
                    Files.createDirectories(parentPath)
                }
            }
        }
    }

    override fun close() {
        log.debug("Closing downloader")
        httpClient.close()
        log.debug("Downloader closed")
    }

    companion object {
        private val log = LoggerFactory.getLogger(Downloader::class.java)

        private fun buildHttpClient(config: DownloaderConfig): CloseableHttpClient {
            val requestConfig =
                    RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD)
                            .setConnectionRequestTimeout(120_000)
                            .setConnectTimeout(60_000)
                            .setSocketTimeout(60_000)
                            .build()

            return HttpClientBuilder.create()
                    .setMaxConnTotal(config.maxKeepAliveConnectionTotal)
                    .setMaxConnPerRoute(config.perRouteKeepAliveConnectionLimit)
                    .setDefaultRequestConfig(requestConfig)
                    .setRedirectStrategy(PermanentRedirectSupportRedirectStrategy())
                    .setKeepAliveStrategy(LimitedKeepAliveStrategy(defaultKeepAliveMills = 120_000))
                    .evictExpiredConnections()
                    .build()
        }
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