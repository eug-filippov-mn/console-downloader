package com.eug.md

object SpeedLimitParser {
    private const val BYTE_SPEED_MULTIPLIER = 1
    private const val KILO_BYTE_SPEED_MULTIPLIER = 1024
    private const val MEGA_BYTE_SPEED_MULTIPLIER = 1024 * 1024

    fun parseAsBytes(speed: String): Double {
        try {
            val speedNumber = parseAsSpeedNumber(speed)
            val speedMultiplier = parseAsSpeedMultiplier(speed)
            return speedNumber * speedMultiplier

        } catch (e: NumberFormatException) {
            throw SpeedLimitParseException.invalidSpeedFormat(cause = e)
        }
    }

    private fun parseAsSpeedMultiplier(speed: String): Int {
        val lastCharacter = speed.last()
        return when {
            lastCharacter.isDigit() -> BYTE_SPEED_MULTIPLIER
            lastCharacter == 'k' -> KILO_BYTE_SPEED_MULTIPLIER
            lastCharacter == 'm' -> MEGA_BYTE_SPEED_MULTIPLIER
            else -> throw SpeedLimitParseException.invalidSpeedMultiplier(lastCharacter)
        }
    }

    private fun parseAsSpeedNumber(speed: String): Double =
            if (speed.last().isDigit()) {
                speed.toDouble()
            } else {
                speed.dropLast(1).toDouble()
            }

}