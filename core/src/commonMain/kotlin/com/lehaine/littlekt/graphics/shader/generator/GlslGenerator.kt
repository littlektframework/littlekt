package com.lehaine.littlekt.graphics.shader.generator

import com.lehaine.littlekt.graphics.shader.Shader
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.InstructionType.*
import com.lehaine.littlekt.graphics.shader.generator.delegate.*
import com.lehaine.littlekt.graphics.shader.generator.type.BoolResult
import com.lehaine.littlekt.graphics.shader.generator.type.GenType
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat3
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2DArray
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.ShadowTexture2D
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4


/**
 * @author Colton Daily
 * @date 11/25/2021
 */
enum class InstructionType {
    DEFINE,
    ASSIGN,
    IF,
    ELSEIF,
    ELSE,
    ENDIF,
    DISCARD
}

data class Instruction(val type: InstructionType, var result: String = "") {
    companion object {
        fun assign(left: String?, right: String?): Instruction {
            return Instruction(ASSIGN, "$left = $right")
        }
    }
}

interface GlslProvider {
    fun generate(): String
}

abstract class GlslGenerator : GlslProvider, Shader {
    val uniforms = mutableSetOf<String>()
    val attributes = mutableSetOf<String>()
    val varyings = mutableSetOf<String>()
    override val parameters = mutableListOf<ShaderParameter>()
    val instructions = mutableListOf<Instruction>()

    var gl_Position by BuiltinVarDelegate()
    var gl_FragCoord by BuiltinVarDelegate()
    var gl_FragColor by BuiltinVarDelegate()

    override fun generate(): String {
        removeUnusedDefinitions()

        val sb = StringBuilder()
        uniforms.forEach {
            sb.appendLine("uniform $it;")
        }
        attributes.forEach {
            sb.appendLine("attribute $it;")
        }
        varyings.forEach {
            sb.appendLine("\nvarying $it;")
        }

        sb.appendLine("void main(void) {")
        instructions.forEach {
            val instructionString = when (it.type) {
                DEFINE, ASSIGN -> "${it.result};"
                IF -> {
                    "if (${it.result}) {"
                }
                ELSEIF -> {
                    "else if (${it.result}) {"
                }
                ELSE -> {
                    "else {"
                }
                ENDIF -> "}"
                DISCARD -> "discard;"
            }
            sb.appendLine(instructionString)
        }
        sb.appendLine("}")
        return sb.toString()
    }

    fun appendComponent(builder: GlslGeneratorComponent) {
        uniforms.addAll(builder.uniforms)
        attributes.addAll(builder.attributes)
        varyings.addAll(builder.varyings)
        instructions.addAll(builder.instructions)
    }

    private fun removeUnusedDefinitions() {
        instructions.removeAll { it.result.contains("{def}") }
    }

    fun <T : Variable> varying(factory: (GlslGenerator) -> T) = VaryingDelegate(factory)
    fun <T : Variable> attribute(factory: (GlslGenerator) -> T) = AttributeDelegate(factory)
    fun <T : Variable> uniform(factory: (GlslGenerator) -> T) = UniformDelegate(factory)
    fun <T : Variable> uniformArray(size: Int, init: (builder: GlslGenerator) -> T) =
        UniformArrayDelegate(size, init)

    fun <T : Variable> samplersArray(size: Int) = UniformArrayDelegate(size, ::Sampler2DArray)

    fun discard() = instructions.add(Instruction(DISCARD))

    protected inline fun If(condition: BoolResult, body: () -> Unit) {
        instructions.add(Instruction(IF, condition.value))
        body()
        instructions.add(Instruction(ENDIF))
    }

    protected inline fun ElseIf(condition: BoolResult, body: () -> Unit) {
        instructions.add(Instruction(ELSEIF, condition.value))
        body()
        instructions.add(Instruction(ENDIF))
    }

    protected inline fun Else(body: () -> Unit) {
        instructions.add(Instruction(ELSE))
        body()
        instructions.add(Instruction(ENDIF))
    }

    fun castMat3(m: Mat4) = Mat3(this, "mat3(${m.value})")
    fun int(v: GLFloat) = GLInt(this, "int(${v.value})")

