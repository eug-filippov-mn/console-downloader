package com.eug.md

import com.eug.md.arguments.Opts
import com.eug.md.utils.getOptionValue
import com.eug.md.utils.hasOption
import org.apache.commons.cli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class Settings(
        val threadsNumber: Int,
        val linksFilePath: Path,
        val outputDirPath: Path,
        val speedLimitInBytes: Double,
        val interactiveMode: Boolean) {

    companion object {

        fun from(commandLine: CommandLine): Settings {
            return Settings(
                    threadsNumber = parseThreadNumber(commandLine.getOptionValue(Opts.threadsNumber)),
                    linksFilePath = parseLinksFilePath(commandLine.getOptionValue(Opts.linksFilePath)),
                    outputDirPath = parseOutputDirPath(commandLine.getOptionValue(Opts.outputDirPath)),
                    speedLimitInBytes = parseSpeedLimit(commandLine.getOptionValue(Opts.speedLimit)),
                    interactiveMode = commandLine.hasOption(Opts.interactiveMode)
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