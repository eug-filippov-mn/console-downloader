package com.eug.md

import java.lang.RuntimeException
import java.nio.file.Path

sealed class ConsoleDownloaderException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

class NotValidOptionException(msg: String, value: Any) : ConsoleDownloaderException(msg)

open class LinksFileParseException(msg: String, cause: Throwable? = null) : ConsoleDownloaderException(msg, cause) {
    companion object {

        fun invalidFormatAtLine(linksFilePath: Path, rowNumbers: List<Int>) =
                LinksFileParseException("Invalid links file - \"$linksFilePath\". Invalid format at" +
                        " rows - $rowNumbers. Valid file format is <HTTP link><whitespace><file name>")

        fun noContent(linksFilePath: Path) =
                LinksFileParseException("Unable to read links from file \"$linksFilePath\" cause file is empty")

        fun noSuchFile(linksFilePath: Path, cause: Throwable) =
                LinksFileParseException("Unable to read links cause no such file - \"$linksFilePath\"", cause)

        fun accessDenied(linksFilePath: Path, cause: Throwable) =
                LinksFileParseException(
                        "Unable to read links from file \"$linksFilePath\" cause access is denied", cause)
    }
}

class SpeedLimitParseException(value: String, cause: Throwable? = null)
    : ConsoleDownloaderException("Unable to parse passed speed limit - \"$value\"", cause)

class UnexpectedResponseStatusException(status: Int)
    : ConsoleDownloaderException("Unexpected response status - $status")