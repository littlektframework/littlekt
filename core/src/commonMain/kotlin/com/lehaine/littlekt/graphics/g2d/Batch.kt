package com.lehaine.littlekt.graphics.g2d

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
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
    val drawing: Boolean
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
        colorBits = colorBits,
        flipX = flipX,
        flipY = flipY
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
        srcX: Int = slice.x,
        srcY: Int = slice.y,
        srcWidth: Int = slice.width,
        srcHeight: Int = slice.height,
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
        dstFuncAlpha: BlendFactor,
    )

    fun setToPreviousBlendFunction()

    fun useDefaultShader()

    companion object {
        const val X1 = 0
        const val Y1 = 1
        const val C1 = 2
        const val U1 = 3
        const val V1 = 4
        const val X2 = 5
        const val Y2 = 6
        const val C2 = 7
        const val U2 = 8
        const val V2 = 9
        const val X3 = 10
        const val Y3 = 11
        const val C3 = 12
        const val U3 = 13
        const val V3 = 14
        const val X4 = 15
        const val Y4 = 16
        const val C4 = 17
        const val U4 = 18
        const val V4 = 19
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Batch> T.use(projectionMatrix: Mat4? = null, action: (T) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    end()
}