    fun radians(v: GLFloat) = GLFloat(this, "radians(${v.value})")
    fun radians(v: Vec2) = Vec2(this, "radians(${v.value})")
    fun radians(v: Vec3) = Vec3(this, "radians(${v.value})")
    fun radians(v: Vec4) = Vec4(this, "radians(${v.value})")

    fun degrees(v: GLFloat) = GLFloat(this, "degrees(${v.value})")
    fun degrees(v: Vec2) = Vec2(this, "degrees(${v.value})")
    fun degrees(v: Vec3) = Vec3(this, "degrees(${v.value})")
    fun degrees(v: Vec4) = Vec4(this, "degrees(${v.value})")

    fun sin(v: GLFloat) = GLFloat(this, "sin(${v.value})")
    fun sin(v: Vec2) = Vec2(this, "sin(${v.value})")
    fun sin(v: Vec3) = Vec3(this, "sin(${v.value})")
    fun sin(v: Vec4) = Vec4(this, "sin(${v.value})")

    fun cos(v: GLFloat) = GLFloat(this, "cos(${v.value})")
    fun cos(v: Vec2) = Vec2(this, "cos(${v.value})")
    fun cos(v: Vec3) = Vec3(this, "cos(${v.value})")
    fun cos(v: Vec4) = Vec4(this, "cos(${v.value})")

    fun tan(v: GLFloat) = GLFloat(this, "tan(${v.value})")
    fun tan(v: Vec2) = Vec2(this, "tan(${v.value})")
    fun tan(v: Vec3) = Vec3(this, "tan(${v.value})")
    fun tan(v: Vec4) = Vec4(this, "tan(${v.value})")

    fun acos(v: GLFloat) = GLFloat(this, "acos(${v.value})")
    fun acos(v: Vec2) = Vec2(this, "acos(${v.value})")
    fun acos(v: Vec3) = Vec3(this, "acos(${v.value})")
    fun acos(v: Vec4) = Vec4(this, "acos(${v.value})")

    fun atan(v: GLFloat) = GLFloat(this, "atan(${v.value})")
    fun atan(v: Vec2) = Vec2(this, "atan(${v.value})")
    fun atan(v: Vec3) = Vec3(this, "atan(${v.value})")
    fun atan(v: Vec4) = Vec4(this, "atan(${v.value})")

    fun exp(v: GLFloat) = GLFloat(this, "exp(${v.value})")
    fun exp(v: Vec2) = Vec2(this, "exp(${v.value})")
    fun exp(v: Vec3) = Vec3(this, "exp(${v.value})")
    fun exp(v: Vec4) = Vec4(this, "exp(${v.value})")

    fun log(v: GLFloat) = GLFloat(this, "log(${v.value})")
    fun log(v: Vec2) = Vec2(this, "log(${v.value})")
    fun log(v: Vec3) = Vec3(this, "log(${v.value})")
    fun log(v: Vec4) = Vec4(this, "log(${v.value})")

    fun exp2(v: GLFloat) = GLFloat(this, "exp2(${v.value})")
    fun exp2(v: Vec2) = Vec2(this, "exp2(${v.value})")
    fun exp2(v: Vec3) = Vec3(this, "exp2(${v.value})")
    fun exp2(v: Vec4) = Vec4(this, "exp2(${v.value})")

    fun log2(v: GLFloat) = GLFloat(this, "log2(${v.value})")
    fun log2(v: Vec2) = Vec2(this, "log2(${v.value})")
    fun log2(v: Vec3) = Vec3(this, "log2(${v.value})")
    fun log2(v: Vec4) = Vec4(this, "log2(${v.value})")

    fun sqrt(v: GLFloat) = GLFloat(this, "sqrt(${v.value})")
    fun sqrt(v: Vec2) = Vec2(this, "sqrt(${v.value})")
    fun sqrt(v: Vec3) = Vec3(this, "sqrt(${v.value})")
    fun sqrt(v: Vec4) = Vec4(this, "sqrt(${v.value})")

    fun inversesqrt(v: GLFloat) = GLFloat(this, "inversesqrt(${v.value})")
    fun inversesqrt(v: Vec2) = Vec2(this, "inversesqrt(${v.value})")
    fun inversesqrt(v: Vec3) = Vec3(this, "inversesqrt(${v.value})")
    fun inversesqrt(v: Vec4) = Vec4(this, "inversesqrt(${v.value})")

