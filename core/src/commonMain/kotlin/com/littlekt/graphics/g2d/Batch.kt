package com.littlekt.graphics.g2d

import com.littlekt.Releasable
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader
import com.littlekt.math.Mat4
import com.littlekt.math.geom.Angle
import io.ygdrasil.webgpu.RenderPassEncoder
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState.ColorTargetState.BlendState

/**
 * An interface for creating batch renderers.
 *
 * @see SpriteBatch
 * @author Colton Daily
 * @date 2/8/2022
 */
interface Batch : Releasable {

    /**
     * The current [Shader] used for rendering. Any subsequent calls to [draw] will use this shader.
     */
    var shader: Shader

    /** The [Shader] to be used as the default shader if no [shader] is specified directly. */
    val defaultShader: Shader

    /**
     * The transform matrix that can be used to multiply against the [viewProjection] matrix. This
     * should be set directly instead of manipulating the underlying matrix.
     */
    var transformMatrix: Mat4

    /**
     * The view projection matrix to be used when rendering. This should be set directly instead of
     * manipulating the underlying matrix.
     */
    var viewProjection: Mat4

    /** @return `true` if [begin] has been called and [end] hasn't yet. */
    val drawing: Boolean

    /**
     * The index of the last mesh rendered to. This can be manipulated to draw back on a mesh that
     * has already been drawn to one [flush] has been invoked.
     */
    var lastMeshIdx: Int

    /**
     * Invoke this to begin a [Batch] pass. Calls to [draw], [flush], and [end] cannot happen until
     * [begin] is called.
     */
    fun begin(viewProjection: Mat4? = null)

    /**
     * Draws a textured quad.
     *
     * @param texture the texture to draw
     * @param x the x-coord to draw the texture
     * @param y the y-coord to draw the texture
     * @param originX the x-origin to draw the quad
     * @param originY the y-origin to draw the quad
     * @param width the width of the quad
     * @param height the height of the quad
     * @param scaleX the scale of the quad in the x-direction
     * @param scaleY the scale of the quad in the y-direction
     * @param rotation the rotation of the quad
     * @param color the color of the quad
     * @param flipX flip the quad horizontally
     * @param flipY flip the quad vertically
     */
    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = texture.width.toFloat(),
        height: Float = texture.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        flipX: Boolean = false,
        flipY: Boolean = false,
    ) =
        draw(
            texture = texture,
            x = x,
            y = y,
            originX = originX,
            originY = originY,
            width = width,
            height = height,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            srcX = 0,
            srcY = 0,
            srcWidth = texture.width,
            srcHeight = texture.height,
            color = color,
            flipX = flipX,
            flipY = flipY
        )

    /**
     * Draws a textured quad.
     *
     * @param slice the texture slice to draw
     * @param x the x-coord to draw the texture
     * @param y the y-coord to draw the texture
     * @param originX the x-origin to draw the quad
     * @param originY the y-origin to draw the quad
     * @param width the width of the quad
     * @param height the height of the quad
     * @param scaleX the scale of the quad in the x-direction
     * @param scaleY the scale of the quad in the y-direction
     * @param rotation the rotation of the quad
     * @param color the color of the quad
     * @param flipX flip the quad horizontally
     * @param flipY flip the quad vertically
     */
    fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = slice.width.toFloat(),
        height: Float = slice.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    /**
     * Draws a textured quad.
     *
     * @param slice the texture slice to draw
     * @param x the x-coord to draw the texture
     * @param y the y-coord to draw the texture
     * @param originX the x-origin to draw the quad
     * @param originY the y-origin to draw the quad
     * @param width the width of the quad
     * @param height the height of the quad
     * @param scaleX the scale of the quad in the x-direction
     * @param scaleY the scale of the quad in the y-direction
     * @param rotation the rotation of the quad
     * @param color the color of the quad
     * @param srcX the x-coord of the slices location texture
     * @param srcY the y-coord of the slices location on the texture
     * @param srcWidth the width of the slice
     * @param srcHeight the height of the slice
     * @param flipX flip the quad horizontally
     * @param flipY flip the quad vertically
     */
    fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = slice.width.toFloat(),
        height: Float = slice.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        srcX: Int = slice.x,
        srcY: Int = slice.y,
        srcWidth: Int = slice.width,
        srcHeight: Int = slice.height,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    /**
     * Draws a textured quad.
     *
     * @param texture the texture to draw
     * @param x the x-coord to draw the texture
     * @param y the y-coord to draw the texture
     * @param originX the x-origin to draw the quad
     * @param originY the y-origin to draw the quad
     * @param width the width of the quad
     * @param height the height of the quad
     * @param scaleX the scale of the quad in the x-direction
     * @param scaleY the scale of the quad in the y-direction
     * @param rotation the rotation of the quad
     * @param color the color of the quad
     * @param srcX the x-coord of the slices location texture
     * @param srcY the y-coord of the slices location on the texture
     * @param srcWidth the width of the slice
     * @param srcHeight the height of the slice
     * @param flipX flip the quad horizontally
     * @param flipY flip the quad vertically
     */
    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = texture.width.toFloat(),
        height: Float = texture.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        srcX: Int = 0,
        srcY: Int = 0,
        srcWidth: Int = texture.width,
        srcHeight: Int = texture.height,
        color: Color = Color.WHITE,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    /**
     * Draws the texture, using the specified vertices. This may cause a [flush], if the
     * [spriteVertices] exceed the internal meshes buffer capacity.
     *
     * @param texture the texture to draw from
     * @param spriteVertices the vertices defining the draw call
     * @param offset the offset of the [spriteVertices]
     * @param count the size of vertices to use
     */
    fun draw(
        texture: Texture,
        spriteVertices: FloatArray,
        offset: Int = 0,
        count: Int = spriteVertices.size - offset
    )

    /**
     * Updates the internal mesh and submits all queued draw calls to the [RenderPassEncoder] and
     * clears the draw call queue. This will increment [lastMeshIdx] by 1. If any new calls to
     * [draw] before [end] is invoked, will be written to a new mesh to allow for multiple flushes
     * over multiple render passes within a single command pass.
     *
     * @param renderPassEncoder the encoder to use for the render pass.
     * @param viewProjection an optional view projection matrix to begin the initial render pass.
     *   **Note**: this does NOT draw or render the queued draw calls with this matrix! If the
     *   required matrix is needed for rendering then set [viewProjection] directly prior to any
     *   [draw] calls.
     */
    fun flush(renderPassEncoder: RenderPassEncoder, viewProjection: Mat4? = null)

    /**
     * Only invoke this when no more drawing or flushes are needed. This will reset [lastMeshIdx]
     * back to `0` and clear mesh vertices. Invoking this function should mean, that, render passes
     * are now finished, and the command encoder is finished.
     *
     * **WARNING:** Calling [begin] before finishing the command encoder pass will overwrite any
     * existing mesh data! Ensure [CommandEncoder.finish] is called and uploaded with [Queue.submit]
     * to prevent this. If more drawing is required, then only invoke [flush] instead of [end].
     */
    fun end()

    /** Sets the blend state to the new blend state. */
    fun setBlendState(newBlendState: BlendState)

    /**
     * Sets this blend state back to the previous blend state. Calling this function in succession
     * will swap back the blend states back and forth
     */
    fun swapToPreviousBlendState()

    /** Reverts back to the default shader. */
    fun useDefaultShader()
}
