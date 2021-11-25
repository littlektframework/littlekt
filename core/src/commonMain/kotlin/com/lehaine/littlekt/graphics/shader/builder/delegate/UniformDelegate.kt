package com.lehaine.littlekt.graphics.shader.builder.delegate

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class UniformDelegate<T : Variable>(private val factory: (ShaderBuilder) -> T) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: ShaderBuilder,
        property: KProperty<*>
    ): UniformDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: ShaderBuilder, property: KProperty<*>): T {
        thisRef.uniforms.add("${v.typeName} ${property.name}")
        return v
    }
}