    fun abs(v: GLFloat) = GLFloat(this, "abs(${v.value})")
    fun abs(v: Vec2) = Vec2(this, "abs(${v.value})")
    fun abs(v: Vec3) = Vec3(this, "abs(${v.value})")
    fun abs(v: Vec4) = Vec4(this, "abs(${v.value})")

    fun sign(v: GLFloat) = GLFloat(this, "sign(${v.value})")
    fun sign(v: Vec2) = Vec2(this, "sign(${v.value})")
    fun sign(v: Vec3) = Vec3(this, "sign(${v.value})")
    fun sign(v: Vec4) = Vec4(this, "sign(${v.value})")

    fun floor(v: GLFloat) = GLFloat(this, "floor(${v.value})")
    fun floor(v: Vec2) = Vec2(this, "floor(${v.value})")
    fun floor(v: Vec3) = Vec3(this, "floor(${v.value})")
    fun floor(v: Vec4) = Vec4(this, "floor(${v.value})")

    fun ceil(v: GLFloat) = GLFloat(this, "ceil(${v.value})")
    fun ceil(v: Vec2) = Vec2(this, "ceil(${v.value})")
    fun ceil(v: Vec3) = Vec3(this, "ceil(${v.value})")
    fun ceil(v: Vec4) = Vec4(this, "ceil(${v.value})")

    fun fract(v: GLFloat) = GLFloat(this, "fract(${v.value})")
    fun fract(v: Vec2) = Vec2(this, "fract(${v.value})")
    fun fract(v: Vec3) = Vec3(this, "fract(${v.value})")
    fun fract(v: Vec4) = Vec4(this, "fract(${v.value})")

    fun mod(v: GLFloat, base: GLFloat) = GLFloat(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec2, base: Vec2) = Vec2(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec3, base: Vec3) = Vec3(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec4, base: Vec4) = Vec4(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec2, base: GLFloat) = Vec2(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec3, base: GLFloat) = Vec3(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec4, base: GLFloat) = Vec4(this, "mod(${v.value}, ${base.value})")

    fun min(v: GLFloat, base: GLFloat) = GLFloat(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: Vec2) = Vec2(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec3, base: Vec3) = Vec3(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec4, base: Vec4) = Vec4(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: GLFloat) = Vec2(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec3, base: GLFloat) = Vec3(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec4, base: GLFloat) = Vec4(this, "min(${v.value}, ${base.value})")

    fun max(v: GLFloat, v2: Float) = GLFloat(this, "max(${v.value}, ${v2.str()})")
    fun max(v: GLFloat, base: GLFloat) = GLFloat(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: Vec2) = Vec2(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec3, base: Vec3) = Vec3(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec4, base: Vec4) = Vec4(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: GLFloat) = Vec2(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec3, base: GLFloat) = Vec3(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec4, base: GLFloat) = Vec4(this, "max(${v.value}, ${base.value})")

    fun clamp(v: GLFloat, min: GLFloat, max: GLFloat) =
        GLFloat(this, "clamp(${v.value}, ${min.value}, ${max.value})")

    fun clamp(v: Vec2, min: Vec2, max: Vec2) = Vec2(this, "clamp(${v.value}, ${min.value}, ${max.value})")
    fun clamp(v: Vec3, min: Vec3, max: Vec3) = Vec3(this, "clamp(${v.value}, ${min.value}, ${max.value})")
    fun clamp(v: Vec4, min: Vec4, max: Vec4) = Vec4(this, "clamp(${v.value}, ${min.value}, ${max.value})")
    fun clamp(v: Vec2, min: GLFloat, max: GLFloat) =
        Vec2(this, "clamp(${v.value}, ${min.value}, ${max.value})")

    fun clamp(v: Vec3, min: GLFloat, max: GLFloat) =
        Vec3(this, "clamp(${v.value}, ${min.value}, ${max.value})")

    fun clamp(v: Vec4, min: GLFloat, max: GLFloat) =
        Vec4(this, "clamp(${v.value}, ${min.value}, ${max.value})")

    fun clamp(v: GLFloat, min: Float, max: Float) =
        GLFloat(this, "clamp(${v.value}, ${min.str()}, ${max.str()})")

    fun clamp(v: GLFloat, min: GLFloat, max: Float) =
        GLFloat(this, "clamp(${v.value}, ${min.value}, ${max.str()})")

