/**
 * @author Colton Daily
 * @date 9/29/2021
 */
package com.littlekt.graph.internal

import kotlin.reflect.KMutableProperty0

internal fun Int.isFlagSet(flag: Int) = (this and flag) != 0

internal fun Int.isUnshiftedFlagSet(flag: Int): Boolean {
    val temp = 1 shl flag
    return (this and temp) != 0
}

internal fun KMutableProperty0<Int>.setFlagExclusive(flag: Int) {
    this.set(1 shl flag)
}

internal fun KMutableProperty0<Int>.setFlag(flag: Int) {
    this.set(this.get() or 1 shl flag)
}

internal fun KMutableProperty0<Int>.unsetFlag(flag: Int) {
    val temp = 1 shl flag
    this.set(this.get() and temp.inv())
}

internal fun KMutableProperty0<Int>.invertFlags() {
    this.set(this.get().inv())
}

internal fun Int.binaryStringRepresentation(startPadding: Int = 10): String {
    return this.toString(2).padStart(startPadding, '0')
}
