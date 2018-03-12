package com.eug.md.utils

inline fun <T> measureExecTime(block: () -> T): MeasuredResult<T> {
    val start = System.currentTimeMillis()
    val result = block()
    return MeasuredResult(System.currentTimeMillis() - start, result)
}

data class MeasuredResult<out T>(val executionTime: Long, val result: T)