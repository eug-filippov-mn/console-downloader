package com.eug.md

import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.concurrent.BoundedThreadPoolExecutor
import com.eug.md.utils.concurrent.joinAll
import com.eug.md.utils.measureExecTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp.run(args)
}

class ConsoleDownloaderApp private constructor(
        private val settings: Settings,
        private val downloader: Downloader,
        private val executor: BoundedThreadPoolExecutor) {

    companion object {
        private const val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50
        private const val TASKS_BOUND = 100_000

        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)
        private val appIO: AppIO = AppIO(System.`in`, System.out, System.err)

        fun run(args: Array<String>) {
            try {
                val settings = Settings.from(args)
                createOutDirIfNotExists(settings.outputDirPath)

                val downloaderConfig = DownloaderConfig(
                        outDirPath = settings.outputDirPath,
                        speedLimitInBytes = settings.speedLimitInBytes,
                        perRouteKeepAliveConnectionLimit = settings.threadsNumber,
                        maxKeepAliveConnectionTotal = maxOf(MAX_KEEP_ALIVE_CONNECTION_TOTAL, settings.threadsNumber)
                )
                val downloader = Downloader(downloaderConfig)
                val executor = BoundedThreadPoolExecutor(
                        numberOfThreads = settings.threadsNumber,
                        threadsNameFormat = "app-pool thrd-%d",
                        bound = TASKS_BOUND
                )

                val app = ConsoleDownloaderApp(settings, downloader, executor)
                app.run()
            } catch (e: Exception) {
                appIO.printError(e.message)
                log.error(e.message, e)
                exitProcess(-1)
            }
        }

        private fun createOutDirIfNotExists(outDirPath: Path) {
            try {
                if (!Files.exists(outDirPath)) {
                    Files.createDirectories(outDirPath)
                }
            } catch (e: IOException) {
                throw OutDirCreationException(outDirPath, e)
            }
        }
    }

    fun run() {
        try {
            log.debug("Start application")
            log.debug("{}", settings)

            val measuredDownloadingResults = measureExecTime { downloadFiles() }

            log.debug("Downloading results {}", measuredDownloadingResults)
            appIO.printResults(measuredDownloadingResults)

        } finally {
            executor.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
        }
    }

    private fun downloadFiles() : List<MeasuredResult<DownloadResult>> {
        val tasksCreationResult = DownloadTaskFactory.createTasks(settings.linksFilePath)

        if (tasksCreationResult.invalidRowNumbers.isNotEmpty()) {
            val continueProcessing = appIO.requestContinueConfirmation(tasksCreationResult)
            if (!continueProcessing) {
                exitProcess(0)
            }
        }

        return downloader.use {
            tasksCreationResult.tasks.map { downloadTask -> executeTask(it, downloadTask) }.joinAll()
        }
    }

    private fun executeTask(downloader: Downloader, downloadTask: DownloadTask)
            : CompletableFuture<MeasuredResult<DownloadResult>> {

        val downloadAction = Supplier { measureExecTime { downloader.download(downloadTask) } }
        return CompletableFuture.supplyAsync(downloadAction, executor)
    }
}
