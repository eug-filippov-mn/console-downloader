package com.eug.md.utils.concurrent

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*


class BoundedThreadPoolExecutor(
        numberOfThreads: Int,
        threadsNameFormat: String = "pool-%d-thread",
        bound: Int) : Executor {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BoundedThreadPoolExecutor::class.java)
    }

    private val executor = threadPoolExecutor(
            corePoolSize = numberOfThreads,
            maximumPoolSize = numberOfThreads,
            threadsNameFormat = threadsNameFormat,
            keepAliveTime = 0L,
            keepAliveTimeUnit = TimeUnit.MILLISECONDS,
            workQueue = LinkedBlockingQueue()
    )
    private val semaphore = Semaphore(bound)

    override fun execute(command: Runnable?) {
        blockingExecute(command)
    }

    private fun blockingExecute(command: Runnable?) {
        semaphore.acquire()
        try {
            executor.execute({
                try {
                    command?.run()
                } finally {
                    semaphore.release()
                }
            })
        } catch (e: RejectedExecutionException) {
            semaphore.release()
        }
    }

    fun shutdownAndAwaitTermination(timeout: Long, unit: TimeUnit) {
        log.debug("Shutting down executor")
        MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit)
        log.debug("Executor shut down")
    }

    fun shutdownNowAndAwaitTermination(timeout: Long, unit: TimeUnit) {
        log.debug("Shutting down executor")
        executor.shutdownNow()
        executor.awaitTermination(timeout, unit)
        log.debug("Executor shut down")
    }

    private fun threadPoolExecutor(
            corePoolSize: Int,
            maximumPoolSize: Int,
            threadsNameFormat: String,
            keepAliveTime: Long,
            keepAliveTimeUnit: TimeUnit,
            workQueue: BlockingQueue<Runnable>
    ): ThreadPoolExecutor {

        val threadFactory = ThreadFactoryBuilder().setNameFormat(threadsNameFormat).build()
        return ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                keepAliveTimeUnit,
                workQueue,
                threadFactory
        )
    }
}

fun BoundedThreadPoolExecutor?.shutdownNowAndAwaitTerminationSilently(timeout: Long, unit: TimeUnit, cause: Throwable) {
    if (this != null) {
        try {
            this.shutdownNowAndAwaitTermination(timeout, unit)
        } catch (e: Throwable) {
            cause.addSuppressed(e)
        }
    }
}