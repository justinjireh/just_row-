package com.listinglab.rowplus.telemetry

/**
 * Parses ASCII lines from the Hydrow serial protocol into typed packets.
 *
 * Wire format (CR/LF-terminated ASCII, space-delimited):
 *   Di2 <distance> <handleForce> <handlePosition> <power> <sequence>
 *   Ds2 <avgPower> <distance> <endHandle> <startHandle> <eodSeq> <eorSeq> <seq>
 *   Rv F<fw>H<hw>
 *   Rs <serial>
 *   Rl <dragFactor>
 *   Rm <mode>
 */
object PacketParser {

    sealed class ParsedPacket {
        data class Interval(val packet: Di2Packet) : ParsedPacket()
        data class Stroke(val packet: Ds2Packet) : ParsedPacket()
        data class ResistanceLevel(val dragFactor: Int) : ParsedPacket()
        data class Version(val firmware: String, val hardware: String) : ParsedPacket()
        data class SerialNumber(val serial: String) : ParsedPacket()
        data class ModeConfirm(val mode: Int) : ParsedPacket()
        data class Comment(val text: String) : ParsedPacket()
        data class Unknown(val line: String) : ParsedPacket()
    }

    /**
     * Parse a single line from the serial stream or log file.
     *
     * Log file lines have a leading timestamp: "<epoch_ms> Di2 ..."
     * Raw serial lines have no timestamp: "Di2 ..."
     */
    fun parseLine(line: String): ParsedPacket? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        // Log-format comment lines
        if (trimmed.contains("##") || trimmed.contains("# Workout")) {
            return ParsedPacket.Comment(trimmed)
        }

        // Outgoing commands in log files (prefixed with >)
        if (trimmed.contains(">")) return null

        val parts = trimmed.split(" ")

        // Determine if first token is a timestamp (all digits)
        val hasTimestamp = parts.isNotEmpty() && parts[0].all { it.isDigit() }
        val offset = if (hasTimestamp) 1 else 0
        val timestamp = if (hasTimestamp) parts[0].toLongOrNull() ?: 0L else System.currentTimeMillis()

        if (parts.size <= offset) return null
        val prefix = parts[offset]

        return try {
            when (prefix) {
                "Di2" -> parseDi2(parts, offset, timestamp)
                "Ds2" -> parseDs2(parts, offset, timestamp)
                "Rl" -> parseRl(parts, offset)
                "Rv" -> parseRv(parts, offset)
                "Rs" -> parseRs(parts, offset)
                "Rm" -> parseRm(parts, offset)
                else -> ParsedPacket.Unknown(trimmed)
            }
        } catch (e: Exception) {
            ParsedPacket.Unknown(trimmed)
        }
    }

    private fun parseDi2(parts: List<String>, offset: Int, timestamp: Long): ParsedPacket.Interval {
        // Di2 <distance> <handleForce> <handlePosition> <power> <sequence>
        return ParsedPacket.Interval(
            Di2Packet(
                timestamp = timestamp,
                distance = parts[offset + 1].toDouble(),
                handleForce = parts[offset + 2].toDouble(),
                handlePosition = parts[offset + 3].toDouble(),
                power = parts[offset + 4].toDouble(),
                sequence = parts[offset + 5].toInt(),
            )
        )
    }

    private fun parseDs2(parts: List<String>, offset: Int, timestamp: Long): ParsedPacket.Stroke {
        // Ds2 <avgPower> <distance> <endHandle> <startHandle> <eodSeq> <eorSeq> <seq>
        return ParsedPacket.Stroke(
            Ds2Packet(
                timestamp = timestamp,
                averagePower = parts[offset + 1].toDouble(),
                distance = parts[offset + 2].toDouble(),
                endHandlePosition = parts[offset + 3].toDouble(),
                startHandlePosition = parts[offset + 4].toDouble(),
                endOfDriveSequence = parts[offset + 5].toInt(),
                endOfRecoverySequence = parts[offset + 6].toInt(),
                sequence = parts[offset + 7].toInt(),
            )
        )
    }

    private fun parseRl(parts: List<String>, offset: Int): ParsedPacket.ResistanceLevel {
        return ParsedPacket.ResistanceLevel(parts[offset + 1].toInt())
    }

    private fun parseRv(parts: List<String>, offset: Int): ParsedPacket.Version {
        val v = parts[offset + 1]
        val fIdx = v.indexOf('F')
        val hIdx = v.indexOf('H')
        return ParsedPacket.Version(
            firmware = if (fIdx >= 0 && hIdx > fIdx) v.substring(fIdx + 1, hIdx) else v,
            hardware = if (hIdx >= 0) v.substring(hIdx + 1) else "",
        )
    }

    private fun parseRs(parts: List<String>, offset: Int): ParsedPacket.SerialNumber {
        return ParsedPacket.SerialNumber(parts[offset + 1])
    }

    private fun parseRm(parts: List<String>, offset: Int): ParsedPacket.ModeConfirm {
        return ParsedPacket.ModeConfirm(parts[offset + 1].toInt())
    }
}
