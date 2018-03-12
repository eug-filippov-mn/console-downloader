package com.eug.md.utils.concurrent

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun threadPoolExecutor(
        numberOfThreads: Int,
        threadsNameFormat: String,
        keepAliveTime: Long = 0L,
        keepAliveTimeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue()
): ThreadPoolExecutor {

    val threadFactory = ThreadFactoryBuilder().setNameFormat(threadsNameFormat).build()
    return ThreadPoolExecutor(
            numberOfThreads,
            numberOfThreads,
            keepAliveTime,
            keepAliveTimeUnit,
            workQueue,
            threadFactory
    )
}