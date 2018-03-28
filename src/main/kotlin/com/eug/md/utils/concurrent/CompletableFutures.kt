package com.eug.md.utils.concurrent

import java.util.concurrent.CompletableFuture

fun <T> List<CompletableFuture<T>>.joinAll() : List<T> {
    val allFuturesResult = CompletableFuture.allOf(*this.toTypedArray())

    return allFuturesResult
            .thenApply { this.map { future -> future.join() } }
            .join()
}