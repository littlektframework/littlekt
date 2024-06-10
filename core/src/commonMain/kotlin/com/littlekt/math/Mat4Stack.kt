package com.littlekt.math

/**
 * @author Colton Daily
 * @date 4/10/2024
 */
class Mat4Stack(val stackSize: Int = DEFAULT_STACK_SIZE) : Mat4() {
    companion object {
        const val DEFAULT_STACK_SIZE = 32
    }

    private var stackIndex = 0
    private val stack = FloatArray(16 * stackSize)

    fun push(): Mat4Stack {
        if (stackIndex >= stackSize) {
            throw IllegalStateException("Matrix stack overflow")
        }
        val offset = stackIndex * 16
        for (i in 0..15) {
            stack[offset + i] = data[i]
        }
        stackIndex++
        return this
    }

    fun pop(): Mat4Stack {
        if (stackIndex <= 0) {
            throw IllegalStateException("Matrix stack underflow")
        }
        stackIndex--
        val offset = stackIndex * 16
        for (i in 0..15) {
            data[i] = stack[offset + i]
        }
        return this
    }

    fun reset(): Mat4Stack {
        stackIndex = 0
        setToIdentity()
        return this
    }
}
