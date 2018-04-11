package com.eug.md.arguments

import org.apache.commons.cli.*

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
                    .desc(
                            "Path to file with links list. "
                            + "Valid file's line format is <HTTP|HTTPS link>whitespace<file name>"
                    )
                    .hasArg()
                    .required()
                    .build()

    val outputDirPath: Option =
            Option.builder("o")
                    .longOpt("output-dir-path")
                    .argName("OUTPUT_DIR_PATH")
                    .desc("Path to dir where downloaded files will be saved")
                    .hasArg()
                    .required()
                    .build()

    val speedLimit: Option =
            Option.builder("l")
                    .longOpt("speed-limit")
                    .argName("SPEED_LIMIT")
                    .desc(
                            "Download speed limit for all threads. Speed value can be used with suffixes - k,m "
                            + "to set speed in kilobytes or megabytes respectively. If suffix is not passed,"
                            + " value treated as speed in bytes. Speed can be passed as integer or as double"
                    )
                    .hasArg()
                    .required()
                    .build()

    val interactiveMode: Option =
            Option.builder("i")
                    .longOpt("interactive-mode")
                    .hasArg(false)
                    .desc(
                            "If passed, instead of exit with error, app shows continue dialog when there are "
                            + "invalid rows in links file. If file contains more than 100 rows, invalid rows' "
                            + "numbers will be written to ~/console-downloader.invalid-rows-report"
                    )
                    .build()

    val help: Option =
            Option.builder("h")
                    .longOpt("help")
                    .desc("Print help information")
                    .build()

    fun all(): Options =
            Options()
                    .addOption(threadsNumber)
                    .addOption(linksFilePath)
                    .addOption(outputDirPath)
                    .addOption(speedLimit)
                    .addOption(interactiveMode)
                    .addOption(help)

    fun allExceptHelp(): Options =
            Options()
                    .addOption(threadsNumber)
                    .addOption(linksFilePath)
                    .addOption(outputDirPath)
                    .addOption(speedLimit)
                    .addOption(interactiveMode)

    fun help(): Options = Options().addOption(help)

}