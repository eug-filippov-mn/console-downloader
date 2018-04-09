package com.eug.md

import com.eug.md.settings.Settings
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.measureExecTime
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import kotlin.concurrent.thread

class TasksLauncher(settings: Settings) {

    private val semaphore = Semaphore(TASKS_NUMBER_BOUND)

    private val downloader = Downloader(DownloaderConfig(
            outDirPath = settings.outputDirPath,
            speedLimitInBytes = settings.speedLimitInBytes,
            perRouteKeepAliveConnectionLimit = settings.threadsNumber,
            maxKeepAliveConnectionTotal = maxOf(MAX_KEEP_ALIVE_CONNECTION_TOTAL, settings.threadsNumber)
    ))

    private val downloadTasksExecutor = Executors.newFixedThreadPool(
            settings.threadsNumber,
            ThreadFactoryBuilder().setNameFormat("download thrd-%d").build()
    )

    private val downloadTasksCompletionService = ExecutorCompletionService<MeasuredResult<DownloadResult>>(
            downloadTasksExecutor
    )

    @Volatile
    var stopped: Boolean = false
        private set

    private lateinit var launcherThread: Thread

    fun startDownloading(tasks: List<DownloadTask>) {
        launcherThread = thread(name = "launcher-thrd") {
            try {
                tasks.forEach(this::executeTask)
            } catch (e: InterruptedException) {
                log.debug("Launcher thread was interrupted")
                stopped = true
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun executeTask(task: DownloadTask) {
        semaphore.acquire()
        try {
            downloadTasksCompletionService.submit {
                try {
                    measureExecTime { downloader.download(task) }
                } finally {
                    semaphore.release()
                }
            }
        } catch (e: RejectedExecutionException) {
            log.warn("Task was rejected {}", task, e)
            semaphore.release()
        }
    }

    fun takeNextCompleted(): MeasuredResult<DownloadResult>  {
        try {

            return downloadTasksCompletionService.take().get()

        } catch (e: ExecutionException) {
            e.cause?.let { throw it }
            throw e
        }
    }

    fun stop() {
        log.debug("Stop request received")

        log.debug("Interrupting launcher thread...")
        launcherThread.interrupt()

        log.debug("Shutting down executor...")
        MoreExecutors.shutdownAndAwaitTermination(downloadTasksExecutor, 1, TimeUnit.MINUTES)
        log.debug("Executor shut down")
    }

    companion object {
        private const val TASKS_NUMBER_BOUND = 100_000
        private const val MAX_KEEP_ALIVE_CONNECTION_TOTAL = 50

        private val log: Logger = LoggerFactory.getLogger(TasksLauncher::class.java)
    }
}