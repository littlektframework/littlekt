package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.util.internal.isFlagSet
import kotlin.jvm.JvmInline

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
@JvmInline
value class SizeFlag(val bit: Int) {

    fun isFlagSet(flag: SizeFlag) = bit.isFlagSet(flag.bit)

    infix fun or(flag: SizeFlag) = SizeFlag(bit.or(flag.bit))
    infix fun and(flag: SizeFlag) = SizeFlag(bit.and(flag.bit))

    companion object {
        val FILL = SizeFlag(1 shl 0)
        val EXPAND = SizeFlag(1 shl 1)
        val SHRINK_CENTER = SizeFlag(1 shl 2)
        val SHRINK_END = SizeFlag(1 shl 3)

        private val values =
            arrayOf(FILL, EXPAND, SHRINK_CENTER, SHRINK_END)

        fun values() = values

        fun set(flags: List<SizeFlag>): SizeFlag {
            var bit = flags[0].bit
            flags.forEachIndexed { index, sizeFlag ->
                if (index != 0) {
                    bit = bit or sizeFlag.bit
                }
            }
            return SizeFlag(bit)
        }

        operator fun invoke(str: String): SizeFlag = when (str) {
            "FILL" -> FILL
            "EXPAND" -> EXPAND
            "SHRINK_CENTER" -> SHRINK_CENTER
            "SHRINK_END" -> SHRINK_END
            else -> SizeFlag(str.substringAfter('(').substringBefore(')').toIntOrNull() ?: 0)
        }
    }


    override fun toString(): String = when (this) {
        FILL -> "FILL"
        EXPAND -> "EXPAND"
        SHRINK_CENTER -> "SHRINK_CENTER"
        SHRINK_END -> "SHRINK_END"
        else -> "SizeFlag($bit)"
    }
}