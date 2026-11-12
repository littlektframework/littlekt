package com.littlekt.file.font.ttf.table

import com.littlekt.file.ByteBuffer
import com.littlekt.util.datastructure.IntMap

/**
 * Horizontal kerning between pairs of glyph indices, read from the `kern` table and the pair
 * adjustments of the `GPOS` table. Lookups don't allocate.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
internal class KerningTable {
    private val pairs = IntMap<Short>()
    private val classSubtables = mutableListOf<ClassPairs>()

    val isEmpty: Boolean
        get() = pairs.size == 0 && classSubtables.isEmpty()

    /** @return the kerning between glyph indices [left] and [right] in font units. */
    operator fun get(left: Int, right: Int): Int {
        pairs[key(left, right)]?.let {
            return it.toInt()
        }
        for (i in classSubtables.indices) {
            val subtable = classSubtables[i]
            val value = subtable[left, right]
            if (value != NOT_FOUND) return value
        }
        return 0
    }

    /** Reads the pairs of a `kern` table at [offset]. */
    fun parseKern(buffer: ByteBuffer, offset: Int) {
        val version = buffer.getUShort(offset).toInt()
        if (version == 0) {
            val count = buffer.getUShort(offset + 2).toInt()
            var sub = offset + 4
            repeat(count) {
                val length = buffer.getUShort(sub + 2).toInt()
                val coverage = buffer.getUShort(sub + 4).toInt()
                val format = coverage ushr 8
                val horizontal = coverage and 1 != 0
                val minimum = coverage and 2 != 0
                val crossStream = coverage and 4 != 0
                if (format == 0 && horizontal && !minimum && !crossStream) {
                    readFormat0Pairs(buffer, sub + 6)
                }
                sub += length
            }
        } else {
            val count = buffer.getInt(offset + 4)
            var sub = offset + 8
            repeat(count) {
                val length = buffer.getInt(sub)
                val coverage = buffer.getUShort(sub + 4).toInt()
                val format = coverage and 0xFF
                val vertical = coverage and 0x8000 != 0
                val crossStream = coverage and 0x4000 != 0
                if (format == 0 && !vertical && !crossStream) readFormat0Pairs(buffer, sub + 8)
                sub += length
            }
        }
    }

    /** Reads the pair adjustments of the `kern` feature of a `GPOS` table at [offset]. */
    fun parseGpos(buffer: ByteBuffer, offset: Int) {
        val featureList = offset + buffer.getUShort(offset + 6).toInt()
        val lookupList = offset + buffer.getUShort(offset + 8).toInt()
        val lookupIndices = mutableSetOf<Int>()
        val featureCount = buffer.getUShort(featureList).toInt()
        for (i in 0 until featureCount) {
            val record = featureList + 2 + i * 6
            if (buffer.getString(record, 4) != "kern") continue
            val feature = featureList + buffer.getUShort(record + 4).toInt()
            val count = buffer.getUShort(feature + 2).toInt()
            for (j in 0 until count) lookupIndices += buffer.getUShort(feature + 4 + j * 2).toInt()
        }
        val lookupCount = buffer.getUShort(lookupList).toInt()
        lookupIndices.sorted().forEach { index ->
            if (index >= lookupCount) return@forEach
            val lookup = lookupList + buffer.getUShort(lookupList + 2 + index * 2).toInt()
            val type = buffer.getUShort(lookup).toInt()
            val subtableCount = buffer.getUShort(lookup + 4).toInt()
            for (s in 0 until subtableCount) {
                var subtable = lookup + buffer.getUShort(lookup + 6 + s * 2).toInt()
                var subtableType = type
                if (type == EXTENSION) {
                    subtableType = buffer.getUShort(subtable + 2).toInt()
                    subtable += buffer.getInt(subtable + 4)
                }
                if (subtableType == PAIR_ADJUSTMENT) readPairPos(buffer, subtable)
            }
        }
    }

    private fun readFormat0Pairs(buffer: ByteBuffer, offset: Int) {
        val count = buffer.getUShort(offset).toInt()
        var pair = offset + 8
        repeat(count) {
            val left = buffer.getUShort(pair).toInt()
            val right = buffer.getUShort(pair + 2).toInt()
            val value = buffer.getShort(pair + 4)
            val key = key(left, right)
            if (pairs[key] == null) pairs[key] = value
            pair += 6
        }
    }

    private fun readPairPos(buffer: ByteBuffer, offset: Int) {
        val format = buffer.getUShort(offset).toInt()
        val coverage = readCoverage(buffer, offset + buffer.getUShort(offset + 2).toInt())
        val valueFormat1 = buffer.getUShort(offset + 4).toInt()
        val valueFormat2 = buffer.getUShort(offset + 6).toInt()
        val size1 = valueRecordSize(valueFormat1)
        val size2 = valueRecordSize(valueFormat2)
        val xAdvance = xAdvanceOffset(valueFormat1)
        when (format) {
            1 -> {
                val pairSetCount = buffer.getUShort(offset + 8).toInt()
                for (i in 0 until pairSetCount) {
                    val left = coverage.glyphAt(i)
                    if (left < 0) continue
                    val pairSet = offset + buffer.getUShort(offset + 10 + i * 2).toInt()
                    val count = buffer.getUShort(pairSet).toInt()
                    var record = pairSet + 2
                    repeat(count) {
                        if (xAdvance >= 0) {
                            val right = buffer.getUShort(record).toInt()
                            val key = key(left, right)
                            if (pairs[key] == null) pairs[key] = buffer.getShort(record + 2 + xAdvance)
                        }
                        record += 2 + size1 + size2
                    }
                }
            }
            2 -> {
                if (xAdvance < 0) return
                val classDef1 = readClassDef(buffer, offset + buffer.getUShort(offset + 8).toInt())
                val classDef2 = readClassDef(buffer, offset + buffer.getUShort(offset + 10).toInt())
                val class1Count = buffer.getUShort(offset + 12).toInt()
                val class2Count = buffer.getUShort(offset + 14).toInt()
                val values = ShortArray(class1Count * class2Count)
                var record = offset + 16
                for (c1 in 0 until class1Count) {
                    for (c2 in 0 until class2Count) {
                        values[c1 * class2Count + c2] = buffer.getShort(record + xAdvance)
                        record += size1 + size2
                    }
                }
                classSubtables += ClassPairs(coverage, classDef1, classDef2, class2Count, values)
            }
        }
    }

    private fun readCoverage(buffer: ByteBuffer, offset: Int): Coverage {
        val coverage = Coverage()
        when (buffer.getUShort(offset).toInt()) {
            1 -> {
                val count = buffer.getUShort(offset + 2).toInt()
                for (i in 0 until count) {
                    coverage.add(buffer.getUShort(offset + 4 + i * 2).toInt(), i)
                }
            }
            2 -> {
                val count = buffer.getUShort(offset + 2).toInt()
                for (i in 0 until count) {
                    val range = offset + 4 + i * 6
                    val start = buffer.getUShort(range).toInt()
                    val end = buffer.getUShort(range + 2).toInt()
                    val startIndex = buffer.getUShort(range + 4).toInt()
                    for (glyph in start..end) coverage.add(glyph, startIndex + glyph - start)
                }
            }
        }
        return coverage
    }

    private fun readClassDef(buffer: ByteBuffer, offset: Int): IntMap<Int> {
        val classes = IntMap<Int>()
        when (buffer.getUShort(offset).toInt()) {
            1 -> {
                val start = buffer.getUShort(offset + 2).toInt()
                val count = buffer.getUShort(offset + 4).toInt()
                for (i in 0 until count) {
                    val value = buffer.getUShort(offset + 6 + i * 2).toInt()
                    if (value != 0) classes[start + i] = value
                }
            }
            2 -> {
                val count = buffer.getUShort(offset + 2).toInt()
                for (i in 0 until count) {
                    val range = offset + 4 + i * 6
                    val start = buffer.getUShort(range).toInt()
                    val end = buffer.getUShort(range + 2).toInt()
                    val value = buffer.getUShort(range + 4).toInt()
                    if (value != 0) for (glyph in start..end) classes[glyph] = value
                }
            }
        }
        return classes
    }

    private fun valueRecordSize(format: Int): Int = (format and 0xFF).countOneBits() * 2

    private fun xAdvanceOffset(format: Int): Int {
        if (format and X_ADVANCE == 0) return -1
        return (format and (X_ADVANCE - 1)).countOneBits() * 2
    }

    private class Coverage {
        val indices = IntMap<Int>()
        private val glyphs = mutableListOf<Int>()

        fun add(glyph: Int, index: Int) {
            indices[glyph] = index
            while (glyphs.size <= index) glyphs += -1
            glyphs[index] = glyph
        }

        fun glyphAt(index: Int): Int = if (index < glyphs.size) glyphs[index] else -1
    }

    private class ClassPairs(
        val coverage: Coverage,
        val classDef1: IntMap<Int>,
        val classDef2: IntMap<Int>,
        val class2Count: Int,
        val values: ShortArray,
    ) {
        operator fun get(left: Int, right: Int): Int {
            if (coverage.indices[left] == null) return NOT_FOUND
            val c1 = classDef1[left] ?: 0
            val c2 = classDef2[right] ?: 0
            val index = c1 * class2Count + c2
            return if (index < values.size) values[index].toInt() else NOT_FOUND
        }
    }

    private companion object {
        const val PAIR_ADJUSTMENT = 2
        const val EXTENSION = 9
        const val X_ADVANCE = 0x4
        const val NOT_FOUND = Int.MIN_VALUE

        fun key(left: Int, right: Int): Int = (left shl 16) or (right and 0xFFFF)
    }
}
