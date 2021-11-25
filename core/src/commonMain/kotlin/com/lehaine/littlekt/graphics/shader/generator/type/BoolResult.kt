package com.lehaine.littlekt.graphics.shader.generator.type

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
data class BoolResult(val value: String) {
    infix fun or(a: BoolResult): BoolResult = BoolResult("(${this.value} || ${a.value})")
    infix fun and(a: BoolResult): BoolResult = BoolResult("(${this.value} && ${a.value})")
}