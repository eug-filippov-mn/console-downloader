package com.eug.md.utils.concurrent

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*


class BoundedThreadPoolExecutor(
        numberOfThreads: Int,
        threadsNameFormat: String,
        bound: Int) : Executor {

    private val semaphore = Semaphore(bound)
    private val executor = Executors.newFixedThreadPool(
            numberOfThreads,
            ThreadFactoryBuilder().setNameFormat(threadsNameFormat).build()
    )

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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BoundedThreadPoolExecutor::class.java)
    }
}