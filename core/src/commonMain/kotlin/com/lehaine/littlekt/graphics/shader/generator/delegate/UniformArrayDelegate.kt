package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class UniformArrayDelegate<T : Variable>(
    val size: Int,
    private val factory: (builder: GlslGenerator) -> T,
    private val precision: Precision
) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): UniformArrayDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        when (v.typeName) {
            "mat4" -> thisRef.parameters.add(ShaderParameter.UniformArrayMat4(property.name))
        }
        thisRef.uniforms.add("${precision.value}${v.typeName} ${property.name}[$size]")
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): T {
        return v
    }
}