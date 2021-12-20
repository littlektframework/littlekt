package com.lehaine.littlekt.file.font.ttf

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.table.*
import com.lehaine.littlekt.graphics.font.Glyph

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class TtfFontReader {

    private val tables = Tables()
    private var encoding: Encoding = DefaultEncoding(this)
    private lateinit var glyphNames: GlyphNames

    internal lateinit var glyphs: GlyphSet

    var isCIDFont: Boolean = false
        private set
    var outlinesFormat: String = ""
        private set
    var unitsPerEm: Int = 0
        private set
    var ascender: Int = 0
        private set
    var descender: Int = 0
        private set
    var numberOfHMetrics: Int = 0
        private set
    var numGlyphs: Int = 0
        private set

    operator fun get(codePoint: Int): Glyph = this[codePoint.toChar()]
    operator fun get(char: Char): Glyph {
        val glyphIndex = encoding.charToGlyphIndex(char) ?: 0
        return glyphs[glyphIndex].let {
            it.calcPath()
            it.toImmutable()
        }
    }

    fun parse(buffer: MixedBuffer) {
        val numTables: Int
        val tableEntries: List<TableEntry>
        val signature = buffer.getString(0, 4)
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
            val flavor = buffer.getString(4, 4)
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

        var indexToLocFormat = 0
        var ltagTable: List<String>

        var cffTableEntry: TableEntry? = null
        var fvarTableEntry: TableEntry? = null
        var glyfTableEntry: TableEntry? = null
        var gdefTableEntry: TableEntry? = null
        var gposTableEntry: TableEntry? = null
        var gsubTableEntry: TableEntry? = null
        var hmtxTableEntry: TableEntry? = null
        var kernTableEntry: TableEntry? = null
        var locaTableEntry: TableEntry? = null
        var nameTableEntry: TableEntry? = null
        var metaTableEntry: TableEntry? = null
        var parser: Parser

        tableEntries.forEach { tableEntry ->
            val table: Table
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
                "hhea" -> {
                    table = uncompressTable(buffer, tableEntry)
                    tables.hhea = HheaParser(table.buffer, table.offset).parse().also {
                        ascender = it.ascender
                        descender = it.descender
                        numberOfHMetrics = it.numberOfHMetrics
                    }
                }
                "hmtx" -> {
                    hmtxTableEntry = tableEntry
                }
                "ltag" -> {
                    table = uncompressTable(buffer, tableEntry)
                    ltagTable = LtagParser(table.buffer, table.offset).parse()
                }
                "maxp" -> {
                    table = uncompressTable(buffer, tableEntry)
                    tables.maxp = MaxpParser(table.buffer, table.offset).parse().also {
                        numGlyphs = it.numGlyphs
                    }
                }
                "name" -> {
                    nameTableEntry = tableEntry
                }
                "OS/2" -> {
                    table = uncompressTable(buffer, tableEntry)
                    val os2 = Os2Parser(table.buffer, table.offset).parse()
                    tables.os2 = os2
                }
                "post" -> {
                    table = uncompressTable(buffer, tableEntry)
                    tables.post = PostParser(table.buffer, table.offset).parse().also {
                        glyphNames = GlyphNames(it)
                    }
                }
                "prep" -> {
                    table = uncompressTable(buffer, tableEntry)
                    parser = Parser(table.buffer, table.offset)
                    tables.prep = parser.parseByteList(tableEntry.length)
                }
                "glyf" -> glyfTableEntry = tableEntry
                "loca" -> locaTableEntry = tableEntry
                "CFF " -> cffTableEntry = tableEntry
                "kern" -> kernTableEntry = tableEntry
                "GDEF" -> gdefTableEntry = tableEntry
                "GPOS" -> gposTableEntry = tableEntry
                "GSUB" -> gsubTableEntry = tableEntry
                "meta" -> metaTableEntry = tableEntry
            }
        }
        // TODO Determine if name table is needed


        if (glyfTableEntry != null && locaTableEntry != null) {
            val glyf = glyfTableEntry!!
            val loca = locaTableEntry!!
            val shortVersion = indexToLocFormat == 0
            val locaTable = uncompressTable(buffer, loca)
            val locaOffsets = LocaParser(locaTable.buffer, locaTable.offset, numGlyphs, shortVersion).parse()
            val glyfTable = uncompressTable(buffer, glyf)
            glyphs = GlyfParser(glyfTable.buffer, glyfTable.offset, locaOffsets, this).parse()
        } else if (cffTableEntry != null) {
            // TODO CFF table entry
        } else {
            throw RuntimeException("Font doesn't contain TrueType or CFF outlines.")
        }

        val hmtxTable =
            uncompressTable(buffer, hmtxTableEntry ?: throw RuntimeException("hmtx table entry was not found!"))
        HmtxParser(hmtxTable.buffer, hmtxTable.offset, numberOfHMetrics, numGlyphs, glyphs).parse()
        addGlyphNames()
    }

    private fun parseOpenTypeTableEntries(buffer: MixedBuffer, numTables: Int): List<TableEntry> {
        val tableEntries = mutableListOf<TableEntry>()
        var p = 12
        for (i in 0 until numTables) {
            val tag = buffer.getString(p, 4)
            val checksum = buffer.getUint32(p + 4)
            val offset = buffer.getUint32(p + 8)
            val length = buffer.getUint32(p + 12)
            tableEntries += TableEntry(tag, checksum, offset, length, Compression.NONE, 0)
            p += 16
        }
        return tableEntries.toList()
    }

    private fun parseWOFFTableEntries(buffer: MixedBuffer, numTables: Int): List<TableEntry> {
        // TODO("Not yet implemented")
        return listOf()
    }

    private fun uncompressTable(buffer: MixedBuffer, tableEntry: TableEntry): Table {
        if (tableEntry.compression == Compression.WOFF) {
            // TODO impl inflating WOFF compression
        }
        return Table(buffer, tableEntry.offset)
    }

    private fun addGlyphNames() {
        val glyphIndexMap =
            tables.cmap?.glyphIndexMap ?: error("Unable to add glyph name due to cmap table being null")
        val codes = glyphIndexMap.keys
        codes.forEach {
            val glyphIndex = glyphIndexMap[it] ?: 0
            glyphs[glyphIndex].apply {
                addUnicode(it)
            }
        }
        glyphs.forEachIndexed { i, glyph ->
            if (encoding is CffEncoding) {
                if (isCIDFont) {
                    glyph.name = "gid$i"
                } else {
                    glyph.name = (encoding as CffEncoding).charset[i].toString()
                }
            } else if (glyphNames.names.isNotEmpty()) {
                glyph.name = glyphNames.names[i]
            }
        }
    }

    override fun toString(): String {
        return "TtfFont(outlinesFormat='$outlinesFormat', tables=$tables, encoding=$encoding, unitsPerEm=$unitsPerEm, ascender=$ascender, descender=$descender, numberOfHMetrics=$numberOfHMetrics, numGlyphs=$numGlyphs, glyphNames=$glyphNames, glyphs=$glyphs)"
    }
}

private enum class Compression {
    WOFF,
    NONE
}

private data class TableEntry(
    val tag: String,
    val checksum: Int,
    val offset: Int,
    val length: Int,
    val compression: Compression,
    val compressedLength: Int
)

private data class Table(
    val buffer: MixedBuffer, val offset: Int
)

internal class Tables {
    var head: Head? = null
    var cmap: Cmap? = null
    var cvt: ShortArray? = null
    var fpgm: ByteArray? = null
    var hhea: Hhea? = null
    var maxp: Maxp? = null
    var os2: Os2? = null
    var post: Post? = null
    var prep: ByteArray? = null

    override fun toString(): String {
        return "Tables(head=$head, cmap=$cmap, hhea=$hhea, maxp=$maxp, os2=$os2, post=$post"
    }
}


