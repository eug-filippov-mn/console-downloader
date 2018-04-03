package com.eug.md

import com.eug.md.settings.SpeedLimitParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SpeedLimitParserTest {

    @Test
    fun `should throw a parse exception when not a number passed`() {
        assertThrows(SpeedLimitParseException::class.java) {
            SpeedLimitParser.parseAsBytes("notANumberValue")
        }
    }

    @Test
    fun `should throw a parse exception when number with suffix differs from "k" or "m" passed`() {
        assertThrows(SpeedLimitParseException::class.java) {
            SpeedLimitParser.parseAsBytes("10Mb")
        }
    }

    @Test
    fun `should convert number to double when number without suffixes passed`() {
        assertEquals(SpeedLimitParser.parseAsBytes("10"), 10.0)
    }

    @Test
    fun `should convert number to double multiply it by 1024 when number with "k" suffix passed`() {
        assertEquals(SpeedLimitParser.parseAsBytes("10k"), 10.0 * 1024)
    }

    @Test
    fun `should convert number to double multiply it by 1024 * 1024 when number with "m" suffix passed`() {
        assertEquals(SpeedLimitParser.parseAsBytes("10m"), 10.0 * 1024 * 1024)
    }
}