package com.littlekt.tools

import java.io.File
import java.math.BigInteger
import java.util.regex.Pattern
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
class FileNameComparator : Comparator<File> {

    override fun compare(left: File, right: File): Int {
        val s1 = left.name
        val s2 = right.name
        return StringCompare.compareNames(s1, s2)
    }
}

class NameComparator : Comparator<String> {

    override fun compare(s1: String, s2: String): Int {
        return StringCompare.compareNames(s1, s2)
    }
}

object StringCompare {
    private val NUMBERS = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")

    fun compareNames(s1: String, s2: String): Int {
        val split1 = NUMBERS.split(s1)
        val split2 = NUMBERS.split(s2)
        for (i in 0 until min(split1.size, split2.size)) {
            val c1 = split1[i][0]
            val c2 = split2[i][0]
            var cmp = 0
            // If both segments start with a digit, sort them numerically using
            // BigInteger to stay safe
            if (c1 in '0'..'9' && c2 in '0'..'9')
                cmp = BigInteger(split1[i]).compareTo(BigInteger(split2[i]))

            // If we haven't sorted numerically before, or if numeric sorting yielded
            // equality (e.g 007 and 7) then sort lexicographically
            if (cmp == 0) cmp = split1[i].compareTo(split2[i])

            // Abort once some prefix has unequal ordering
            if (cmp != 0) return cmp
        }

        // If we reach this, then both strings have equally ordered prefixes, but
        // maybe one string is longer than the other (i.e. has more segments)
        return split1.size - split2.size
    }
}
