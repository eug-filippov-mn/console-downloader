package com.eug.md.utils.concurrent

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.*


class BoundedThreadPoolExecutor(
        numberOfThreads: Int,
        threadsNameFormat: String = "pool-%d-thread",
        bound: Int) : Executor {

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
        MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit)
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