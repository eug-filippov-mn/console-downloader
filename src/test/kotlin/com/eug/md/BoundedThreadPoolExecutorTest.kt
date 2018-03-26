package com.eug.md

import com.eug.md.utils.concurrent.BoundedThreadPoolExecutor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BoundedThreadPoolExecutorTest {
    companion object {
        private const val FIRST_TASK_EXEC_TIME = 10_000L
        private const val THREAD_BLOCK_DETECT_TIMEOUT = 2000L
        private const val BLOCK_THREAD_JOIN_TIMEOUT = 2000L

        private const val EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 2L
    }

    @Test
    fun executeShouldBlockExecutionThreadWhenExecutorBoundIsReached() {
        Assumptions.assumeTrue(
                FIRST_TASK_EXEC_TIME / 2 > THREAD_BLOCK_DETECT_TIMEOUT,
                "First task exec time must be greater than block timeout for proper test execution"
        )

        val executor = BoundedThreadPoolExecutor(
                numberOfThreads = 10,
                threadsNameFormat = "does-not-matter",
                bound = 1
        )

        val outOfBoundTaskExecutionThread = thread(start = false) {
            try {
                executor.execute {
                    try {
                        fail { "Execute should block execution thread on full task queue" }
                    } catch (success: InterruptedException) { }
                }
                fail { "Execute should block execution thread on full task queue" }
            } catch (success: InterruptedException) { }
        }

        try {
            executor.execute {
                try {
                    Thread.sleep(FIRST_TASK_EXEC_TIME)
                } catch (success: InterruptedException) { }
            }
            outOfBoundTaskExecutionThread.start()
            Thread.sleep(THREAD_BLOCK_DETECT_TIMEOUT)

            outOfBoundTaskExecutionThread.interrupt()
            outOfBoundTaskExecutionThread.join(BLOCK_THREAD_JOIN_TIMEOUT)

            executor.shutdownAndAwaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            Assertions.assertFalse(outOfBoundTaskExecutionThread.isAlive)
        } catch (unexpected: Exception) {
            fail("Unexpected exception" , unexpected)
        }

    }
}