package com.eug.md

import com.eug.md.utils.measureExecTime
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.github.glytching.junit.extension.folder.TemporaryFolder
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension
import io.github.glytching.junit.extension.random.Random
import io.github.glytching.junit.extension.random.RandomBeansExtension
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import ru.lanwen.wiremock.ext.WiremockUriResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri
import java.io.File
import java.nio.file.Path

@ExtendWith(value = [
    TemporaryFolderExtension::class,
    WiremockResolver::class,
    WiremockUriResolver::class,
    RandomBeansExtension::class
])
class DownloaderIT(
        @Wiremock private val mockServer: WireMockServer,
        @WiremockUri private val mockServerUri: String) {

    companion object {
        private val MAX_DOWNLOAD_SPEED = Double.MAX_VALUE
        private const val FILE_SAMPLES_DIR = "__files"
        private const val SAMPLE_FILE_NAME = "success-download-img-sample.png"
    }

    private lateinit var tempFolder: TemporaryFolder
    private lateinit var outDirPath: Path
    private lateinit var defaultConfig: DownloaderConfig

    @BeforeEach
    fun beforeEach(tempFolder: TemporaryFolder) {
        this.tempFolder = tempFolder
        this.outDirPath = tempFolder.createDirectory("download-results").toPath()
        this.defaultConfig = DownloaderConfig(
                outDirPath = outDirPath,
                speedLimitInBytes = MAX_DOWNLOAD_SPEED,
                maxKeepAliveConnectionTotal = 1,
                perRouteKeepAliveConnectionLimit = 1
        )
    }

    @Test
    fun `should return failed result when there aren't such file by passed url`(
            @Random fileNameUrlPath: String, @Random outFileName: String) {

        mockServer.stubFor(get(urlMatching("/$fileNameUrlPath"))
                .willReturn(notFound()))

        val task = DownloadTask(number = 1, url = resolveUrlToFile(fileNameUrlPath), fileNames = listOf(outFileName))
        val downloadResult = Downloader(defaultConfig).use { it.download(task) }

        assertEquals(Status.FAILED, downloadResult.status)
        assertEquals(0, downloadResult.savedBytesCount)
        assertEquals(downloadResult::class, FailedResult::class)

        val exception = (downloadResult as FailedResult).exception
        assertEquals(exception::class, UnexpectedResponseStatusException::class)
        assertEquals("Unexpected response status - 404", exception.message)
    }

    @Test
    fun `should return success result when requested file exist`(
            @Random fileNameUrlPath: String, @Random outFileName: String) {

        mockServer.stubFor(get(urlMatching("/$fileNameUrlPath"))
                .willReturn(ok().withBodyFile(SAMPLE_FILE_NAME)))

        val task = DownloadTask(number = 1, url = resolveUrlToFile(fileNameUrlPath), fileNames = listOf(outFileName))
        val downloadResult = Downloader(defaultConfig).use { it.download(task) }

        assertEquals(Status.SUCCESS, downloadResult.status)
        assertEquals(downloadResult::class, SuccessResult::class)

        val sampleFileSize = sizeOfTestResourcesFile(SAMPLE_FILE_NAME)
        assertEquals(sampleFileSize, downloadResult.savedBytesCount)
        assertEquals(sampleFileSize, outFileSize(outFileName))
    }

    @Test
    fun `should download file with speed restriction if passed download speed fewer than file size`(
            @Random fileNameUrlPath: String, @Random outFileName: String) {

        mockServer.stubFor(get(urlMatching("/$fileNameUrlPath"))
                .willReturn(ok().withBodyFile(SAMPLE_FILE_NAME)))

        val downloadSpeed = 1024.0
        val sampleFileSize = sizeOfTestResourcesFile(SAMPLE_FILE_NAME)
        val expectedDownloadTimeInSeconds = sampleFileSize / downloadSpeed
        val downloaderConfig = DownloaderConfig(
                outDirPath = outDirPath,
                speedLimitInBytes =  downloadSpeed,
                maxKeepAliveConnectionTotal = 1,
                perRouteKeepAliveConnectionLimit = 1
        )

        val task = DownloadTask(number = 1, url = resolveUrlToFile(fileNameUrlPath), fileNames = listOf(outFileName))
        val measuredDownloadResult = measureExecTime { Downloader(downloaderConfig).use { it.download(task) } }

        val actualDownloadTimeInSecond = measuredDownloadResult.executionTime / 1000.0
        val measureError = 0.5
        assertEquals(expectedDownloadTimeInSeconds, actualDownloadTimeInSecond, measureError)

        val downloadResult = measuredDownloadResult.result
        assertEquals(Status.SUCCESS, downloadResult.status)
        assertEquals(downloadResult::class, SuccessResult::class)
        assertEquals(sampleFileSize, downloadResult.savedBytesCount)
        assertEquals(sampleFileSize, outFileSize(outFileName))
    }

    @Test
    fun `should download to several out files if multiple out files specified for one url`(
            @Random fileNameUrlPath: String, @Random(type = String::class) outFileNames: List<String>) {

        mockServer.stubFor(get(urlMatching("/$fileNameUrlPath"))
                .willReturn(ok().withBodyFile(SAMPLE_FILE_NAME)))

        val task = DownloadTask(number = 1, url = resolveUrlToFile(fileNameUrlPath), fileNames = outFileNames)
        val downloadResult = Downloader(defaultConfig).use { it.download(task) }

        assertEquals(Status.SUCCESS, downloadResult.status)
        assertEquals(downloadResult::class, SuccessResult::class)

        val sampleFileSize = sizeOfTestResourcesFile(SAMPLE_FILE_NAME)
        assertEquals(sampleFileSize, downloadResult.savedBytesCount)
        outFileNames
                .map(this::outFileSize)
                .forEach { outFileSize -> assertEquals(sampleFileSize, outFileSize) }
    }

    @Test
    fun `should properly process 301 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        testRedirectionForStatus(
                HttpStatus.SC_MOVED_PERMANENTLY, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should properly process 302 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        testRedirectionForStatus(
                HttpStatus.SC_MOVED_TEMPORARILY, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should properly process 303 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        testRedirectionForStatus(
                HttpStatus.SC_SEE_OTHER, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should properly process 305 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        testRedirectionForStatus(
                HttpStatus.SC_USE_PROXY, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should properly process 307 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        testRedirectionForStatus(
                HttpStatus.SC_TEMPORARY_REDIRECT, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should properly process 308 redirection`(
            @Random firstFileNameUrlPath: String, @Random secondFileNameUrlPath: String, @Random outFileName: String) {

        val permanentRedirect = 308
        testRedirectionForStatus(permanentRedirect, firstFileNameUrlPath, secondFileNameUrlPath, outFileName)
    }

    @Test
    fun `should create directories after downloading if they exist in file name path`(
            @Random fileNameUrlPath: String, @Random outFileName: String, @Random outFileDirName: String) {

        mockServer.stubFor(get(urlMatching("/$fileNameUrlPath"))
                .willReturn(ok().withBodyFile(SAMPLE_FILE_NAME)))

        val outFilePath = "$outFileDirName/$outFileName"
        val task = DownloadTask(
                number = 1,
                url = resolveUrlToFile(fileNameUrlPath),
                fileNames = listOf(outFilePath)
        )
        val downloadResult = Downloader(defaultConfig).use { it.download(task) }

        assertEquals(Status.SUCCESS, downloadResult.status)
        assertEquals(downloadResult::class, SuccessResult::class)

        val sampleFileSize = sizeOfTestResourcesFile(SAMPLE_FILE_NAME)
        assertEquals(sampleFileSize, downloadResult.savedBytesCount)
        assertEquals(sampleFileSize, outFileSize(outFilePath))
    }

    private fun testRedirectionForStatus(
            status: Int, firstFileNameUrlPath: String, secondFileNameUrlPath: String, outFileName: String) {

        val redirectUrl = resolveUrlToFile(secondFileNameUrlPath)
        val firstUrlResponse = status(status).withHeader(HttpHeaders.LOCATION, redirectUrl)

        mockServer.stubFor(get(urlMatching("/$firstFileNameUrlPath"))
                .willReturn(firstUrlResponse))

        mockServer.stubFor(get(urlMatching("/$secondFileNameUrlPath"))
                .willReturn(ok().withBodyFile(SAMPLE_FILE_NAME)))

        val task =
                DownloadTask(number = 1, url = resolveUrlToFile(firstFileNameUrlPath), fileNames = listOf(outFileName))
        val downloadResult = Downloader(defaultConfig).use { it.download(task) }
        assertEquals(Status.SUCCESS, downloadResult.status)
        assertEquals(downloadResult::class, SuccessResult::class)

        val sampleFileSize = sizeOfTestResourcesFile(SAMPLE_FILE_NAME)
        assertEquals(sampleFileSize, downloadResult.savedBytesCount)
        assertEquals(sampleFileSize, outFileSize(outFileName))
    }

    private fun resolveUrlToFile(fileName: String) = "$mockServerUri/$fileName"

    private fun sizeOfTestResourcesFile(fileName: String): Long {
        val classLoader = javaClass.classLoader
        return File(classLoader.getResource("$FILE_SAMPLES_DIR/$fileName").file).length()
    }

    private fun outFileSize(outFileName: String) = outDirPath.resolve(outFileName).toFile().length()
}