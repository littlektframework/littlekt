package com.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderBlockBuilder(base: ShaderBlock? = null) {
    val includes = mutableListOf<ShaderStruct>().apply { base?.includes?.let { addAll(it) } }
    val markers = mutableMapOf<String, String>().apply { base?.markers?.let { putAll(it) } }
    var body: String = base?.body ?: ""

    fun include(struct: ShaderStruct) {
        includes.add(struct)
    }

    fun marker(name: String): String {
        val markerPlaceholder = "%$name%"
        markers[name] = markerPlaceholder
        return markerPlaceholder
    }

    protected fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    fun build(): ShaderBlock {
        return ShaderBlock(includes, markers, body)
    }
}
