package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.delegate.*
import com.lehaine.littlekt.graphics.shader.generator.type.BoolResult
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface Shader {
    var source: String
    val parameters: List<ShaderParameter>
}

interface FragmentShader : Shader
interface VertexShader : Shader

abstract class FragmentShaderModel : GlslGenerator(), FragmentShader {
    override var source: String = ""
        get() {
            if (field.isBlank()) {
                field = generate()
            }
            return field
        }

    var gl_FragCoord by BuiltinVarDelegate()
    var gl_FragColor by BuiltinVarDelegate()

    val gl_FrontFacing = BoolResult("gl_FrontFacing")

    /**
     * Data coming **IN** from the Vertex Shader.
     */
    fun <T : Variable> varying(factory: (GlslGenerator) -> T, precision: Precision = Precision.DEFAULT) =
        VaryingDelegate(factory, precision)

    /**
     * Data coming **IN** from the Vertex Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> varyingCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT
    ): VaryingConstructorDelegate<T> =
        VaryingConstructorDelegate(createVariable(clazz), precision) as VaryingConstructorDelegate<T>


    fun dFdx(v: GLFloat) = GLFloat(this, "dFdx(${v.value})")
    fun dFdy(v: GLFloat) = GLFloat(this, "dFdy(${v.value})")
}

abstract class VertexShaderModel : GlslGenerator(), VertexShader {
    var gl_Position by BuiltinVarDelegate()

    override var source: String = ""
        get() {
            if (field.isBlank()) {
                field = generate()
            }
            return field
        }

    /**
     * Data that may change per vertex. Passed from the OpenGL context to the Vertex Shader.
     */
    fun <T : Variable> attribute(factory: (GlslGenerator) -> T, precision: Precision = Precision.DEFAULT) =
        AttributeDelegate(factory, precision)

    /**
     * Data that may change per vertex. Passed from the OpenGL context to the Vertex Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> attributeCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT
    ): AttributeConstructorDelegate<T> =
        AttributeConstructorDelegate(createVariable(clazz), precision) as AttributeConstructorDelegate<T>


    /**
     * Data going **OUT** to the Fragment Shader.
     */
    fun <T : Variable> varying(factory: (GlslGenerator) -> T, precision: Precision = Precision.DEFAULT) =
        VaryingDelegate(factory, precision)

    /**
     * Data going **OUT** to the Fragment Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> varyingCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT
    ): VaryingConstructorDelegate<T> =
        VaryingConstructorDelegate(createVariable(clazz), precision) as VaryingConstructorDelegate<T>
}


open class ShaderModel {
    inline fun fragment(crossinline src: FragmentShaderModel.() -> Unit): FragmentShaderModel {
        return object : FragmentShaderModel() {}.apply(src)
    }

    inline fun vertex(crossinline src: VertexShaderModel.() -> Unit): VertexShaderModel {
        return object : VertexShaderModel() {}.apply(src)
    }
}
