package com.eug.md

import com.eug.md.arguments.ArgParser
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.concurrent.BoundedThreadPoolExecutor
import com.eug.md.utils.concurrent.joinAll
import com.eug.md.utils.measureExecTime
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp.run(args)
}

class ConsoleDownloaderApp private constructor(
        private val settings: Settings) : Closeable  {

    private val tasksProgressExecutor: ExecutorService = Executors.newSingleThreadExecutor(
            ThreadFactoryBuilder().setNameFormat("tasks-progress-thrd").build()
    )

    private val downloadTaskExecutor = BoundedThreadPoolExecutor(
            numberOfThreads = settings.threadsNumber,
            threadsNameFormat = "download thrd-%d",
            bound = TASKS_BOUND
    )

    private val downloader =
        Downloader(DownloaderConfig(
                outDirPath = settings.outputDirPath,
                speedLimitInBytes = settings.speedLimitInBytes,
                perRouteKeepAliveConnectionLimit = settings.threadsNumber,
                maxKeepAliveConnectionTotal = maxOf(MAX_KEEP_ALIVE_CONNECTION_TOTAL, settings.threadsNumber)
        ))

    fun run() {
        terminal.statusLine("Processing file with links")
        val tasksCreationResult = DownloadTaskFactory.createTasks(settings.linksFilePath)
        validateTasksCreationResult(tasksCreationResult)

        terminal.statusLine("Starting downloading")
        val measuredDownloadingResults = measureExecTime { downloadFiles(tasksCreationResult.tasks) }

        terminal.statusLine("Downloading finished")
        log.debug("Results {}", measuredDownloadingResults)
        terminal.printResults(measuredDownloadingResults)
    }

    private fun downloadFiles(tasks: List<DownloadTask>): List<MeasuredResult<DownloadResult>> {
        try {

            val completedCount = AtomicInteger(0)

            val futures = tasks.map { downloadTask ->
                executeTask(downloader, downloadTask)
                        .whenCompleteAsync(BiConsumer { _, _ ->
                            terminal.statusLine("${completedCount.incrementAndGet()} / ${tasks.size} completed")
                        }, tasksProgressExecutor)
            }

            return futures.joinAll()

        } catch (e: CompletionException) {
            e.cause?.let { throw  it}
            throw e
        }
    }

    private fun validateTasksCreationResult(tasksCreationResult: TasksCreationResult) {
        val invalidRowNumbers = tasksCreationResult.invalidRowNumbers
        if (invalidRowNumbers.isNotEmpty()) {
            if (settings.interactiveMode) {
                val continueProcessing = terminal.requestContinueConfirmation(tasksCreationResult)
                if (!continueProcessing) {
                    exitProcess(0)
                }
            } else {
                throw LinksFileParseException.invalidFormatAtLine(settings.linksFilePath, invalidRowNumbers)
            }
        }
    }

    private fun executeTask(downloader: Downloader, downloadTask: DownloadTask)
            : CompletableFuture<MeasuredResult<DownloadResult>> {

        val downloadAction = Supplier { measureExecTime { downloader.download(downloadTask) } }
        return CompletableFuture.supplyAsync(downloadAction, downloadTaskExecutor)
    }

    override fun close() {
        log.debug("Closing app")

        downloadTaskExecutor.shutdownAndAwaitTermination(10, TimeUnit.SECONDS)
        MoreExecutors.shutdownAndAwaitTermination(tasksProgressExecutor, 5, TimeUnit.SECONDS)
        downloader.close()

        log.debug("App closed")
    }

    companion object {
        private const val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50
        private const val TASKS_BOUND = 100_000

        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)

        private val terminal: Terminal = Terminal()

        fun run(args: Array<String>) {
            try {

                if (ArgParser.containsHelp(args)) {
                    terminal.printHelp()
                    exitProcess(0)
                }

                terminal.statusLine("Start arguments parsings")
                val settings = Settings.from(ArgParser.parse(args))
                log.debug("App settings from parsed args {}", settings)
                createOutDirIfNotExists(settings.outputDirPath)

                terminal.statusLine("Starting application")
                ConsoleDownloaderApp(settings).use { it.run() }

            } catch (e: Throwable) {
                terminal.printError(e.message)
                log.error(e.message, e)
                exitProcess(-1)
            }
        }

        private fun createOutDirIfNotExists(outDirPath: Path) {
            try {
                if (!Files.exists(outDirPath)) {
                    terminal.statusLine("Creating directories for out path")
                    Files.createDirectories(outDirPath)
                }
            } catch (e: IOException) {
                throw OutDirCreationException(outDirPath, e)
            }
        }
    }
}
