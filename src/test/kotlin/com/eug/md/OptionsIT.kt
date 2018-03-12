package com.eug.md

import com.xenomachina.argparser.ArgParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OptionsIT {

    @Test
    fun `test`() {
        try {
            val args = arrayOf("-n", "-f")
            val options = Options(ArgParser(args))
            options.speedLimit
        val exception = Assertions.assertThrows(Exception::class.java) {

        }
        } catch (e: Exception) {
            println(e)
        }
    }
}