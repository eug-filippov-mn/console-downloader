package com.eug.md

import org.apache.commons.cli.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Opts {
    val threadsNumber: Option =
            Option.builder("n")
                    .longOpt("threads-number")
                    .argName("THREADS_NUMBER")
                    .desc("Number of threads for concurrent downloading")
                    .hasArg()
                    .required()
                    .build()

    val linksFilePath: Option =
            Option.builder("f")
                    .longOpt("links-file-path")
                    .argName("LINKS_FILE_PATH")
                    .desc("Path to file with links list")
                    .hasArg()
                    .required()
                    .build()

    val outputDirPath: Option =
            Option.builder("o")
                    .longOpt("--output-dir-path")
                    .argName("OUTPUT_DIR_PATH")
                    .desc("Path to dir for downloaded files")
                    .hasArg()
                    .required()
                    .build()

    val speedLimit: Option =
            Option.builder("l")
                    .longOpt("speed-limit")
                    .argName("SPEED_LIMIT")
                    .desc("Speed limit for all threads")
                    .hasArg()
                    .required()
                    .build()

    fun all(): Options  =
            Options()
                    .addOption(threadsNumber)
                    .addOption(linksFilePath)
                    .addOption(outputDirPath)
                    .addOption(speedLimit)
}

data class Settings(
        val threadsNumber: Int,
        val linksFilePath: Path,
        val outputDirPath: Path,
        val speedLimitInBytes: Double) {

    companion object {

        fun from(args: Array<String>): Settings {
            val commandLine: CommandLine
            try {
                commandLine = DefaultParser().parse(Opts.all(), args)
            } catch (e: ParseException) {
                throw NotValidOptionsException(e.message ?: "Options parse error", e)
            }

            return Settings(
                    threadsNumber = parseThreadNumber(commandLine.getOptionValue(Opts.threadsNumber.opt)),
                    linksFilePath = parseLinksFilePath(commandLine.getOptionValue(Opts.linksFilePath.opt)),
                    outputDirPath = parseOutputDirPath(commandLine.getOptionValue(Opts.outputDirPath.opt)),
                    speedLimitInBytes = parseSpeedLimit(commandLine.getOptionValue(Opts.speedLimit.opt))
            )
        }

        private fun parseThreadNumber(threadNumberArgValue: String): Int {
            try {
                val threadNumber = threadNumberArgValue.toInt()
                if (threadNumber <= 0) {
                    throw NotValidOptionsException("${Opts.threadsNumber.argName} must be a positive")
                }
                return threadNumber
            } catch (e: NumberFormatException) {
                throw NotValidOptionsException("${Opts.threadsNumber.argName} must be a number", cause = e)
            }
        }

        private fun parseLinksFilePath(linkFilePathArgValue: String): Path {
            val linksFilePath = Paths.get(linkFilePathArgValue)
            if (!Files.exists(linksFilePath)) {
                throw NotValidOptionsException("${Opts.linksFilePath.argName} is not exists")
            }
            if (Files.isDirectory(linksFilePath)) {
                throw NotValidOptionsException("${Opts.linksFilePath.argName} must be a file, not a directory")
            }
            if (!Files.isReadable(linksFilePath)) {
                throw NotValidOptionsException("${Opts.linksFilePath.argName} isn't readable")
            }
            return linksFilePath
        }

        private fun parseOutputDirPath(outputDirPathArgValue: String): Path {
            val outDirPath = Paths.get(outputDirPathArgValue)

            if (Files.exists(outDirPath)) {
                if (!Files.isWritable(outDirPath)) {
                    throw NotValidOptionsException("${Opts.outputDirPath.argName} isn't writable")
                }
                if (!Files.isDirectory(outDirPath)) {
                    throw NotValidOptionsException("${Opts.outputDirPath.argName} must be a directory")
                }
            }
            return outDirPath
        }

        private fun parseSpeedLimit(speedLimitArgValue: String): Double =
                try {
                    SpeedLimitParser.parseAsBytes(speedLimitArgValue)
                } catch (e: SpeedLimitParseException) {
                    throw NotValidOptionsException("Invalid ${Opts.speedLimit.argName}. ${e.message}", e)
                }
    }
}