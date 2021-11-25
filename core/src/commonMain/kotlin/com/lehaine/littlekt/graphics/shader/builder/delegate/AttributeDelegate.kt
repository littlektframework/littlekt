package com.lehaine.littlekt.graphics.shader.builder.delegate

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class AttributeDelegate<T : Variable>(private val factory: (ShaderBuilder) -> T) {
    private lateinit var v: T

    operator fun provideDelegate(thisRef: ShaderBuilder,
                                 property: KProperty<*>
    ): AttributeDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: ShaderBuilder, property: KProperty<*>): T {
        thisRef.attributes.add("${v.typeName} ${property.name}")
        return v
    }
}