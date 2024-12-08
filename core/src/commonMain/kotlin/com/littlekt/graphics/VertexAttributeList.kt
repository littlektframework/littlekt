package com.littlekt.graphics

/**
 * A [List] of [VertexAttribute]. Useful for needing to cache by using the lists [key] property.
 *
 * @param attributes vertex attribute list to be used to calculate [key]
 * @author Colton Daily
 * @date 12/8/2024
 */
class VertexAttributeList(val attributes: List<VertexAttribute>) :
    List<VertexAttribute> by attributes {
    /**
     * A key that is calculated by each of the [attributes] in the lists [VertexAttribute.usage]
     * value.
     */
    val key = attributes.map { it.usage.usage }.reduce { acc, usage -> acc or usage }
}
