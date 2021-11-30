package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.InstructionType
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.func.Void
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/29/2021
 */

class FunctionVoidDelegate(
    private val func: Void,
    private val body: () -> Unit
) {
    private lateinit var call: () -> Unit

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionVoidDelegate {
        func.value = property.name
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}()"
            )
        )
        thisRef.addAsFunctionInstruction = true
        body()
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )
        call = {
            thisRef.addInstruction(Instruction(InstructionType.INVOKE_FUNC, "${func.value}()"))
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): () -> Unit {
        return call
    }
}

class FunctionVoidDelegate1<P1 : Variable>(
    private val func: Void,
    private val param1: ((GlslGenerator) -> P1),
    private val body: (P1) -> Unit
) {
    private lateinit var v: P1
    private lateinit var call: (P1) -> Unit

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionVoidDelegate1<P1> {
        func.value = property.name
        v = param1(thisRef)
        v.value = "p1"
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
        call = {
            thisRef.addInstruction(Instruction(InstructionType.INVOKE_FUNC, "${func.value}(${it.value})"))
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (P1) -> Unit {
        return call
    }
}

class FunctionVoidDelegate2<P1 : Variable, P2 : Variable>(
    private val func: Void,
    private val param1: ((GlslGenerator) -> P1),
    private val param2: ((GlslGenerator) -> P2),
    private val body: (P1, P2) -> Unit
) {
    private lateinit var p1: P1
    private lateinit var p2: P2
    private lateinit var call: (P1, P2) -> Unit

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionVoidDelegate2<P1, P2> {
        func.value = property.name
        p1 = param1(thisRef)
        p1.value = "p1"
        p2 = param2(thisRef)
        p2.value = "p2"

        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}(${p1.typeName} ${p1.value}, ${p2.typeName} ${p2.value})"
            )
        )
        thisRef.addAsFunctionInstruction = true
        body(p1, p2)
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )

        call = { a, b ->
            thisRef.addInstruction(Instruction(InstructionType.INVOKE_FUNC, "${func.value}(${a.value}, ${b.value})"))
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (P1, P2) -> Unit {
        return call
    }
}