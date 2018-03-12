package com.eug.md

import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.concurrent.threadPoolExecutor
import com.eug.md.utils.joinAll
import com.eug.md.utils.measureExecTime
import com.xenomachina.argparser.ArgParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.util.*
import java.util.concurrent.*
import java.util.function.Supplier
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp(args).run()
}

class ConsoleDownloaderApp {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)
        private val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50
    }

    private val out: PrintStream = System.out
    private val scanner: Scanner = Scanner(System.`in`)
    private val options: Options
    private val downloader: Downloader
    private val executor: ExecutorService

    constructor(args: Array<String>) {
        try {
            options = Options(ArgParser(args))

            val downloaderConfig = DownloaderConfig(
                    outDirPath = options.outputDirPath,
                    speedLimitInBytes = options.speedLimit,
                    perRouteKeepAliveConnectionLimit = options.threadsNumbers,
                    maxKeepAliveConnectionTotal = maxOf(MAX_KEEP_ALIVE_CONNECTION_TOTAL, options.threadsNumbers)
            )
            downloader = Downloader(downloaderConfig)

            executor = threadPoolExecutor(
                    numberOfThreads = options.threadsNumbers,
                    threadsNameFormat = "app-pool thrd-%d"
            )
        } catch (e: Exception) {
            log.error(e.message, e)
            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    fun run() {
        try {
            log.debug("Start application")
            log.debug("{}", options)

            val measuredDownloadingResults = measureExecTime { downloadFiles() }

            log.debug("Downloading results {}", measuredDownloadingResults)

            out.println(ToTableFormatter.format(measuredDownloadingResults))
        } catch (e: Exception) {
            log.error(e.message, e)
            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    private fun downloadFiles() : List<MeasuredResult<DownloadResult>> {
        try {
            val taskCreationResult = DownloadTaskFactory.createTasks(options.linksFilePath)

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
