package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class UniformDelegate<T : Variable>(private val factory: (GlslGenerator) -> T) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): UniformDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): T {
        thisRef.uniforms.add("${v.typeName} ${property.name}")
        return v
    }
}