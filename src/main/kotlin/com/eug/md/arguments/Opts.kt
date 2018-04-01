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
                    .desc("Path to file with links list. Valid file format is <HTTP link><whitespace><file name>")
                    .hasArg()
                    .required()
                    .build()

    val outputDirPath: Option =
            Option.builder("o")
                    .longOpt("output-dir-path")
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

    val interactiveMode: Option =
            Option.builder("i")
                    .longOpt("interactive-mode")
                    .hasArg(false)
                    .desc("If passed, app shows continue dialog when there are invalid rows in links file")
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