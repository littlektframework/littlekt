package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class ComponentDelegate<T : Variable>(private val factory: (GlslGenerator) -> T) {
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
        thisRef.builder.addInstruction(Instruction.assign(v.value, value.value))
    }
}