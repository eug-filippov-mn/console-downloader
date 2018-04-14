package com.eug.md

import java.lang.RuntimeException
import java.nio.file.Path

sealed class ConsoleDownloaderException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

class NotValidOptionsException(msg: String, cause: Throwable? = null) : ConsoleDownloaderException(msg)

class SpeedLimitParseException private constructor(msg: String, cause: Throwable? = null)
    : ConsoleDownloaderException(msg, cause) {

    companion object {

        fun invalidSpeedMultiplier(speedMultiplier: Char) =
                SpeedLimitParseException("Unrecognized speed multiplier - $speedMultiplier")

        fun invalidSpeedFormat(cause: Throwable) =
                SpeedLimitParseException("Invalid speed format", cause)
    }
}

class OutDirCreationException(outDirPath: Path, cause: Throwable)
    : ConsoleDownloaderException("Unable to create out dir - $outDirPath", cause)

open class LinksFileParseException(msg: String, cause: Throwable? = null) : ConsoleDownloaderException(msg, cause) {
    companion object {

        fun noContent(linksFilePath: Path) =
                LinksFileParseException("Unable to read links from file \"$linksFilePath\" cause file is empty")

        fun noSuchFile(linksFilePath: Path, cause: Throwable) =
                LinksFileParseException("Unable to read links cause no such file - \"$linksFilePath\"", cause)

        fun accessDenied(linksFilePath: Path, cause: Throwable) =
                LinksFileParseException(
                        "Unable to read links from file \"$linksFilePath\" cause access is denied", cause)
    }
}

class InvalidLinksFileFormatException(linksFilePath: Path, rowNumbers: List<Int>) :
        LinksFileParseException("Invalid links file - \"$linksFilePath\". Invalid format at rows - $rowNumbers")

class UnexpectedResponseStatusException(statusCode: Int)
    : ConsoleDownloaderException("Unexpected response status - $statusCode")