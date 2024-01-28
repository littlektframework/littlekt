package com.lehaine.littlekt.graphics.shader.generator.type

import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class ArrayItemDelegate<T : Variable>(
    private val index: Int,
    private val factory: (builder: GlslGenerator) -> T
) {
    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: Variable,
        property: KProperty<*>
    ): ArrayItemDelegate<T> {
        v = factory(thisRef.builder)
        return this
    }

    operator fun getValue(thisRef: Variable, property: KProperty<*>): T {
        if (v.value == null) {
            v.value = "${thisRef.value}[$index]"
        }
        return v
    }

    operator fun setValue(thisRef: Variable, property: KProperty<*>, value: T) {
        if (v.value == null) {
            v.value = "${thisRef.value}[$index]"
        }
        thisRef.builder.instructions.add(Instruction.assign(v.value, value.value))
    }
}