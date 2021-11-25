package com.lehaine.littlekt.graphics.shader.builder.delegate

import com.lehaine.littlekt.graphics.shader.builder.Instruction
import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class ComponentDelegate<T : Variable>(private val factory: (ShaderBuilder) -> T) {
    private lateinit var v: T

    operator fun provideDelegate(thisRef: Variable, property: KProperty<*>): ComponentDelegate<T> {
        v = factory(thisRef.builder)
        return this
    }

    operator fun getValue(thisRef: Variable, property: KProperty<*>): T {
        if (v.value == null) {
            v.value = "${thisRef.value}.${property.name}"
        }
        return v
    }

    operator fun setValue(thisRef: Variable, property: KProperty<*>, value: T) {
        if (v.value == null) {
            v.value = "${thisRef.value}.${property.name}"
        }
        thisRef.builder.instructions.add(Instruction.assign(v.value, value.value))
    }
}