package com.eug.md

import com.eug.md.arguments.Opts
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.format.*
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.apache.commons.cli.HelpFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.text.NumberFormat
import java.util.*


class Terminal {
    private val scanner  = Scanner(System.`in`)
    private val out: PrintStream = System.out
    private val err: PrintStream = System.err
    private val helpFormatter = HelpFormatter()

    tailrec fun yesNoDialog(dialogMessage: String, yesAnswer: String, noAnswer: String): Boolean {
        message("$dialogMessage $yesAnswer/$noAnswer")

        val confirmLine = scanner.nextLine().trim().toLowerCase()
        return when(confirmLine) {
            yesAnswer.toLowerCase() -> true
            noAnswer.toLowerCase() -> false
            else -> {
                message("Invalid input")
                yesNoDialog(dialogMessage, yesAnswer, noAnswer)
            }
        }
    }

    fun statusLine(message: String) {
        val formattedMessage = "\r> ${message.padEnd(STATUS_LINE_LENGTH)}"
        if (log.isDebugEnabled) {
            log.debug(formattedMessage)
        } else {
            out.print(formattedMessage)
        }
    }

    fun printResults(results: MeasuredResult<List<MeasuredResult<DownloadResult>>>) {
        out.print("\r")
        out.println(ToTableFormatter.format(results))
    }

    fun printHelp() {
        helpFormatter.printHelp("console-downloader [OPTION]...", Opts.all())
    }

    fun printError(message: String?) {
        val messageToPrint = message ?: "Unexpected error"

        if (log.isDebugEnabled) {
            log.debug(messageToPrint)
        } else {
            err.print("\r")
            err.println(messageToPrint)
        }
    }

    fun message(message: String) {
        if (log.isDebugEnabled) {
            log.debug(message)
        } else {
            out.print("\r")
            out.println(message)
        }
    }

    private object ToTableFormatter {
        private val HEADERS = arrayOf("#", "Url", "Status", "Downloaded bytes", "Execution time, ms")
        private val numberFormat = NumberFormat.getInstance()

        fun format(result: MeasuredResult<List<MeasuredResult<DownloadResult>>>) : String {
            val (totalExecutionTime, downloadMeasuredResults) = result

            val table = AsciiTable().apply {
                addHeaders(HEADERS)
                addRows(downloadMeasuredResults.sortedBy { it.result.taskNumber }, columns = { measuredResult ->
                    arrayOf(
                            Column(measuredResult.result.taskNumber),
                            Column(measuredResult.result.url),
                            Column(formatStatus(measuredResult.result)),
                            Column(numberFormat.format(measuredResult.result.savedBytesCount)),
                            Column(numberFormat.format(measuredResult.executionTime))
                    )
                })

                addTotal(
                        Column("Total downloaded bytes", colspan = 3),
                        Column(numberFormat.format(toTotalDownloadedBytesSize(downloadMeasuredResults)), colspan = 2)
                )
                addTotal(
                        Column("Total execution time, ms", colspan = 3),
                        Column(numberFormat.format(totalExecutionTime), colspan = 2)
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

    companion object {
        private const val STATUS_LINE_LENGTH = 100
        private val log: Logger = LoggerFactory.getLogger(Terminal::class.java)
    }
}