    fun clamp(v: Vec2, min: Float, max: Float) = Vec2(this, "clamp(${v.value}, ${min.str()}, ${max.str()})")
    fun clamp(v: Vec3, min: Float, max: Float) = Vec3(this, "clamp(${v.value}, ${min.str()}, ${max.str()})")
    fun clamp(v: Vec4, min: Float, max: Float) = Vec4(this, "clamp(${v.value}, ${min.str()}, ${max.str()})")

    fun mix(v: GLFloat, y: GLFloat, a: GLFloat) = GLFloat(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec2, y: Vec2, a: Vec2) = Vec2(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec3, y: Vec3, a: Vec3) = Vec3(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec4, y: Vec4, a: Vec4) = Vec4(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec2, y: Vec2, a: GLFloat) = Vec2(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec3, y: Vec3, a: GLFloat) = Vec3(this, "mix(${v.value}, ${y.value}, ${a.value})")
    fun mix(v: Vec4, y: Vec4, a: GLFloat) = Vec4(this, "mix(${v.value}, ${y.value}, ${a.value})")

    fun step(v: GLFloat, x: GLFloat) = GLFloat(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec2, x: Vec2) = Vec2(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec3, x: Vec3) = Vec3(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec4, x: Vec4) = Vec4(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec2, x: GLFloat) = Vec2(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec3, x: GLFloat) = Vec3(this, "step(${v.value}, ${x.value})")
    fun step(v: Vec4, x: GLFloat) = Vec4(this, "step(${v.value}, ${x.value})")

    fun pow(v: GLFloat, x: Float) = GLFloat(this, "pow(${v.value}, ${x.str()})")

    fun smoothstep(v: GLFloat, u: GLFloat, x: GLFloat) =
        GLFloat(this, "smoothstep(${v.value}, ${u.value}, ${x.value})")

    fun smoothstep(v: Vec2, u: Vec2, x: Vec2) = Vec2(this, "smoothstep(${v.value}, ${u.value}, ${x.value})")
    fun smoothstep(v: Vec3, u: Vec3, x: Vec3) = Vec3(this, "smoothstep(${v.value}, ${u.value}, ${x.value})")
    fun smoothstep(v: Vec4, u: Vec4, x: Vec4) = Vec4(this, "smoothstep(${v.value}, ${u.value}, ${x.value})")

    fun length(v: GenType) = GLFloat(this, "length(${v.value})")
    fun distance(a: GenType, b: GenType) = GLFloat(this, "distance(${a.value}, ${b.value})")
    fun dot(a: GenType, b: GenType) = GLFloat(this, "dot(${a.value}, ${b.value})")
    fun cross(a: Vec3, b: Vec3) = Vec3(this, "dot(${a.value}, ${b.value})")
    fun normalize(v: GLFloat) = GLFloat(this, "normalize(${v.value})")
    fun normalize(v: Vec3) = Vec3(this, "normalize(${v.value})")
    fun normalize(v: Vec4) = Vec4(this, "normalize(${v.value})")
    fun reflect(i: GenType, n: GenType) = Vec3(this, "reflect(${i.value}, ${n.value})")
    fun refract(i: GenType, n: GenType, eta: GLFloat) =
        Vec3(this, "refract(${i.value}, ${n.value}, ${eta.value})")

    fun shadow2D(sampler: ShadowTexture2D, v: Vec2) = Vec4(this, "shadow2D(${sampler.value}, ${v.value})")
    fun texture2D(sampler: Sampler2D, v: Vec2) = Vec4(this, "texture2D(${sampler.value}, ${v.value})")

    fun float() = ConstructorDelegate(GLFloat(this))
    fun float(x: Float) = ConstructorDelegate(GLFloat(this), x.str())
    fun float(x: GLFloat) = ConstructorDelegate(GLFloat(this), x.value)

    fun intVal() = ConstructorDelegate(GLInt(this))
    fun intVal(x: GLInt) = ConstructorDelegate(GLInt(this), x.value)
    fun intVal(x: Int) = ConstructorDelegate(GLInt(this), "$x")

