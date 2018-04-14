package com.eug.md

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import org.apache.commons.validator.routines.UrlValidator
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*


data class DownloadTask(val number: Int, val url: String, val fileNames: List<String>)

data class TasksCreationResult(val tasks: List<DownloadTask>, val invalidRowNumbers: List<Int>, val rowsCount: Int)

object DownloadTaskFactory {
    private const val DEFAULT_DELIMITER = " "
    private const val TOKENS_NUMBER_PER_ROW = 2

    private val urlValidator = UrlValidator(arrayOf("http","https"))

    fun createTasks(linksFilePath: Path) : TasksCreationResult {
        try {

            val (urlsToFileNames, invalidFileRowNumbers, rowsCount) = processFile(linksFilePath)

            val tasks = urlsToFileNames.asMap().entries.mapIndexed { index, urlToFileNames ->
                DownloadTask(
                        number = index + 1,
                        url = urlToFileNames.key,
                        fileNames = urlToFileNames.value.toMutableList()
                )
            }

            return TasksCreationResult(tasks, invalidFileRowNumbers, rowsCount)

        } catch (e: AccessDeniedException) {
            throw LinksFileParseException.accessDenied(linksFilePath, cause = e)
        } catch (e: NoSuchFileException) {
            throw LinksFileParseException.noSuchFile(linksFilePath, cause = e)
        } catch (e: IOException) {
            throw LinksFileParseException("Unable to read links file cause: ${e.message}", e)
        }
    }

    private fun processFile(linksFilePath: Path): Triple<Multimap<String, String>, List<Int>, Int> {
        val reader = Files.newBufferedReader(linksFilePath, Charsets.UTF_8)
        val urlsToFileNames = LinkedListMultimap.create<String, String>()
        val invalidRowNumbers = LinkedList<Int>()
        var rowsCount = 0

        reader.useLines { lines ->
            lines.forEachIndexed { index, line ->

                if (line.trim().isBlank()) {
                    return@forEachIndexed
                }

                rowsCount++
                val lineTokens = line.split(DEFAULT_DELIMITER, limit = 2)
                if (isLineTokensNotValid(lineTokens)) {
                    invalidRowNumbers += (index + 1)
                    return@forEachIndexed
                }

                urlsToFileNames.put(lineTokens.component1(), lineTokens.component2())
            }
        }

        if (urlsToFileNames.isEmpty) {
            if (invalidRowNumbers.isNotEmpty()) {
                throw InvalidLinksFileFormatException(linksFilePath, invalidRowNumbers)
            }
            throw LinksFileParseException.noContent(linksFilePath)
        }
        return Triple(urlsToFileNames, invalidRowNumbers, rowsCount)
    }

    private fun isLineTokensNotValid(lineTokens: List<String>): Boolean {
        val url = lineTokens.component1()
        return lineTokens.size != TOKENS_NUMBER_PER_ROW
                || !urlValidator.isValid(url)
    }
}