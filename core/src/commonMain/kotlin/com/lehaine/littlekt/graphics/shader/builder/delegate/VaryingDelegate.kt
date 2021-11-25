package com.lehaine.littlekt.graphics.shader.builder.delegate

import com.lehaine.littlekt.graphics.shader.builder.Instruction
import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class VaryingDelegate<T : Variable>(private val factory: (ShaderBuilder) -> T) {

    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: ShaderBuilder,
        property: KProperty<*>
    ): VaryingDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: ShaderBuilder, property: KProperty<*>): T {
        thisRef.varyings.add("${v.typeName} ${property.name}")
        return v
    }

    operator fun setValue(thisRef: ShaderBuilder, property: KProperty<*>, value: T) {
        thisRef.varyings.add("${v.typeName} ${property.name}")
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}