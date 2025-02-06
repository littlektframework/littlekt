package com.littlekt.graphics.shader.builder

/**
 * Represents a structure within a shader, encapsulating the name, variable, and body of the
 * structure.
 *
 * @property name The name of the shader struct. This must match the way it is defined in the
 *   [body].
 * @property body The body or definition of the shader structure. This includes defining the
 *   structure and possible any buffers.
 * @property src A reference to the body of the shader structure.
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderStruct(val name: String, body: String) : ShaderBlock(emptyList(), emptyList(), body) {}
