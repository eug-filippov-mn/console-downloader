package com.eug.md

import com.eug.md.arguments.ArgParser
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.closeSilently
import com.eug.md.utils.concurrent.BoundedThreadPoolExecutor
import com.eug.md.utils.concurrent.joinAll
import com.eug.md.utils.concurrent.shutdownNowAndAwaitTerminationSilently
import com.eug.md.utils.measureExecTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp.run(args)
}

class ConsoleDownloaderApp private constructor(
        private val settings: Settings,
        private val downloader: Downloader,
        private val executor: BoundedThreadPoolExecutor) : Closeable  {

    companion object {
        private const val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50
        private const val TASKS_BOUND = 100_000

        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)
        private val appIO: AppIO = AppIO(System.`in`, System.out, System.err)

        fun run(args: Array<String>) {
            var downloader: Downloader? = null
            var executor: BoundedThreadPoolExecutor? = null

            try {
                log.debug("Start arguments parsings")
                if (ArgParser.containsHelp(args)) {
                    appIO.printHelp()
                    exitProcess(0)
                }

                val settings = Settings.from(ArgParser.parse(args))
                log.debug("App settings from parsed args {}", settings)
                createOutDirIfNotExists(settings.outputDirPath)

                log.debug("Starting app's components configuration")
                val downloaderConfig = DownloaderConfig(
                        outDirPath = settings.outputDirPath,
                        speedLimitInBytes = settings.speedLimitInBytes,
                        perRouteKeepAliveConnectionLimit = settings.threadsNumber,
                        maxKeepAliveConnectionTotal = maxOf(MAX_KEEP_ALIVE_CONNECTION_TOTAL, settings.threadsNumber)
                )
                downloader = Downloader(downloaderConfig)
                executor = BoundedThreadPoolExecutor(
                        numberOfThreads = settings.threadsNumber,
                        threadsNameFormat = "app-pool thrd-%d",
                        bound = TASKS_BOUND
                )

                log.debug("Starting application")
                ConsoleDownloaderApp(settings, downloader, executor).use { it.run() }

            } catch (e: Throwable) {
                downloader.closeSilently(e)
                executor.shutdownNowAndAwaitTerminationSilently(1, TimeUnit.SECONDS, e)

                appIO.printError(e.message)
                log.error(e.message, e)
                exitProcess(-1)
            }
        }

        private fun createOutDirIfNotExists(outDirPath: Path) {
            try {
                if (!Files.exists(outDirPath)) {
                    log.debug("Creating directories for out path")
                    Files.createDirectories(outDirPath)
                }
            } catch (e: IOException) {
                throw OutDirCreationException(outDirPath, e)
            }
        }
    }

    fun run() {
        log.debug("Starting downloading")

        //todo add status line
        val measuredDownloadingResults = measureExecTime { downloadFiles() }

        log.debug("Downloading finished. Results {}", measuredDownloadingResults)
        appIO.printResults(measuredDownloadingResults)
    }

    private fun downloadFiles() : List<MeasuredResult<DownloadResult>> {
        try {

            val tasksCreationResult = DownloadTaskFactory.createTasks(settings.linksFilePath)
            validateTasksCreationResult(tasksCreationResult)

            return tasksCreationResult.tasks.map { downloadTask -> executeTask(downloader, downloadTask) }.joinAll()

        } catch (e: CompletionException) {
            e.cause?.let { throw  it}
            throw e
        }
    }

    private fun validateTasksCreationResult(tasksCreationResult: TasksCreationResult) {
        val invalidRowNumbers = tasksCreationResult.invalidRowNumbers
        if (invalidRowNumbers.isNotEmpty()) {
            if (settings.interactiveMode) {
                val continueProcessing = appIO.requestContinueConfirmation(tasksCreationResult)
                if (!continueProcessing) {
                    exitProcess(0)
                }
            } else{
                throw LinksFileParseException.invalidFormatAtLine(settings.linksFilePath, invalidRowNumbers)
            }
        }
    }

    private fun executeTask(downloader: Downloader, downloadTask: DownloadTask)
            : CompletableFuture<MeasuredResult<DownloadResult>> {

        val downloadAction = Supplier { measureExecTime { downloader.download(downloadTask) } }
        return CompletableFuture.supplyAsync(downloadAction, executor)
    }

    override fun close() {
        log.debug("Closing app")
        executor.shutdownAndAwaitTermination(10, TimeUnit.SECONDS)
        downloader.close()
        log.debug("App closed")
    }
}
