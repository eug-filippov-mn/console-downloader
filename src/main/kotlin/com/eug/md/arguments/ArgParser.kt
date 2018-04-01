package com.eug.md.arguments

import com.eug.md.NotValidOptionsException
import com.eug.md.utils.hasOption
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.ParseException

object ArgParser {

    fun containsHelp(args: Array<String>): Boolean =
            handleParseErrors {
                val commandLine= DefaultParser().parse(Opts.help(), args, true)
                commandLine.hasOption(Opts.help)
            }

    fun parse(args: Array<String>): CommandLine =
            handleParseErrors {
                DefaultParser().parse(Opts.allExceptHelp(), args)
            }

    private inline fun <T> handleParseErrors(block: () -> T): T {
        try {
            return block()
        } catch (e: ParseException) {
            throw NotValidOptionsException(e.message ?: "Options parse error", e)
        }
    }
}