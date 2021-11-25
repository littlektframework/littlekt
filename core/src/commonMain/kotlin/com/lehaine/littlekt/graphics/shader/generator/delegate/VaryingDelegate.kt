package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class VaryingDelegate<T : Variable>(private val factory: (GlslGenerator) -> T) {

    private lateinit var v: T

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): VaryingDelegate<T> {
        v = factory(thisRef)
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): T {
        thisRef.varyings.add("${v.typeName} ${property.name}")
        return v
    }

    operator fun setValue(thisRef: GlslGenerator, property: KProperty<*>, value: T) {
        thisRef.varyings.add("${v.typeName} ${property.name}")
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}