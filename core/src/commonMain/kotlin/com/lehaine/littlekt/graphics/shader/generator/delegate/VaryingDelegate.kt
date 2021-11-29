package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class VaryingDelegate<T : Variable>(private val factory: (GlslGenerator) -> T, private val precision: Precision) {

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
        v.builder.varyings.add("${precision.value}${v.typeName} ${property.name}")
        return v
    }

    operator fun setValue(thisRef: GlslGenerator, property: KProperty<*>, value: T) {
        v.builder.varyings.add("${precision.value}${v.typeName} ${property.name}")
        v.builder.instructions.add(Instruction.assign(property.name, value.value))
    }
}

class VaryingConstructorDelegate<T : Variable>(private val v: T, private val precision: Precision) {

    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): VaryingConstructorDelegate<T> {
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        v.builder.varyings.add("${precision.value}${v.typeName} ${property.name}")
        return v
    }
}