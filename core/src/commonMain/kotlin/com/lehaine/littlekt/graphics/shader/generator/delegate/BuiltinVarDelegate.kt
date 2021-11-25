package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class BuiltinVarDelegate {
    private lateinit var v: Vec4

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): BuiltinVarDelegate {
        v = Vec4(thisRef, property.name)
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): Vec4 {
        return v
    }

    operator fun setValue(thisRef: GlslGenerator, property: KProperty<*>, value: Vec4) {
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}