package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class AttributeDelegate<T : Variable>(
    private val factory: (GlslGenerator) -> T,
    private val precision: Precision
) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): AttributeDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): T {
        when (v.typeName) {
            "vec2" -> thisRef.parameters.add(ShaderParameter.AttributeVec2(property.name))
            "vec3" -> thisRef.parameters.add(ShaderParameter.AttributeVec3(property.name))
            "vec4" -> thisRef.parameters.add(ShaderParameter.AttributeVec4(property.name))
        }
        thisRef.attributes.add("${precision.value}${v.typeName} ${property.name}")
        return v
    }
}

class AttributeConstructorDelegate<T : Variable>(private val v: T, private val precision: Precision) {

    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): AttributeConstructorDelegate<T> {
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        v.builder.attributes.add("${precision.value}${v.typeName} ${property.name}")
        return v
    }
}