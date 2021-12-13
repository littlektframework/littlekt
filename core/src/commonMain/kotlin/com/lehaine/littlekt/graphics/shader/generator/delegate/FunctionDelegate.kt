package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.InstructionType
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.GenType
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/29/2021
 */

class FunctionDelegate<RT : GenType, F : Func<RT>>(
    private val funcType: ((GlslGenerator) -> F),
    private val body: () -> RT
) {
    private lateinit var func: F
    private lateinit var call: () -> RT

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate<RT, F> {
        func = funcType(thisRef)
        func.value = property.name
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}()"
            )
        )
        thisRef.addAsFunctionInstruction = true
        val result = body()
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )
        call = {
            thisRef.addInstruction(Instruction(InstructionType.INVOKE_FUNC, "${func.value}()"))
            result
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): () -> RT {
        return call
    }
}

class FunctionDelegate1<RT : GenType, F : Func<RT>, P1 : Variable>(
    private val funcType: ((GlslGenerator) -> F),
    private val param1: ((GlslGenerator) -> P1),
    private val body: (P1) -> RT
) {
    private lateinit var func: F
    private lateinit var v: P1
    private lateinit var call: (P1) -> FunctionReturnDelegate<RT>

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate1<RT, F, P1> {
        func = funcType(thisRef)
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
        val result = body(v)
        thisRef.functionInstructions.add(Instruction(InstructionType.DEFINE, "return ${result.value}"))

        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )
        call = {
            FunctionReturnDelegate(result, "${func.value}(${it.value})")
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (P1) -> FunctionReturnDelegate<RT> {
        return call
    }
}

class FunctionDelegate2<RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable>(
    private val funcType: ((GlslGenerator) -> F),
    private val param1: ((GlslGenerator) -> P1),
    private val param2: ((GlslGenerator) -> P2),
    private val body: (P1, P2) -> RT
) {
    private lateinit var func: F
    private lateinit var p1: P1
    private lateinit var p2: P2
    private lateinit var call: (P1, P2) -> FunctionReturnDelegate<RT>

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate2<RT, F, P1, P2> {
        func = funcType(thisRef)
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
        val result = body(p1, p2)
        thisRef.functionInstructions.add(Instruction(InstructionType.DEFINE, "return ${result.value}"))
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )

        call = { a, b ->
            FunctionReturnDelegate(result, "${func.value}(${a.value}, ${b.value})")
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (P1, P2) -> FunctionReturnDelegate<RT> {
        return call
    }
}

class FunctionDelegate3<RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable, P3 : Variable>(
    private val funcType: ((GlslGenerator) -> F),
    private val param1: ((GlslGenerator) -> P1),
    private val param2: ((GlslGenerator) -> P2),
    private val param3: ((GlslGenerator) -> P3),
    private val body: (P1, P2, P3) -> RT
) {
    private lateinit var func: F
    private lateinit var p1: P1
    private lateinit var p2: P2
    private lateinit var p3: P3
    private lateinit var call: (P1, P2, P3) -> FunctionReturnDelegate<RT>

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate3<RT, F, P1, P2, P3> {
        func = funcType(thisRef)
        func.value = property.name
        p1 = param1(thisRef)
        p1.value = "p1"
        p2 = param2(thisRef)
        p2.value = "p2"
        p3 = param3(thisRef)
        p3.value = "p3"

        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}(${p1.typeName} ${p1.value}, ${p2.typeName} ${p2.value}, ${p3.typeName} ${p3.value})"
            )
        )
        thisRef.addAsFunctionInstruction = true
        val result = body(p1, p2, p3)
        thisRef.functionInstructions.add(Instruction(InstructionType.DEFINE, "return ${result.value}"))
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )

        call = { a, b, c ->
            FunctionReturnDelegate(result, "${func.value}(${a.value}, ${b.value}, ${c.value})")
        }

        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): (P1, P2, P3) -> FunctionReturnDelegate<RT> {
        return call
    }
}

class FunctionDelegate4<RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable, P3 : Variable, P4 : Variable>(
    private val funcType: ((GlslGenerator) -> F),
    private val param1: ((GlslGenerator) -> P1),
    private val param2: ((GlslGenerator) -> P2),
    private val param3: ((GlslGenerator) -> P3),
    private val param4: ((GlslGenerator) -> P4),
    private val body: (P1, P2, P3, P4) -> RT
) {
    private lateinit var func: F
    private lateinit var p1: P1
    private lateinit var p2: P2
    private lateinit var p3: P3
    private lateinit var p4: P4
    private lateinit var call: (P1, P2, P3, P4) -> FunctionReturnDelegate<RT>

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): FunctionDelegate4<RT, F, P1, P2, P3, P4> {
        func = funcType(thisRef)
        func.value = property.name
        p1 = param1(thisRef)
        p1.value = "p1"
        p2 = param2(thisRef)
        p2.value = "p2"
        p3 = param3(thisRef)
        p3.value = "p3"
        p4 = param4(thisRef)
        p4.value = "p4"

        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.FUNC_DEFINED,
                "${func.typeName} ${func.value}(${p1.typeName} ${p1.value}, ${p2.typeName} ${p2.value}, ${p3.typeName} ${p3.value}, ${p4.typeName} ${p4.value})"
            )
        )
        thisRef.addAsFunctionInstruction = true
        val result = body(p1, p2, p3, p4)
        thisRef.functionInstructions.add(Instruction(InstructionType.DEFINE, "return ${result.value}"))
        thisRef.addAsFunctionInstruction = false
        thisRef.functionInstructions.add(
            Instruction(
                InstructionType.END_FUNC
            )
        )

        call = { a, b, c, d ->
            FunctionReturnDelegate(result, "${func.value}(${a.value}, ${b.value}, ${c.value}, ${d.value})")
        }

        return this
    }

    operator fun getValue(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): (P1, P2, P3, P4) -> FunctionReturnDelegate<RT> {
        return call
    }
}