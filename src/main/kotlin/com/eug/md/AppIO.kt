package com.eug.md

import com.eug.md.arguments.Opts
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.format.*
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.apache.commons.cli.HelpFormatter
import java.io.InputStream
import java.io.PrintStream
import java.util.*

class AppIO(
        `in`: InputStream,
        private val out: PrintStream,
        private val err: PrintStream) {

    private val scanner  = Scanner(`in`)
    private val helpFormatter = HelpFormatter()

    fun requestContinueConfirmation(tasksCreationResult: TasksCreationResult): Boolean {
        out.println("Invalid format at rows - ${tasksCreationResult.invalidRowNumbers}.")
        out.println("${tasksCreationResult.tasks.size} out of ${tasksCreationResult.rowsCount} rows will be processed.")
        return processUserInput(tasksCreationResult)
    }

    private tailrec fun processUserInput(tasksCreationResult: TasksCreationResult): Boolean {
        out.println("Continue processing? y/n")

        val confirmLine = scanner.nextLine().trim()
        return when(confirmLine) {
            "y","Y" -> true
            "n","N" -> false
            else -> {
                out.println("Invalid input")
                processUserInput(tasksCreationResult)
            }
        }
    }

    fun printResults(results: MeasuredResult<List<MeasuredResult<DownloadResult>>>) {
        out.println(ToTableFormatter.format(results))
    }

    fun printHelp() {
        helpFormatter.printHelp("console-downloader [OPTION]...", Opts.all())
    }

    fun printError(message: String?) {
        err.println(message ?: "Unexpected error")
    }

    private object ToTableFormatter {
        private val HEADERS = arrayOf("#", "Url", "Status", "Downloaded bytes", "Execution time, ms")

        fun format(result: MeasuredResult<List<MeasuredResult<DownloadResult>>>) : String {
            val (totalExecutionTime, downloadMeasuredResults) = result

            val table = AsciiTable().apply {
                addHeaders(HEADERS)
                addRows(downloadMeasuredResults, columns = { measuredResult ->
                    arrayOf(
                            Column(measuredResult.result.taskNumber),
                            Column(measuredResult.result.url),
                            Column(formatStatus(measuredResult.result)),
                            Column(measuredResult.result.savedBytesCount),
                            Column(measuredResult.executionTime)
                    )
                })
                //todo add number format
                addTotal(
                        Column("Total downloaded bytes", colspan = 3),
                        Column(toTotalDownloadedBytesSize(downloadMeasuredResults), colspan = 2)
                )
                addTotal(
                        Column("Total execution time, ms", colspan = 3),
                        Column(totalExecutionTime, colspan = 2)
                )
                setAlignment(TextAlignment.CENTER)
            }

            return table.render()
        }


        private fun formatStatus(result: DownloadResult) : String {
            return when (result) {
                is SuccessResult -> result.status.name
                is FailedResult -> when(result.exception) {
                    is ConsoleDownloaderException -> "${result.status.name}. ${result.exception.message}"
                    else ->
                        "${result.status.name} (${result.exception.javaClass.simpleName} ${result.exception.message})"
                }
            }
        }

        private fun toTotalDownloadedBytesSize(downloadMeasuredResults: List<MeasuredResult<DownloadResult>>) =
            downloadMeasuredResults.map { it -> it.result.savedBytesCount }.sum()

    }
}