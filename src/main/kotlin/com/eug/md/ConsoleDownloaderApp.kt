package com.eug.md

import com.eug.md.arguments.ArgParser
import com.eug.md.settings.Settings
import com.eug.md.utils.MeasuredResult
import com.eug.md.utils.measureExecTime
import com.google.common.base.StandardSystemProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    ConsoleDownloaderApp.run(args)
}

class ConsoleDownloaderApp private constructor(private val settings: Settings) : Closeable  {
    private val tasksLauncher = TasksLauncher(settings)

    fun run() {
        terminal.statusLine("Processing file with links")
        val tasksCreationResult = DownloadTaskFactory.createTasks(settings.linksFilePath)
        validateTasksCreationResult(tasksCreationResult)

        terminal.statusLine("Starting downloading")
        val measuredDownloadingResults = measureExecTime { downloadFiles(tasksCreationResult.tasks) }

        terminal.statusLine("Downloading finished")
        terminal.printResults(measuredDownloadingResults)
    }

    private fun downloadFiles(tasks: List<DownloadTask>): List<MeasuredResult<DownloadResult>> {
        tasksLauncher.startDownloading(tasks)

        val completedTasks = mutableListOf<MeasuredResult<DownloadResult>>()
        var completedTasksCount = 0

        while (completedTasksCount != tasks.size && !tasksLauncher.stopped) {
            completedTasks += tasksLauncher.takeNextCompleted()

            completedTasksCount++
            terminal.statusLine("$completedTasksCount / ${tasks.size} downloads completed")
        }
        return completedTasks
    }

    private fun validateTasksCreationResult(tasksCreationResult: TasksCreationResult) {
        if (tasksCreationResult.invalidRowNumbers.isNotEmpty()) {
            if (settings.interactiveMode) {
                printInvalidRowsMessage(settings.linksFilePath, tasksCreationResult)

                val continueProcessing = terminal.yesNoDialog(
                        dialogMessage = "Continue processing?", yesAnswer = "y", noAnswer = "n"
                )

                if (!continueProcessing) {
                    exitProcess(0)
                }
            } else {
                throw InvalidLinksFileFormatException(settings.linksFilePath, tasksCreationResult.invalidRowNumbers)
            }
        }
    }

    override fun close() {
        log.debug("Closing app...")
        tasksLauncher.stop()
        log.debug("App closed")
    }

    companion object {
        private const val INVALID_ROW_NUMBERS_LIMIT_TO_PRINT_TO_TERMINAL = 100

        private val INVALID_ROW_NUMBERS_FILE_PATH =
                Paths.get(StandardSystemProperty.USER_HOME.value())
                        .resolve("console-downloader.invalid-rows-report")

        private val log: Logger = LoggerFactory.getLogger(ConsoleDownloaderApp::class.java)

        private val terminal: Terminal = Terminal()

        fun run(args: Array<String>) {
            try {

                if (ArgParser.containsHelp(args)) {
                    terminal.printHelp()
                    exitProcess(0)
                }

                terminal.statusLine("Parsings arguments")
                val commandLine = ArgParser.parse(args)
                val settings = Settings.from(commandLine)
                log.debug("App settings from parsed args {}", settings)

                createOutDirIfNotExists(settings.outputDirPath)

                terminal.statusLine("Starting application")
                ConsoleDownloaderApp(settings).use { it.run() }

            } catch (e: Throwable) {
                terminal.printError(e.message)
                log.error(e.message, e)
                exitProcess(-1)
            }
        }

        private fun createOutDirIfNotExists(outDirPath: Path) {
            try {
                if (!Files.exists(outDirPath)) {
                    terminal.statusLine("Creating directories for out path")
                    Files.createDirectories(outDirPath)
                }
            } catch (e: IOException) {
                throw OutDirCreationException(outDirPath, e)
            }
        }

        private fun printInvalidRowsMessage(linksFilePath: Path, tasksCreationResult: TasksCreationResult) {
            val (tasks, invalidRowNumbers, rowsCount) = tasksCreationResult

            var rowsWrittenToFile = false
            if (invalidRowNumbers.size > INVALID_ROW_NUMBERS_LIMIT_TO_PRINT_TO_TERMINAL) {
                rowsWrittenToFile = writeInvalidRowsToFile(invalidRowNumbers)
            }

            if (rowsWrittenToFile) {
                terminal.message(
                        """Invalid links file - "$linksFilePath"
                            |${invalidRowNumbers.size} invalid rows detected
                            |Invalid rows' numbers saved to $INVALID_ROW_NUMBERS_FILE_PATH
                            |""".trimMargin()
                )
            } else {
                terminal.message("Invalid links file - \"$linksFilePath\". Invalid format at rows - $invalidRowNumbers")
            }

            terminal.message("${tasks.size} out of $rowsCount rows will be processed.")
        }

        private fun writeInvalidRowsToFile(invalidRowNumbers: List<Int>): Boolean {
            var rowsWrittenToFile = false
            try {
                val fileContent = invalidRowNumbers.joinToString(",")
                INVALID_ROW_NUMBERS_FILE_PATH.toFile().writeText(fileContent)
                rowsWrittenToFile = true
            } catch (e: IOException) {
                log.debug("Exception while writing invalid rows to file", e)
            }

            return rowsWrittenToFile
        }
    }
}
