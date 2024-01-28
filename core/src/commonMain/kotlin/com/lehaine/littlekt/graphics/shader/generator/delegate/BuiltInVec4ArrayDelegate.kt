package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4Array
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class BuiltInVec4ArrayDelegate {
    private lateinit var va: Vec4Array

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>,
    ): BuiltInVec4ArrayDelegate {
        va = Vec4Array(thisRef, property.name)
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): Vec4Array {
        return va
    }

    operator fun setValue(thisRef: GlslGenerator, property: KProperty<*>, value: Vec4) {
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}