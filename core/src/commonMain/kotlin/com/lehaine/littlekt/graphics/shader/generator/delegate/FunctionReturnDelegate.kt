package com.lehaine.littlekt.graphics.shader.generator.delegate

import com.lehaine.littlekt.graphics.shader.generator.Instruction
import com.lehaine.littlekt.graphics.shader.generator.InstructionType
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class FunctionReturnDelegate<T : Variable>(private val v: T, private val inlineValue: String? = null) {
    private var define: Instruction
    private var defined: Boolean = false

    init {
        val definitionString = "${v.typeName} {def} ${getInitializerExpr(inlineValue)}"
        define = Instruction(InstructionType.DEFINE, definitionString)
        v.builder.addInstruction(define)
    }

    val lit: T
        get() {
            v.builder.removeInstruction(define)
            v.value = inlineValue
            return v
        }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): FunctionReturnDelegate<T> {
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!defined) {
            define.result = define.result.replace("{def}", property.name)
            defined = true
        }
        return v
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!defined) {
            define.result = define.result.replace("{def}", property.name)
            defined = true
        }
        v.builder.addInstruction(Instruction.assign(property.name, value.value))
    }

    private fun getInitializerExpr(initialValue: String?): String {
        return if (initialValue == null) "" else " = $initialValue"
    }
}