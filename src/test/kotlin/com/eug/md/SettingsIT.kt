package com.eug.md

import com.eug.md.arguments.ArgParser
import com.eug.md.settings.Settings
import io.github.glytching.junit.extension.folder.TemporaryFolder
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files

@ExtendWith(value = [TemporaryFolderExtension::class])
class SettingsIT {
    companion object {
        private const val DEFAULT_SPEED = "100m"
        private const val DEFAULT_THREADS_NUMBER = "10"
    }

    private lateinit var tempFolder: TemporaryFolder
    private lateinit var outDir: File
    private lateinit var linksFile: File

    @BeforeEach
    fun beforeEach(tempFolder: TemporaryFolder) {
        this.tempFolder = tempFolder
        this.outDir = tempFolder.createDirectory("out")
        this.linksFile = tempFolder.createFile("links-file")
    }

    @Test
    fun `setting creation should throw exception with missing required arguments msg when arguments is empty`() {
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(emptyArray()))
        }
        assertEquals(
                "Missing required options: n, f, o, l. Run app with --help argument to print help information",
                notValidOptionsException.message
        )
    }

    @Test
    fun `setting creation from arguments where thread number is not a number should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", "notANumber",
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("THREADS_NUMBER must be a number", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where thread number is negative should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", "-10",
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("THREADS_NUMBER must be a positive", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where thread number is zero should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", "0",
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("THREADS_NUMBER must be a positive", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where links file does not exist should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", "notExistedFile",
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("LINKS_FILE_PATH is not exists", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where links file is directory should throw exception with valid msg`() {
        val linksFileAsDirectory = tempFolder.createDirectory("does-not-matter")
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFileAsDirectory.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("LINKS_FILE_PATH must be a file, not a directory", notValidOptionsException.message)
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    fun `setting creation from arguments where links file is not readable should throw exception with valid msg`() {
        linksFile.setReadable(false)
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("LINKS_FILE_PATH isn't readable", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where out dir is file should throw exception with valid msg`() {
        val outDirAsFile = tempFolder.createFile("does-not-matter")
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDirAsFile.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("OUTPUT_DIR_PATH must be a directory", notValidOptionsException.message)
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    fun `setting creation from arguments where out dir hasn't write permissions should throw exception with valid msg`() {
        outDir.setWritable(false)
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("OUTPUT_DIR_PATH isn't writable", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where out dir does not exists should create settings`() {
        val pathToNotExistedDir =
                tempFolder.createDirectory("directory").toPath().resolve("notExistedDir")
        Assumptions.assumeFalse(Files.exists(pathToNotExistedDir))

        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", pathToNotExistedDir.toAbsolutePath().toString(),
                "-l", DEFAULT_SPEED
        )

        val settings = Settings.from(ArgParser.parse(arguments))

        assertEquals(DEFAULT_THREADS_NUMBER.toInt(), settings.threadsNumber)
        assertEquals(linksFile.toPath(), settings.linksFilePath)
        assertEquals(pathToNotExistedDir, settings.outputDirPath)
        assertEquals(defaultSpeedInBytes(), settings.speedLimitInBytes)
    }


    @Test
    fun `setting creation from arguments where speed not a number should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", "speed"
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("Invalid SPEED_LIMIT. Invalid speed format", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from arguments where speed multiplier is not k or m should throw exception with valid msg`() {
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", "1000g"
        )
        val notValidOptionsException = Assertions.assertThrows(NotValidOptionsException::class.java) {
            Settings.from(ArgParser.parse(arguments))
        }
        assertEquals("Invalid SPEED_LIMIT. Unrecognized speed multiplier - g", notValidOptionsException.message)
    }

    @Test
    fun `setting creation from valid arguments should create settings properly`() {
        val arguments = arrayOf(
                "-n", DEFAULT_THREADS_NUMBER,
                "-f", linksFile.absolutePath,
                "-o", outDir.absolutePath,
                "-l", DEFAULT_SPEED
        )

        val settings = Settings.from(ArgParser.parse(arguments))

        assertEquals(DEFAULT_THREADS_NUMBER.toInt(), settings.threadsNumber)
        assertEquals(linksFile.toPath(), settings.linksFilePath)
        assertEquals(outDir.toPath(), settings.outputDirPath)
        assertEquals(defaultSpeedInBytes(), settings.speedLimitInBytes)
    }

    private fun defaultSpeedInBytes() = DEFAULT_SPEED.dropLast(1).toDouble() * 1024 * 1024
}