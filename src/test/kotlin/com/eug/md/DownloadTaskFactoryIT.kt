package com.eug.md

import io.github.glytching.junit.extension.folder.TemporaryFolder
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import java.nio.file.Paths

@ExtendWith(value = [
    TemporaryFolderExtension::class
])
class DownloadTaskFactoryIT {
    private lateinit var tempFolder: TemporaryFolder
    private val linksFileName = "links"

    @BeforeEach
    fun beforeEach(tempFolder: TemporaryFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `should throw parse exception when passed path to file which not exists`() {
        val notExistedFilePath = Paths.get("notExistedPath")
        val exception = assertThrows(LinksFileParseException::class.java) {
            DownloadTaskFactory.createTasks(notExistedFilePath)
        }
        assertEquals("Unable to read links cause no such file - \"$notExistedFilePath\"", exception.message)
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    fun `should throw parse exception when passed path to file which doesn't have read permission`() {
        val linksFile = tempFolder.createFile(linksFileName)
        linksFile.setReadable(false)
        val exception = assertThrows(LinksFileParseException::class.java) {
            DownloadTaskFactory.createTasks(linksFile.toPath())
        }
        assertEquals(
                "Unable to read links from file \"${linksFile.toPath()}\" cause access is denied",
                exception.message)
    }

    @Test
    fun `should return invalid row number when passed path to file which contains line with less tokens than need`() {
        val linksFile = tempFolder.createFile(linksFileName)
        val fileContent = """
            http://domain.net/file-name outFileName
            http://domain.net/file-name
        """.trimIndent()
        linksFile.writeText(fileContent)

        val (tasks, invalidRowNumbers, rowsCount) = DownloadTaskFactory.createTasks(linksFile.toPath())

        assertEquals(invalidRowNumbers.size, 1)
        assertEquals(invalidRowNumbers[0], 2)
        assertEquals(tasks.size, 1)
        assertEquals(rowsCount, 2)
    }

    @Test
    fun `should return invalid row number when passed path to file which contains line with invalid url`() {
        val linksFile = tempFolder.createFile(linksFileName)
        val fileContent = """
            invalidUrl outFileName
            http://domain.net/file-name outFileName
        """.trimIndent()
        linksFile.writeText(fileContent)

        val (tasks, invalidRowNumbers, rowsCount) = DownloadTaskFactory.createTasks(linksFile.toPath())

        assertEquals(invalidRowNumbers.size, 1)
        assertEquals(invalidRowNumbers[0], 1)
        assertEquals(tasks.size, 1)
        assertEquals(rowsCount, 2)
    }

    @Test
    fun `should throw parse exception when passed path to file which contains only invalid lines`() {
        val linksFile = tempFolder.createFile(linksFileName)
        val fileContent = """
            invalidUrl outFileName
            http://domain.net/file-name
        """.trimIndent()
        linksFile.writeText(fileContent)

        val exception = assertThrows(LinksFileParseException::class.java) {
            DownloadTaskFactory.createTasks(linksFile.toPath())
        }

        val expectedExceptionMessage = "Invalid links file - \"$linksFile\". Invalid format at rows - [1, 2]"
        assertEquals(expectedExceptionMessage, exception.message)
    }

    @Test
    fun `should throw parse exception when passed path to empty file`() {
        val linksFile = tempFolder.createFile(linksFileName)
        val exception = assertThrows(LinksFileParseException::class.java) {
            DownloadTaskFactory.createTasks(linksFile.toPath())
        }
        assertEquals("Unable to read links from file \"$linksFile\" cause file is empty", exception.message)
    }

    @Test
    fun `should return download tasks with single url-filename pairs when file without urls duplicates passed`() {
        val linksFile = tempFolder.createFile(linksFileName)

        val firstUrlToFileNamePair = Pair("http://domain1.net/file-name1", "outFileName1")
        val secondUrlToFileNamePair = Pair("http://domain2.net/file-name2", "outFileName2")
        val fileContent = """
            ${firstUrlToFileNamePair.first} ${firstUrlToFileNamePair.second}
            ${secondUrlToFileNamePair.first} ${secondUrlToFileNamePair.second}
        """.trimIndent()

        linksFile.writeText(fileContent)

        val (tasks, invalidRowNumbers, rowsCount) = DownloadTaskFactory.createTasks(linksFile.toPath())

        assertEquals(2, rowsCount)
        assertTrue(invalidRowNumbers.isEmpty())
        assertEquals(2, tasks.size)
        assertAll("first task",
                Executable { assertEquals(firstUrlToFileNamePair.first, tasks[0].url) },
                Executable { assertEquals(1, tasks[0].fileNames.size) },
                Executable { assertEquals(firstUrlToFileNamePair.second, tasks[0].fileNames[0]) }
        )
        assertAll("second task",
                Executable { assertEquals(secondUrlToFileNamePair.first, tasks[1].url) },
                Executable { assertEquals(1, tasks[1].fileNames.size) },
                Executable { assertEquals(secondUrlToFileNamePair.second, tasks[1].fileNames[0]) }
        )
    }

    @Test
    fun `should return download task with single url to multiple filenames when file with urls duplicates passed`() {
        val linksFile = tempFolder.createFile(linksFileName)

        val url = "http://domain.net/file-name"
        val fileName1 = "outFileName1"
        val fileName2 = "outFileName2"

        val fileContent = """
            $url $fileName1
            $url $fileName2
        """.trimIndent()

        linksFile.writeText(fileContent)

        val (tasks, invalidRowNumbers, rowsCount) = DownloadTaskFactory.createTasks(linksFile.toPath())

        assertEquals(2, rowsCount)
        assertTrue(invalidRowNumbers.isEmpty())
        assertEquals(1, tasks.size)
        assertAll(
                Executable { assertEquals(url, tasks[0].url) },
                Executable { assertEquals(2, tasks[0].fileNames.size) },
                Executable { assertEquals(fileName1, tasks[0].fileNames[0]) },
                Executable { assertEquals(fileName2, tasks[0].fileNames[1]) }
        )
    }

    @Test
    fun `should process file when links file has file name with whitespaces`() {
        val linksFile = tempFolder.createFile(linksFileName)

        val url = "http://domain.net/file-name"
        val fileName1 = "out File Name 1"

        val fileContent = "$url $fileName1"

        linksFile.writeText(fileContent)

        val (tasks, invalidRowNumbers, rowsCount) = DownloadTaskFactory.createTasks(linksFile.toPath())

        assertEquals(1, rowsCount)
        assertTrue(invalidRowNumbers.isEmpty())
        assertEquals(1, tasks.size)
        assertAll(
                Executable { assertEquals(url, tasks[0].url) },
                Executable { assertEquals(1, tasks[0].fileNames.size) },
                Executable { assertEquals(fileName1, tasks[0].fileNames[0]) }
        )
    }
}