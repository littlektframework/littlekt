package com.lehaine.littlekt.graphics.shader.generator

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.InstructionType.*
import com.lehaine.littlekt.graphics.shader.generator.delegate.*
import com.lehaine.littlekt.graphics.shader.generator.type.Bool
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.GenType
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat2
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat3
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2DArray
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2DVarArray
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.ShadowTexture2D
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass
import com.lehaine.littlekt.graphics.shader.generator.type.func.Void as FuncVoid


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
    DISCARD,
    FUNC_DEFINED,
    INVOKE_FUNC,
    END_FUNC,
    FOR,
    END_FOR
}

data class Instruction(val type: InstructionType, var result: String = "") {
    companion object {
        fun assign(left: String?, right: String?): Instruction {
            return Instruction(ASSIGN, "$left = $right")
        }
    }
}

@JvmInline
value class Precision(val value: String) {
    companion object {
        val DEFAULT = Precision("")
        val LOW = Precision("lowp ")
        val MEDIUM = Precision("mediump ")
        val HIGH = Precision("highp ")
    }
}

interface GlslProvider {
    fun generate(context: Context): String
}

abstract class GlslGenerator : GlslProvider {
    internal val uniforms = mutableSetOf<String>()
    internal val attributes = mutableSetOf<String>()
    internal val varyings = mutableSetOf<String>()

    internal var addAsFunctionInstruction = false

    @PublishedApi
    internal val functionInstructions = mutableListOf<Instruction>()

    @PublishedApi
    internal val instructions = mutableListOf<Instruction>()

    open val parameters = mutableListOf<ShaderParameter>()


    @PublishedApi
    internal fun addInstruction(instruction: Instruction) {
        if (addAsFunctionInstruction) {
            functionInstructions.add(instruction)
        } else {
            instructions.add(instruction)
        }
    }

    @PublishedApi
    internal fun removeInstruction(instruction: Instruction) {
        if (addAsFunctionInstruction) {
            functionInstructions.remove(instruction)
        } else {
            instructions.remove(instruction)
        }
    }


    override fun generate(context: Context): String {
        removeUnusedDefinitions()
        val sb = StringBuilder()
        val glVersion = context.gl.version
        if (context.graphics.isGL30) {
            val version = when {
                glVersion.platform == Context.Platform.WEBGL2
                        || glVersion.major >= 3 && glVersion.platform.isMobile -> "300 es"
                glVersion.major >= 3 && glVersion.minor >= 2 -> "150"
                glVersion.major >= 3 && !glVersion.platform.isWebGl -> "130"
                else -> throw IllegalStateException("${context.graphics.glVersion} isn't not considered at least GL 3.0+")
            }
            sb.appendLine("#version $version")
        }
        if (this is FragmentShader) {
            sb.run {
                appendLine("#ifdef GL_ES")
                appendLine("precision highp float;")
                appendLine("precision mediump int;")
                appendLine("#else")
                appendLine("#define lowp ")
                appendLine("#define mediump ")
                appendLine("#define highp ")
                appendLine("#endif")
            }
        } else {
            sb.run {
                appendLine("#ifndef GL_ES")
                appendLine("#define lowp ")
                appendLine("#define mediump ")
                appendLine("#define highp ")
                appendLine("#endif")
            }
        }
        uniforms.forEach {
            sb.appendLine("uniform $it;")
        }
        attributes.forEach {
            if (context.graphics.isGL30) {
                sb.appendLine("in $it;")
            } else {
                sb.appendLine("attribute $it;")
            }
        }
        varyings.forEach {
            if (context.graphics.isGL30) {
                if (this is FragmentShader) {
                    sb.appendLine("in $it;")
                } else {
                    sb.appendLine("out $it;")
                }
            } else {
                sb.appendLine("varying $it;")
            }
        }

        if (context.graphics.isGL30 && this is FragmentShader) {
            sb.appendLine("out lowp vec4 fragColor;")
        }

        functionInstructions.forEach {
            val instructionString = when (it.type) {
                DEFINE, ASSIGN, INVOKE_FUNC -> "${it.result};"
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
                FUNC_DEFINED -> "${it.result} {"
                END_FUNC -> "}"
                FOR -> {
                    "for (${it.result}) {"
                }
                END_FOR -> "}"
                else -> throw IllegalStateException("Instruction ${it.type} is not valid for the custom function instructions!")
            }
            sb.appendLine(instructionString)
        }

        sb.appendLine("void main(void) {")
        instructions.forEach {
            val instructionString = when (it.type) {
                DEFINE, ASSIGN, INVOKE_FUNC -> "${it.result};"
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
                FOR -> {
                    "for (${it.result}) {"
                }
                END_FOR -> "}"
                DISCARD -> "discard;"
                else -> throw IllegalStateException("Instruction ${it.type} is not valid for the main function instructions!")
            }
            sb.appendLine(instructionString)
        }
        sb.appendLine("}")

        var result = sb.toString()
        if (context.graphics.isGL30) {
            result = result.replace("texture2D\\(".toRegex(), "texture(")
                .replace("textureCube\\(".toRegex(), "texture(")
                .replace("gl_FragColor".toRegex(), "fragColor")
        }
        return result
    }

