package com.eug.md

import com.xenomachina.argparser.ArgParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Options(parser: ArgParser) {

    val threadsNumbers: Int by parser
            .storing("-n", "--threads-number", help = "Number of threads for concurrent downloading") {
                this.toInt()
            }
            .addValidator {
                if (value < 0) {
                    throw NotValidOptionException("${this.errorName} can't be negative or zero", value)
                }
            }

    val linksFilePath: Path by parser
            .storing("-f", "--links-file-path", help = "Path to file with links list") {
                Paths.get(this)
            }
            .addValidator {
                if (Files.isDirectory(value)) {
                    throw NotValidOptionException("${this.errorName} point to directory", value)
                }
                if (!Files.isReadable(value)) {
                    throw NotValidOptionException("${this.errorName} hasn't read permission", value)
                }
            }


    val outputDirPath: Path by parser
            .storing("-o", "--output-dir-path", help = "Path to dir for downloaded files") {
                Paths.get(this)
            }
            .addValidator {
                if (!Files.exists(value)) {
                    return@addValidator
                }

                if (!Files.isDirectory(value)) {
                    throw NotValidOptionException("${this.errorName} must be a directory", value)
                }
                if (!Files.isWritable(value)) {
                    throw NotValidOptionException("${this.errorName} hasn't write permission", value)
                }
            }

    val speedLimit: Double by parser
            .storing("-l", "--speed-limit", help = "Speed limit for all threads") {
                SpeedLimitParser.parseAsBytes(this)
            }

    override fun toString(): String {
        return "Options(" +
                "threadNumbers = $threadsNumbers " +
                "linksFilePath = $linksFilePath " +
                "outputDirPath = $outputDirPath " +
                "speedLimit = $speedLimit)"
    }
}