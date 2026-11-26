package com.littlekt.graphics.g2d

import com.littlekt.graphics.Texture
import com.littlekt.math.Mat4

/**
 * A scissor rectangle in render target pixels, or none when not [enabled].
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class ScissorRect {
    var enabled = false
        private set

    var x = 0
        private set

    var y = 0
        private set

    var width = 0
        private set

    var height = 0
        private set

    fun set(x: Int, y: Int, width: Int, height: Int) {
        enabled = true
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    fun set(other: ScissorRect) {
        enabled = other.enabled
        x = other.x
        y = other.y
        width = other.width
        height = other.height
    }

    fun clear() {
        enabled = false
        x = 0
        y = 0
        width = 0
        height = 0
    }

    fun matches(other: ScissorRect): Boolean =
        enabled == other.enabled &&
            (!enabled ||
                (x == other.x && y == other.y && width == other.width && height == other.height))
}

/**
 * A range of quads of a [SpriteBatch] flush drawn with the same texture, render state, matrix and
 * scissor rect.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class SpriteDrawCall {
    lateinit var texture: Texture
    lateinit var renderInfo: Any
    val combinedMatrix = Mat4()
    val scissor = ScissorRect()

    /** The scissor rect the render pass has after this draw call. */
    val passScissor = ScissorRect()
    var offset: Int = 0
    var instances: Int = 0

    /** `true` if the matrix differs from the previous draw call and needs a new camera slot. */
    var matrixChanged = false

    /** `true` if [scissor] must be set on the render pass before this draw call. */
    var scissorChanged = false
}

/**
 * The draw calls recorded by a [SpriteBatch] until its next flush. A draw call is only added when
 * the texture, render state, matrix or scissor rect changes, so a flush can draw every range in
 * the same render pass without flushing between them.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class SpriteDrawCalls {
    private val calls = ArrayList<SpriteDrawCall>()

    /** The number of recorded draw calls. */
    var size: Int = 0
        private set

    /** The last recorded draw call, if any. */
    val current: SpriteDrawCall?
        get() = if (size > 0) calls[size - 1] else null

    operator fun get(index: Int): SpriteDrawCall = calls[index]

    /**
     * Starts a draw call at [offset], reusing the last one if it has no instances yet.
     *
     * @return the started draw call
     */
    fun next(
        texture: Texture,
        renderInfo: Any,
        combinedMatrix: Mat4,
        scissor: ScissorRect,
        offset: Int,
    ): SpriteDrawCall {
        val last = current
        val call =
            if (last != null && last.instances == 0) {
                last
            } else {
                if (size == calls.size) calls += SpriteDrawCall()
                calls[size++]
            }
        val previous = if (size > 1) calls[size - 2] else null
        call.texture = texture
        call.renderInfo = renderInfo
        call.combinedMatrix.set(combinedMatrix)
        call.scissor.set(scissor)
        call.offset = offset
        call.instances = 0
        call.matrixChanged = previous == null || previous.combinedMatrix != combinedMatrix
        call.scissorChanged =
            scissor.enabled && (previous == null || !previous.passScissor.matches(scissor))
        if (scissor.enabled || previous == null) {
            call.passScissor.set(scissor)
        } else {
            call.passScissor.set(previous.passScissor)
        }
        return call
    }

    /** Removes every draw call. */
    fun clear() {
        size = 0
    }

    /** Removes every draw call and releases their references. */
    fun release() {
        size = 0
        calls.clear()
    }
}
