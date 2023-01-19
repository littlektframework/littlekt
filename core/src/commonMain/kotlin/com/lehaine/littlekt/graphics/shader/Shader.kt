package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.delegate.*
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

    var gl_FragCoord by BuiltinVarDelegate()
    var gl_FragColor by BuiltinVarDelegate()

    val gl_FrontFacing = "gl_FrontFacing".bool

    override fun generate(context: Context): String {
        return if (source.isBlank()) {
            super.generate(context).also { source = it }
        } else {
            source = ensureShaderVersionChanges(context, source)
            source
        }
    }

    /**
     * Data coming **IN** from the Vertex Shader.
     * @param predicate if `true` then this varying will be generated.
     */
    fun <T : Variable> varying(
        factory: (GlslGenerator) -> T,
        precision: Precision = Precision.DEFAULT,
        predicate: Boolean = true,
    ) =
        VaryingDelegate(factory, precision, predicate)

    /**
     * Data coming **IN** from the Vertex Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> varyingCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT,
    ): VaryingConstructorDelegate<T> =
        VaryingConstructorDelegate(createVariable(clazz), precision) as VaryingConstructorDelegate<T>


    fun dFdx(v: GLFloat) = GLFloat(this, "dFdx(${v.value})")
    fun dFdy(v: GLFloat) = GLFloat(this, "dFdy(${v.value})")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FragmentShaderModel

        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        return source.hashCode()
    }
}

abstract class VertexShaderModel : GlslGenerator(), VertexShader {
    var gl_Position by BuiltinVarDelegate()

    override var source: String = ""

    override fun generate(context: Context): String {
        return if (source.isBlank()) {
            super.generate(context).also { source = it }
        } else {
            source = ensureShaderVersionChanges(context, source)
            source
        }
    }

    /**
     * Data that may change per vertex. Passed from the OpenGL context to the Vertex Shader.
     * @param predicate if `true` then this attribute will be generated.
     */
    fun <T : Variable> attribute(
        factory: (GlslGenerator) -> T,
        precision: Precision = Precision.DEFAULT,
        predicate: Boolean = true,
    ) =
        AttributeDelegate(factory, precision, predicate)

    /**
     * Data that may change per vertex. Passed from the OpenGL context to the Vertex Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> attributeCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT,
    ): AttributeConstructorDelegate<T> =
        AttributeConstructorDelegate(createVariable(clazz), precision) as AttributeConstructorDelegate<T>


    /**
     * Data going **OUT** to the Fragment Shader.
     * @param predicate if `true` then this varying will be generated.
     */
    fun <T : Variable> varying(
        factory: (GlslGenerator) -> T, precision: Precision = Precision.DEFAULT,
        predicate: Boolean = true,
    ) =
        VaryingDelegate(factory, precision, predicate)

    /**
     * Data going **OUT** to the Fragment Shader.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> varyingCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT,
    ): VaryingConstructorDelegate<T> =
        VaryingConstructorDelegate(createVariable(clazz), precision) as VaryingConstructorDelegate<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VertexShaderModel

        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        return source.hashCode()
    }
}


open class ShaderModel {
    inline fun fragment(crossinline src: FragmentShaderModel.() -> Unit): FragmentShaderModel {
        return object : FragmentShaderModel() {}.apply(src)
    }

    inline fun vertex(crossinline src: VertexShaderModel.() -> Unit): VertexShaderModel {
        return object : VertexShaderModel() {}.apply(src)
    }
}
