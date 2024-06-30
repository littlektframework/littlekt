package com.littlekt.math.geom

// https://github.com/korlibs/korge-next/blob/master/korma/src/commonMain/kotlin/com/soywiz/korma/geom/range/OpenRange.kt
class OpenRange<T : Comparable<T>>(val start: T, val endExclusive: T)

operator fun <T : Comparable<T>> OpenRange<T>.contains(item: T) =
    item >= this.start && item < this.endExclusive
