package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.InstructionType
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class FunctionDelegate<F : Func, T : Variable>(
    private val funcType: ((GlslGenerator) -> F),
    private val param1: ((GlslGenerator) -> T),
    private val body: (T) -> Unit
) {
    private lateinit var func: F
    private lateinit var v: T
    private lateinit var call: (T) -> Unit

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate<F, T> {
        func = funcType(thisRef)
        func.value = property.name
        v = param1(thisRef)
        v.value = "p1"
        call = {
            thisRef.addInstruction(Instruction(InstructionType.INVOKE_FUNC, "${func.value}(${it.value})"))
        }
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}(${v.typeName} ${v.value})"
            )
        )
        thisRef.addAsFunctionInstruction = true
        body(v)
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (T) -> Unit {
        return call
    }
}