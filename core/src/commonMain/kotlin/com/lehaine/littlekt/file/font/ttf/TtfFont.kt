package com.lehaine.littlekt.file.font.ttf

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.tabke.Cmap
import com.lehaine.littlekt.file.font.ttf.tabke.CmapParser
import com.lehaine.littlekt.file.font.ttf.tabke.Head
import com.lehaine.littlekt.file.font.ttf.tabke.HeadParser

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class TtfFont(buffer: MixedBuffer? = null) {

    init {
        buffer?.let { parse(it) }
    }

    private var outlinesFormat: String = ""
    private val tables = Tables()
    private var encoding: Encoding = DefaultEncoding(this)
    private var unitsPerEm: Int = 0
    internal val glyphs: List<Glyph> = listOf()

    private fun parse(buffer: MixedBuffer) {
        val numTables: Int
        val tableEntries: List<TableEntry>
        val signature = buffer.getTag(0)
        if (signature == charArrayOf(
                0.toChar(),
                1.toChar(),
                0.toChar(),
                0.toChar()
            ).concatToString() || signature == "true" || signature == "typ1"
        ) {
            outlinesFormat = "truetype"
            numTables = buffer.getUint16(4).toInt()
            tableEntries = parseOpenTypeTableEntries(buffer, numTables)
        } else if (signature == "OTTO") {
            outlinesFormat = "cff"
            numTables = buffer.getUint16(4).toInt()
            tableEntries = parseOpenTypeTableEntries(buffer, numTables)
        } else if (signature === "wOFF") {
            val flavor = buffer.getTag(4)
            outlinesFormat = when {
                flavor == charArrayOf(
                    0.toChar(),
                    1.toChar(),
                    0.toChar(),
                    0.toChar()
                ).concatToString() -> {
                    "truetype"
                }
                flavor === "OTTO" -> {
                    "cff"
                }
                else -> {
                    throw IllegalStateException("Unsupported OpenType flavor: $flavor")
                }
            }

            numTables = buffer.getUint16(12).toInt()
            tableEntries = parseWOFFTableEntries(buffer, numTables)
        } else {
            throw IllegalStateException("Unsupported OpenType signature: $signature")
        }

        var indexToLocFormat: Int
        var ltagTable: Int

        var cffTableEntry: TableEntry
        var fvarTableEntry: TableEntry
        var glyfTableEntry: TableEntry
        var gdefTableEntry: TableEntry
        var gposTableEntry: TableEntry
        var gsubTableEntry: TableEntry
        var hmtxTableEntry: TableEntry
        var kernTableEntry: TableEntry
        var locaTableEntry: TableEntry
        var nameTableEntry: TableEntry
        var metaTableEntry: TableEntry
        var parser: Parser

        tableEntries.forEach { tableEntry ->
            var table: Table
            when (tableEntry.tag) {
                "cmap" -> {
                    table = uncompressTable(buffer, tableEntry)
                    val cmap = CmapParser(table.buffer, table.offset).parse().also { tables.cmap = it }
                    encoding = CmapEncoding(cmap)
                }
                "cvt" -> {
                    table = uncompressTable(buffer, tableEntry)
                    parser = Parser(table.buffer, table.offset)
                    tables.cvt = parser.parseInt16List(tableEntry.length / 2)
                }
                "fvar" -> {
                    fvarTableEntry = tableEntry
                }
                "fpgm" -> {
                    table = uncompressTable(buffer, tableEntry)
                    parser = Parser(table.buffer, table.offset)
                    tables.fpgm = parser.parseByteList(tableEntry.length)
                }
                "head" -> {
                    table = uncompressTable(buffer, tableEntry)
                    tables.head = HeadParser(table.buffer, table.offset).parse().also {
                        unitsPerEm = it.unitsPerEm
                        indexToLocFormat = it.indexToLocFormat
                    }
                }
            }
        }
    }

    private fun parseOpenTypeTableEntries(buffer: MixedBuffer, numTables: Int): List<TableEntry> {
        TODO("Not yet implemented")
    }

    private fun parseWOFFTableEntries(buffer: MixedBuffer, numTables: Int): List<TableEntry> {
        TODO("Not yet implemented")
    }

    private fun uncompressTable(buffer: MixedBuffer, tableEntry: TableEntry): Table {
        TODO("Not yet implemented")
    }
}

private data class TableEntry(
    val tag: String,
    val checksum: Int,
    val offset: Int,
    val length: Int,
    val compression: Boolean,
    val compressedLength: Int
)

private data class Table(
    val buffer: MixedBuffer, val offset: Int
)

private class Tables {
    var head: Head? = null
    var cmap: Cmap? = null
    var cvt: ShortArray? = null
    var fpgm: ByteArray? = null
}

internal class Glyph {
    val unicodes: List<Int> = listOf()
}
