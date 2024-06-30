package com.littlekt

import java.lang.foreign.MemorySegment

/**
 * @author Colton Daily
 * @date 4/2/2024
 */
class Instance(val segment: MemorySegment) {
    override fun toString(): String {
        return "Instance()"
    }
}
