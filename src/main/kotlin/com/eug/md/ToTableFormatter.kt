package com.eug.md

import com.eug.md.utils.MeasuredResult
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

object ToTableFormatter {
    private val headers = arrayOf("#", "Url", "Result size, bytes", "Status", "Execution time, ms")

    fun format(result: MeasuredResult<List<MeasuredResult<DownloadResult>>>) : String {
        val (totalExecutionTime, downloadMeasuredResults) = result

        return with(AsciiTable()) {
            addRule()
            addRow(*headers)

            downloadMeasuredResults.forEach {
                addRule()
                addRow(
                        it.result.taskNumber,
                        it.result.url,
                        it.result.savedBytesCount,
                        formatStatus(it.result),
                        it.executionTime
                )
            }

            addRule()
            addRow(null, null, null, "Total execution time", totalExecutionTime)

            addRule()
            setTextAlignment(TextAlignment.CENTER)
        }.render()
    }

    private fun formatStatus(result: DownloadResult) : String  =
            when (result) {
                is SuccessResult -> result.status.name
                is FailedResult -> when(result.exception) {
                    is ConsoleDownloaderException -> "${result.status.name}. ${result.exception.message}"
                    else -> "${result.status.name} (${result.exception.javaClass.simpleName} ${result.exception.message})"
                }
            }

}