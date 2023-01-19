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
class UniformDelegate<T : Variable>(
    private val factory: (GlslGenerator) -> T,
    private val precision: Precision,
) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>,
    ): UniformDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        when (v.typeName) {
            "vec2" -> thisRef.parameters.add(ShaderParameter.UniformVec2(property.name))
            "vec3" -> thisRef.parameters.add(ShaderParameter.UniformVec3(property.name))
            "vec4" -> thisRef.parameters.add(ShaderParameter.UniformVec4(property.name))
            "int" -> thisRef.parameters.add(ShaderParameter.UniformInt(property.name))
            "float" -> thisRef.parameters.add(ShaderParameter.UniformFloat(property.name))
            "bool" -> thisRef.parameters.add(ShaderParameter.UniformBoolean(property.name))
            "sampler2D" -> thisRef.parameters.add(ShaderParameter.UniformSample2D(property.name))
            "mat4" -> thisRef.parameters.add(ShaderParameter.UniformMat4(property.name))
        }
        thisRef.uniforms.add("${precision.value}${v.typeName} ${property.name}")
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): T {
        return v
    }
}

class UniformConstructorDelegate<T : Variable>(private val v: T, private val precision: Precision) {

    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
    ): UniformConstructorDelegate<T> {
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        v.builder.uniforms.add("${precision.value}${v.typeName} ${property.name}")
        return v
    }
}