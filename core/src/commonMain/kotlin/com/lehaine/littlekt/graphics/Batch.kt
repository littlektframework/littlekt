package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.BlendFactor
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Colton Daily
 * @date 2/8/2022
 */
interface Batch : Disposable {
    var color: Color
    var colorBits: Float
    var transformMatrix: Mat4
    var projectionMatrix: Mat4
    var shader: ShaderProgram<*, *>

    fun begin(projectionMatrix: Mat4? = null)

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
        colorBits: Float = this.colorBits,
        flipX: Boolean = false,
        flipY: Boolean = false,
    ) = draw(
        texture,
        x,
        y,
        originX,
        originY,
        width,
        height,
        scaleX,
        scaleY,
        rotation,
        0,
        0,
        texture.width,
        texture.height,
        colorBits,
        flipX,
        flipY
    )

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
        colorBits: Float = this.colorBits,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

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
        colorBits: Float = this.colorBits,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int = 0, count: Int = spriteVertices.size)
    fun end()
    fun flush()

    fun setBlendFunction(src: BlendFactor, dst: BlendFactor)
    fun setBlendFunctionSeparate(
        srcFuncColor: BlendFactor,
        dstFuncColor: BlendFactor,
        srcFuncAlpha: BlendFactor,
        dstFuncAlpha: BlendFactor
    )

    fun setToPreviousBlendFunction()

    fun useDefaultShader()
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Batch> T.use(projectionMatrix: Mat4? = null, action: (T) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    end()
}