package com.eug.md

import com.eug.md.utils.*
import com.eug.md.utils.concurrent.threadPoolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.*
import java.util.function.Supplier
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp.run(args)
}

class ConsoleDownloaderApp private constructor(
        private val settings: Settings,
        private val downloader: Downloader,
        private val executor: ExecutorService) {

    private val out: PrintStream = System.out
    private val scanner: Scanner = Scanner(System.`in`)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)
        private const val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50

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
                val executor = threadPoolExecutor(
                        numberOfThreads = settings.threadsNumber,
                        threadsNameFormat = "app-pool thrd-%d"
                )

                val app = ConsoleDownloaderApp(settings, downloader, executor)
                app.run()
            } catch (e: Exception) {
                log.error(e.message, e)
                System.err.println(e.message)
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
        log.debug("Start application")
        log.debug("{}", settings)

        val measuredDownloadingResults = measureExecTime { downloadFiles() }

        log.debug("Downloading results {}", measuredDownloadingResults)

        out.println(ToTableFormatter.format(measuredDownloadingResults))
    }

    private fun downloadFiles() : List<MeasuredResult<DownloadResult>> {
        try {
            val taskCreationResult = DownloadTaskFactory.createTasks(settings.linksFilePath)

            if (taskCreationResult.invalidRowNumbers.isNotEmpty()) {
                out.println("Invalid format at rows - ${taskCreationResult.invalidRowNumbers}.")
                if (!confirmContinue(taskCreationResult)) {
                    exitProcess(0)
                }
            }

            return this.downloader.use { downloader ->
                taskCreationResult.tasks
                        .map { downloadTask ->
                            val downloadAction = Supplier { measureExecTime { downloader.download(downloadTask) } }
                            CompletableFuture.supplyAsync(downloadAction, executor)
                        }
                        .joinAll()
            }

        } finally {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.MINUTES)
        }
    }

    private tailrec fun confirmContinue(tasksCreationResult: TasksCreationResult): Boolean {
        out.println("${tasksCreationResult.tasks.size} out of ${tasksCreationResult.rowsCount} rows will be processed.")
        out.println("Continue processing? y/n")

        val confirmLine = scanner.nextLine().trim()
        return when(confirmLine) {
            "y","Y" -> true
            "n","N" -> false
            else -> {
                out.println("Invalid input")
                confirmContinue(tasksCreationResult)
            }
        }
    }
}
