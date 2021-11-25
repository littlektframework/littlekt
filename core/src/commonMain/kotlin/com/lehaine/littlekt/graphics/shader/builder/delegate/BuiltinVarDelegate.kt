package com.lehaine.littlekt.graphics.shader.builder.delegate

import com.lehaine.littlekt.graphics.shader.builder.Instruction
import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.vec.Vec4
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class BuiltinVarDelegate {
    private lateinit var v: Vec4

    operator fun provideDelegate(
        thisRef: ShaderBuilder,
        property: KProperty<*>
    ): BuiltinVarDelegate {
        v = Vec4(thisRef, property.name)
        return this
    }

    operator fun getValue(thisRef: ShaderBuilder, property: KProperty<*>): Vec4 {
        return v
    }

    operator fun setValue(thisRef: ShaderBuilder, property: KProperty<*>, value: Vec4) {
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}