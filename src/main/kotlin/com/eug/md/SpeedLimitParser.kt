package com.eug.md

object SpeedLimitParser {
    private const val BYTE_SPEED_MULTIPLIER = 1
    private const val KILO_BYTE_SPEED_MULTIPLIER = 1024
    private const val MEGA_BYTE_SPEED_MULTIPLIER = 1024 * 1024

    fun parseAsBytes(speed: String): Double {
        try {
            val speedNumber = parseSpeedNumber(speed)
            val speedMultiplier = parseSpeedMultiplier(speed)
            return speedNumber * speedMultiplier

        } catch (e: NumberFormatException) {
            throw SpeedLimitParseException(speed, cause = e)
        }
    }

    private fun parseSpeedMultiplier(speed: String): Int {
        val lastCharacter = speed.last()
        return when {
            lastCharacter.isDigit() -> BYTE_SPEED_MULTIPLIER
            lastCharacter == 'k' -> KILO_BYTE_SPEED_MULTIPLIER
            lastCharacter == 'm' -> MEGA_BYTE_SPEED_MULTIPLIER
            else -> throw SpeedLimitParseException(speed)
        }
    }

    private fun parseSpeedNumber(speed: String): Double =
            if (speed.last().isDigit()) {
                speed.toDouble()
            } else {
                speed.dropLast(1).toDouble()
            }

}