    fun vec2() = ConstructorDelegate(Vec2(this))
    fun vec2(x: Vec2) = ConstructorDelegate(Vec2(this), "${x.value}")
    fun vec2(x: Float, y: Float) = ConstructorDelegate(Vec2(this), "vec2(${x.str()}, ${y.str()})")
    fun vec2(x: GLFloat, y: Float) = ConstructorDelegate(Vec2(this), "vec2(${x.value}, ${y.str()})")
    fun vec2(x: Float, y: GLFloat) = ConstructorDelegate(Vec2(this), "vec2(${x.str()}, ${y.value})")
    fun vec2(x: GLFloat, y: GLFloat) = ConstructorDelegate(Vec2(this), "vec2(${x.value}, ${y.value})")

    fun vec3() = ConstructorDelegate(Vec3(this))
    fun vec3(x: GLFloat, y: GLFloat, z: GLFloat) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.value}, ${y.value}, ${z.value})"))

    fun vec3(x: GLFloat, y: GLFloat, z: Float) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.value}, ${y.value}, ${z.str()})"))

    fun vec3(x: GLFloat, y: Float, z: GLFloat) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.value}, ${y.str()}, ${z.value})"))

    fun vec3(x: GLFloat, y: Float, z: Float) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.value}, ${y.str()}, ${z.str()})"))

    fun vec3(x: Float, y: GLFloat, z: GLFloat) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.str()}, ${y.value}, ${z.value})"))

    fun vec3(x: Float, y: GLFloat, z: Float) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.str()}, ${y.value}, ${z.str()})"))

    fun vec3(x: Float, y: Float, z: GLFloat) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.str()}, ${y.str()}, ${z.value})"))

    fun vec3(x: Float, y: Float, z: Float) =
        ConstructorDelegate(Vec3(this), ("vec3(${x.str()}, ${y.str()}, ${z.str()})"))

    fun vec3(v2: Vec2, z: Float) = ConstructorDelegate(Vec3(this), ("vec3(${v2.value}, ${z.str()})"))
    fun vec3(v2: Vec2, z: GLFloat) = ConstructorDelegate(Vec3(this), ("vec3(${v2.value}, ${z.value})"))
    fun vec3(x: Float, v2: Vec2) = ConstructorDelegate(Vec3(this), ("vec3(${x.str()}, ${v2.value})"))
    fun vec3(x: GLFloat, v2: Vec2) = ConstructorDelegate(Vec3(this), ("vec3(${x.value}, ${v2.value})"))

    fun vec4() = ConstructorDelegate(Vec4(this))
    fun vec4(vec3: Vec3, w: Float) = ConstructorDelegate(Vec4(this), ("vec4(${vec3.value}, ${w.str()})"))
    fun vec4(vec3: Vec3, w: GLFloat) = ConstructorDelegate(Vec4(this), ("vec4(${vec3.value}, ${w.value})"))
    fun vec4(vec2: Vec2, z: Float, w: Float) =
        ConstructorDelegate(Vec4(this), ("vec4(${vec2.value}, ${z.str()}, ${w.str()})"))

    fun vec4(x: GLFloat, y: GLFloat, zw: Vec2) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.value}, ${y.value}, ${zw.value})"))

    fun vec4(x: Float, y: Float, z: Float, w: Float) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.str()}, ${y.str()}, ${z.str()}, ${w.str()})"))

    fun vec4(x: Float, y: Float, z: Float, w: GLFloat) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.str()}, ${y.str()}, ${z.str()}, ${w.value})"))

    fun vec4(x: GLFloat, y: GLFloat, z: GLFloat, w: GLFloat) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.value}, ${y.value}, ${z.value}, ${w.value})"))

    fun vec4(x: GLFloat, y: GLFloat, z: GLFloat, w: Float) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.value}, ${y.value}, ${z.value}, ${w.str()})"))

    fun vec4(x: GLFloat, y: GLFloat, z: Float, w: Float) =
        ConstructorDelegate(Vec4(this), ("vec4(${x.value}, ${y.value}, ${z.str()}, ${w.str()})"))

    fun mat3() = ConstructorDelegate(Mat3(this))

    operator fun Float.minus(a: GLFloat) = GLFloat(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.plus(a: GLFloat) = GLFloat(a.builder, "(${this.str()} + ${a.value})")
}

fun Float.str(): String {
    val r = "$this"
    return if (r.contains(".")) r else "$r.0"
}

abstract class GlslGeneratorComponent : GlslGenerator()