    fun ensureShaderVersionChanges(context: Context, source: String): String {
        val sb = StringBuilder()
        val glVersion = context.graphics.glVersion
        if (context.graphics.isGL30) {
            val version = when {
                glVersion.platform == Context.Platform.WEBGL2
                        || glVersion.major >= 3 && glVersion.platform.isMobile -> "300 es"
                glVersion.major >= 3 && glVersion.minor >= 2 -> "150"
                glVersion.major >= 3 && !glVersion.platform.isWebGl -> "130"
                else -> throw IllegalStateException("${context.graphics.glVersion} isn't not considered at least GL 3.0+")
            }
            sb.appendLine("#version $version")
        }
        if (this is FragmentShader) {
            sb.run {
                appendLine("#ifdef GL_ES")
                appendLine("precision highp float;")
                appendLine("precision mediump int;")
                appendLine("#else")
                appendLine("#define lowp ")
                appendLine("#define mediump ")
                appendLine("#define highp ")
                appendLine("#endif")
            }
            if (context.graphics.isGL30) {
                sb.appendLine("out lowp vec4 fragColor;")
            }
        } else {
            sb.run {
                appendLine("#ifndef GL_ES")
                appendLine("#define lowp ")
                appendLine("#define mediump ")
                appendLine("#define highp ")
                appendLine("#endif")
            }
        }
        sb.appendLine(source)
        var result = sb.toString()
        if (context.graphics.isGL30) {
            result = if (this is FragmentShader) {
                result.replace("varying ".toRegex(), "in ")
            } else {
                result.replace("varying ".toRegex(), "out ")
            }

            result = result.replace("attribute ".toRegex(), "in ")
                .replace("texture2D\\(".toRegex(), "texture(")
                .replace("textureCube\\(".toRegex(), "texture(")
                .replace("gl_FragColor".toRegex(), "fragColor")
        }
        return result
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

    fun <T : Variable> uniform(factory: (GlslGenerator) -> T, precision: Precision = Precision.DEFAULT) =
        UniformDelegate(factory, precision)

    @Suppress("UNCHECKED_CAST")
    fun <T : Variable> uniformCtr(
        clazz: KClass<T>,
        precision: Precision = Precision.DEFAULT
    ): UniformConstructorDelegate<T> =
        UniformConstructorDelegate(createVariable(clazz), precision) as UniformConstructorDelegate<T>

    fun <T : Variable> uniformArray(
        size: Int,
        init: (builder: GlslGenerator) -> T,
        precision: Precision = Precision.DEFAULT
    ) =
        UniformArrayDelegate(size, init, precision)

    fun <T : Variable> samplersArray(size: Int, precision: Precision = Precision.DEFAULT) =
        UniformArrayDelegate(size, ::Sampler2DVarArray, precision)

    fun <RT : GenType, F : Func<RT>> Func(
        funcFactory: (GlslGenerator) -> F,
        body: () -> RT
    ): FunctionDelegate<RT, F> = FunctionDelegate(funcFactory, body)

    fun <RT : GenType, F : Func<RT>, P1 : Variable> Func(
        funcFactory: (GlslGenerator) -> F,
        p1Factory: (GlslGenerator) -> P1,
        body: (p1: P1) -> RT
    ): FunctionDelegate1<RT, F, P1> = FunctionDelegate1(funcFactory, p1Factory, body)

    fun <RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable> Func(
        funcFactory: (GlslGenerator) -> F,
        p1Factory: (GlslGenerator) -> P1,
        p2Factory: (GlslGenerator) -> P2,
        body: (p1: P1, p2: P2) -> RT
    ): FunctionDelegate2<RT, F, P1, P2> = FunctionDelegate2(funcFactory, p1Factory, p2Factory, body)

    fun <RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable, P3 : Variable> Func(
        funcFactory: (GlslGenerator) -> F,
        p1Factory: (GlslGenerator) -> P1,
        p2Factory: (GlslGenerator) -> P2,
        p3Factory: (GlslGenerator) -> P3,
        body: (p1: P1, p2: P2, p3: P3) -> RT
    ): FunctionDelegate3<RT, F, P1, P2, P3> = FunctionDelegate3(funcFactory, p1Factory, p2Factory, p3Factory, body)

    fun <RT : GenType, F : Func<RT>, P1 : Variable, P2 : Variable, P3 : Variable, P4 : Variable> Func(
        funcFactory: (GlslGenerator) -> F,
        p1Factory: (GlslGenerator) -> P1,
        p2Factory: (GlslGenerator) -> P2,
        p3Factory: (GlslGenerator) -> P3,
        p4Factory: (GlslGenerator) -> P4,
        body: (p1: P1, p2: P2, p3: P3, p4: P4) -> RT
    ): FunctionDelegate4<RT, F, P1, P2, P3, P4> =
        FunctionDelegate4(funcFactory, p1Factory, p2Factory, p3Factory, p4Factory, body)


    fun Void(body: () -> Unit): FunctionVoidDelegate =
        FunctionVoidDelegate(FuncVoid(this), body)

    fun <P1 : Variable> Void(p1Factory: (GlslGenerator) -> P1, body: (p1: P1) -> Unit): FunctionVoidDelegate1<P1> =
        FunctionVoidDelegate1(FuncVoid(this), p1Factory, body)

    fun <P1 : Variable, P2 : Variable> Void(
        p1Factory: (GlslGenerator) -> P1,
        p2Factory: (GlslGenerator) -> P2,
        body: (p1: P1, p2: P2) -> Unit
    ): FunctionVoidDelegate2<P1, P2> = FunctionVoidDelegate2(FuncVoid(this), p1Factory, p2Factory, body)

    internal fun <T : Variable> createVariable(clazz: KClass<T>) = when (clazz) {
        GLFloat::class -> GLFloat(this)
        GLInt::class -> GLInt(this)
        Bool::class -> Bool(this)
        Vec2::class -> Vec2(this)
        Vec3::class -> Vec3(this)
        Vec4::class -> Vec4(this)
        Mat4::class -> Mat4(this)
        Sampler2D::class -> Sampler2D(this)
        else -> throw RuntimeException("${clazz.simpleName} is not supported!")
    }

    fun discard() = instructions.add(Instruction(DISCARD))

    inline fun If(condition: Bool, body: () -> Unit) {
        addInstruction(Instruction(IF, condition.value ?: "true"))
        body()
        addInstruction(Instruction(ENDIF))
    }

    inline fun ElseIf(condition: Bool, body: () -> Unit) {
        addInstruction(Instruction(ELSEIF, condition.value ?: "true"))
        body()
        addInstruction(Instruction(ENDIF))
    }

    inline fun Else(body: () -> Unit) {
        addInstruction(Instruction(ELSE))
        body()
        addInstruction(Instruction(ENDIF))
    }

    inline fun For(start: Int, until: Int, step: Int = 1, body: (GLInt) -> Unit) {
        if (step >= 1) {
            addInstruction(
                Instruction(
                    FOR,
                    "int loopIdx = $start; loopIdx < $until; ${if (step == 1) "loopIdx++" else "forIndex += step"}"
                )
            )
        } else {
            addInstruction(
                Instruction(
                    FOR,
                    "int loopIdx = $start; loopIdx > $until; ${if (step == -1) "loopIdx--" else "forIndex -= step"}"
                )
            )
        }
        body(GLInt(this))
        addInstruction(Instruction(END_FOR))
    }

    inline fun For(start: GLInt, until: GLInt, step: Int = 1, body: (GLInt) -> Unit) {
        if (step >= 1) {
            addInstruction(
                Instruction(
                    FOR,
                    "int loopIdx = $start; loopIdx < $until; ${if (step == 1) "loopIdx++" else "forIndex += step"}"
                )
            )
        } else {
            addInstruction(
                Instruction(
                    FOR,
                    "int loopIdx = $start; loopIdx > $until; ${if (step == -1) "loopIdx--" else "forIndex -= step"}"
                )
            )
        }
        body(GLInt(this))
        addInstruction(Instruction(END_FOR))
    }

    fun castMat3(m: Mat4) = Mat3(this, "mat3(${m.value})")
    fun int(v: GLFloat) = GLInt(this, "int(${v.value})")

    fun ternary(condition: Bool, left: GLInt, right: GLInt): GLInt =
        GLInt(this, "(${condition.value} ? ${left.value} : ${right.value})")

    fun ternary(condition: Bool, left: GLFloat, right: GLFloat): GLFloat =
        GLFloat(this, "(${condition.value} ? ${left.value} : ${right.value})")

    fun ternary(condition: Bool, left: Vec2, right: Vec2): Vec2 =
        Vec2(this, "(${condition.value} ? ${left.value} : ${right.value})")

    fun ternary(condition: Bool, left: Vec3, right: Vec3): Vec3 =
        Vec3(this, "(${condition.value} ? ${left.value} : ${right.value})")

    fun ternary(condition: Bool, left: Vec4, right: Vec4): Vec4 =
        Vec4(this, "(${condition.value} ? ${left.value} : ${right.value})")

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
    fun mod(v: Vec2, base: GLInt) = Vec2(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec3, base: GLInt) = Vec3(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec4, base: GLInt) = Vec4(this, "mod(${v.value}, ${base.value})")
    fun mod(v: Vec2, base: Float) = Vec2(this, "mod(${v.value}, ${base.str()})")
    fun mod(v: Vec3, base: Float) = Vec3(this, "mod(${v.value}, ${base.str()})")
    fun mod(v: Vec4, base: Float) = Vec4(this, "mod(${v.value}, ${base.str()})")
    fun mod(v: Vec2, base: Int) = Vec2(this, "mod(${v.value}, ${base})")
    fun mod(v: Vec3, base: Int) = Vec3(this, "mod(${v.value}, ${base})")
    fun mod(v: Vec4, base: Int) = Vec4(this, "mod(${v.value}, ${base})")

    fun min(v: GLFloat, base: GLFloat) = GLFloat(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: Vec2) = Vec2(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec3, base: Vec3) = Vec3(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec4, base: Vec4) = Vec4(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: GLFloat) = Vec2(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec3, base: GLFloat) = Vec3(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec4, base: GLFloat) = Vec4(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: GLInt) = Vec2(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec3, base: GLInt) = Vec3(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec4, base: GLInt) = Vec4(this, "min(${v.value}, ${base.value})")
    fun min(v: Vec2, base: Float) = Vec2(this, "min(${v.value}, ${base.str()})")
    fun min(v: Vec3, base: Float) = Vec3(this, "min(${v.value}, ${base.str()})")
    fun min(v: Vec4, base: Float) = Vec4(this, "min(${v.value}, ${base.str()})")
    fun min(v: Vec2, base: Int) = Vec2(this, "min(${v.value}, ${base})")
    fun min(v: Vec3, base: Int) = Vec3(this, "min(${v.value}, ${base})")
    fun min(v: Vec4, base: Int) = Vec4(this, "min(${v.value}, ${base})")

    fun max(v: GLFloat, v2: Float) = GLFloat(this, "max(${v.value}, ${v2.str()})")
    fun max(v: GLFloat, base: GLFloat) = GLFloat(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: Vec2) = Vec2(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec3, base: Vec3) = Vec3(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec4, base: Vec4) = Vec4(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: GLFloat) = Vec2(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec3, base: GLFloat) = Vec3(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec4, base: GLFloat) = Vec4(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: GLInt) = Vec2(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec3, base: GLInt) = Vec3(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec4, base: GLInt) = Vec4(this, "max(${v.value}, ${base.value})")
    fun max(v: Vec2, base: Float) = Vec2(this, "max(${v.value}, ${base.str()})")
    fun max(v: Vec3, base: Float) = Vec3(this, "max(${v.value}, ${base.str()})")
    fun max(v: Vec4, base: Float) = Vec4(this, "max(${v.value}, ${base.str()})")
    fun max(v: Vec2, base: Int) = Vec2(this, "max(${v.value}, ${base})")
    fun max(v: Vec3, base: Int) = Vec3(this, "max(${v.value}, ${base})")
    fun max(v: Vec4, base: Int) = Vec4(this, "max(${v.value}, ${base})")

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
    fun texture(sampler: Sampler2DArray, v: Vec3) = Vec4(this, "texture(${sampler.value}, ${v.value})")

    fun float() = ConstructorDelegate(GLFloat(this))
    fun float(x: Float) = ConstructorDelegate(GLFloat(this), x.str())
    fun float(x: GLFloat) = ConstructorDelegate(GLFloat(this), x.value)
    fun float(lit: String) = ConstructorDelegate(GLFloat(this), lit)

    fun intVal() = ConstructorDelegate(GLInt(this))
    fun intVal(x: GLInt) = ConstructorDelegate(GLInt(this), x.value)
    fun intVal(x: Int) = ConstructorDelegate(GLInt(this), "$x")

    fun bool() = ConstructorDelegate(Bool(this))
    fun bool(bool: Bool) = ConstructorDelegate(Bool(this), bool.value)
    fun bool(lit: String) = ConstructorDelegate(Bool(this), lit)

    fun vec2() = ConstructorDelegate(Vec2(this))
    fun vec2(x: Vec2) = ConstructorDelegate(Vec2(this), "${x.value}")
    fun vec2(x: Float, y: Float) = ConstructorDelegate(Vec2(this), "vec2(${x.str()}, ${y.str()})")
    fun vec2(x: GLFloat, y: Float) = ConstructorDelegate(Vec2(this), "vec2(${x.value}, ${y.str()})")
    fun vec2(x: Float, y: GLFloat) = ConstructorDelegate(Vec2(this), "vec2(${x.str()}, ${y.value})")
    fun vec2(x: GLFloat, y: GLFloat) = ConstructorDelegate(Vec2(this), "vec2(${x.value}, ${y.value})")

    fun vec2Lit() = Vec2(this)
    fun vec2Lit(x: Vec2) = Vec2(this, "${x.value}")
    fun vec2Lit(x: Float, y: Float) = Vec2(this, "vec2(${x.str()}, ${y.str()})")
    fun vec2Lit(x: GLFloat, y: Float) = Vec2(this, "vec2(${x.value}, ${y.str()})")
    fun vec2Lit(x: Float, y: GLFloat) = Vec2(this, "vec2(${x.str()}, ${y.value})")
    fun vec2Lit(x: GLFloat, y: GLFloat) = Vec2(this, "vec2(${x.value}, ${y.value})")

    fun vec3() = ConstructorDelegate(Vec3(this))
    fun vec3(v: Vec3) = ConstructorDelegate(Vec3(this), "${v.value}")
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

    fun vec3Lit() = Vec3(this)
    fun vec3Lit(v: Vec3) = Vec3(this, "${v.value}")
    fun vec3Lit(x: GLFloat, y: GLFloat, z: GLFloat) =
        Vec3(this, "vec3(${x.value}, ${y.value}, ${z.value})")

    fun vec3Lit(x: GLFloat, y: GLFloat, z: Float) =
        Vec3(this, "vec3(${x.value}, ${y.value}, ${z.str()})")

    fun vec3Lit(x: GLFloat, y: Float, z: GLFloat) =
        Vec3(this, "vec3(${x.value}, ${y.str()}, ${z.value})")

    fun vec3Lit(x: GLFloat, y: Float, z: Float) =
        Vec3(this, "vec3(${x.value}, ${y.str()}, ${z.str()})")

    fun vec3Lit(x: Float, y: GLFloat, z: GLFloat) =
        Vec3(this, "vec3(${x.str()}, ${y.value}, ${z.value})")

    fun vec3Lit(x: Float, y: GLFloat, z: Float) =
        Vec3(this, "vec3(${x.str()}, ${y.value}, ${z.str()})")

    fun vec3Lit(x: Float, y: Float, z: GLFloat) =
        Vec3(this, "vec3(${x.str()}, ${y.str()}, ${z.value})")

    fun vec3Lit(x: Float, y: Float, z: Float) =
        Vec3(this, "vec3(${x.str()}, ${y.str()}, ${z.str()})")

    fun vec3Lit(v2: Vec2, z: Float) = Vec3(this, "vec3(${v2.value}, ${z.str()})")
    fun vec3Lit(v2: Vec2, z: GLFloat) = Vec3(this, "vec3(${v2.value}, ${z.value})")
    fun vec3Lit(x: Float, v2: Vec2) = Vec3(this, "vec3(${x.str()}, ${v2.value})")
    fun vec3Lit(x: GLFloat, v2: Vec2) = Vec3(this, "vec3(${x.value}, ${v2.value})")

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

    fun vec4Lit() = Vec4(this)
    fun vec4Lit(vec3: Vec3, w: Float) = Vec4(this, "vec4(${vec3.value}, ${w.str()})")
    fun vec4Lit(vec3: Vec3, w: GLFloat) = Vec4(this, "vec4(${vec3.value}, ${w.value})")
    fun vec4Lit(vec2: Vec2, z: Float, w: Float) =
        Vec4(this, "vec4(${vec2.value}, ${z.str()}, ${w.str()})")

    fun vec4Lit(vec: Vec2, vec2: Vec2) =
        Vec4(this, "vec4(${vec.value}, ${vec2.value})")

    fun vec4Lit(x: GLFloat, y: GLFloat, zw: Vec2) =
        Vec4(this, "vec4(${x.value}, ${y.value}, ${zw.value})")

    fun vec4Lit(x: Float, y: Float, z: Float, w: Float) =
        Vec4(this, "vec4(${x.str()}, ${y.str()}, ${z.str()}, ${w.str()})")

    fun vec4Lit(x: Float, y: Float, z: Float, w: GLFloat) =
        Vec4(this, "vec4(${x.str()}, ${y.str()}, ${z.str()}, ${w.value})")

    fun vec4Lit(x: GLFloat, y: GLFloat, z: GLFloat, w: GLFloat) =
        Vec4(this, "vec4(${x.value}, ${y.value}, ${z.value}, ${w.value})")

    fun vec4Lit(x: GLFloat, y: GLFloat, z: GLFloat, w: Float) =
        Vec4(this, "vec4(${x.value}, ${y.value}, ${z.value}, ${w.str()})")

    fun vec4Lit(x: GLFloat, y: GLFloat, z: Float, w: Float) =
        Vec4(this, "vec4(${x.value}, ${y.value}, ${z.str()}, ${w.str()})")

    fun mat3() = ConstructorDelegate(Mat3(this))
    fun mat2() = ConstructorDelegate(Mat2(this))

    fun round(vec4: Vec4) = Vec4(this, "round(${vec4.value})")

    val String.float get() = GLFloat(this@GlslGenerator, this)
    val String.bool get() = Bool(this@GlslGenerator, this)
    val Float.lit get() = GLFloat(this@GlslGenerator, this.str())
    val Int.lit get() = GLInt(this@GlslGenerator, this.toString())

    operator fun Float.times(a: GLFloat) = GLFloat(a.builder, "(${this.str()} * ${a.value})")
    operator fun Float.times(a: GLInt) = GLFloat(a.builder, "(${this.str()} * ${a.value})")
    operator fun Float.times(a: Vec2) = Vec2(a.builder, "(${this.str()} * ${a.value})")
    operator fun Float.times(a: Vec3) = Vec3(a.builder, "(${this.str()} * ${a.value})")
    operator fun Float.times(a: Vec4) = Vec4(a.builder, "(${this.str()} * ${a.value})")
    operator fun Float.div(a: GLFloat) = GLFloat(a.builder, "(${this.str()} / ${a.value})")
    operator fun Float.div(a: GLInt) = GLFloat(a.builder, "(${this.str()} / ${a.value})")
    operator fun Float.div(a: Vec2) = Vec2(a.builder, "(${this.str()} / ${a.value})")
    operator fun Float.div(a: Vec3) = Vec3(a.builder, "(${this.str()} / ${a.value})")
    operator fun Float.div(a: Vec4) = Vec4(a.builder, "(${this.str()} / ${a.value})")
    operator fun Float.minus(a: GLFloat) = GLFloat(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.minus(a: GLInt) = GLFloat(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.minus(a: Vec2) = Vec2(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.minus(a: Vec3) = Vec3(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.minus(a: Vec4) = Vec4(a.builder, "(${this.str()} - ${a.value})")
    operator fun Float.plus(a: GLFloat) = GLFloat(a.builder, "(${this.str()} + ${a.value})")
    operator fun Float.plus(a: GLInt) = GLFloat(a.builder, "(${this.str()} + ${a.value})")
    operator fun Float.plus(a: Vec2) = Vec2(a.builder, "(${this.str()} + ${a.value})")
    operator fun Float.plus(a: Vec3) = Vec3(a.builder, "(${this.str()} + ${a.value})")
    operator fun Float.plus(a: Vec4) = Vec4(a.builder, "(${this.str()} + ${a.value})")

    operator fun Int.times(a: GLFloat) = GLFloat(a.builder, "(${this} * ${a.value})")
    operator fun Int.times(a: GLInt) = GLInt(a.builder, "(${this} * ${a.value})")
    operator fun Int.times(a: Vec2) = Vec2(a.builder, "(${this} * ${a.value})")
    operator fun Int.times(a: Vec3) = Vec3(a.builder, "(${this} * ${a.value})")
    operator fun Int.times(a: Vec4) = Vec4(a.builder, "(${this} * ${a.value})")
    operator fun Int.div(a: GLFloat) = GLFloat(a.builder, "(${this} / ${a.value})")
    operator fun Int.div(a: GLInt) = GLInt(a.builder, "(${this} / ${a.value})")
    operator fun Int.div(a: Vec2) = Vec2(a.builder, "(${this} / ${a.value})")
    operator fun Int.div(a: Vec3) = Vec3(a.builder, "(${this} / ${a.value})")
    operator fun Int.div(a: Vec4) = Vec4(a.builder, "(${this} / ${a.value})")
    operator fun Int.minus(a: GLFloat) = GLFloat(a.builder, "(${this} - ${a.value})")
    operator fun Int.minus(a: GLInt) = GLInt(a.builder, "(${this} - ${a.value})")
    operator fun Int.minus(a: Vec2) = Vec2(a.builder, "(${this} - ${a.value})")
    operator fun Int.minus(a: Vec3) = Vec3(a.builder, "(${this} - ${a.value})")
    operator fun Int.minus(a: Vec4) = Vec4(a.builder, "(${this} - ${a.value})")
    operator fun Int.plus(a: GLFloat) = GLFloat(a.builder, "(${this} + ${a.value})")
    operator fun Int.plus(a: GLInt) = GLInt(a.builder, "(${this} + ${a.value})")
    operator fun Int.plus(a: Vec2) = Vec2(a.builder, "(${this} + ${a.value})")
    operator fun Int.plus(a: Vec3) = Vec3(a.builder, "(${this} + ${a.value})")
    operator fun Int.plus(a: Vec4) = Vec4(a.builder, "(${this} + ${a.value})")
}

fun Float.str(): String {
    val r = "$this"
    return if (r.contains(".")) r else "$r.0"
}

abstract class GlslGeneratorComponent : GlslGenerator()