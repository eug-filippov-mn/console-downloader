package com.eug.md.utils

import java.io.Closeable

fun Closeable?.closeSilently(cause: Throwable) {
    if (this != null) {
        try {
            close()
        } catch (e: Throwable) {
            cause.addSuppressed(e)
        }
    }
}