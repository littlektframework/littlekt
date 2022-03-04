package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.tilemap.TileMap
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledMap(
    val backgroundColor: Color?,
    val orientation: Orientation,
    val renderOrder: RenderOrder,
    val staggerAxis: StaggerAxis?,
    val staggerIndex: StaggerIndex?,
    val layers: List<TiledLayer>,
    val width: Int,
    val height: Int,
    val properties: Map<String, Property>,
    val tileWidth: Int,
    val tileHeight: Int,
    val tileSets: List<TiledTileset>,
    private val textures: MutableList<Texture>
) : TileMap(), Disposable {
    val layersByName by lazy { layers.associateBy { it.name } }
    val layersById by lazy { layers.associateBy { it.id } }

    override fun render(batch: Batch, camera: Camera, x: Float, y: Float) = render(batch, camera, x, y, false)

    fun render(batch: Batch, camera: Camera, x: Float = 0f, y: Float = 0f, displayObjects: Boolean = false) {
        layers.forEach {
            it.render(batch, camera, x, y, displayObjects)
        }
    }

    fun layer(name: String): TiledLayer =
        layersByName[name] ?: error("Layer: '$name' does not exist in this map!")

    operator fun get(layer: String) = layer(layer)

    override fun dispose() {
        textures.forEach { it.dispose() }
        textures.clear()
    }

    enum class Orientation(val value: String) {
        ORTHOGONAL("orthogonal"),
        ISOMETRIC("isometric"),
        STAGGERED("staggered"),
        HEXAGONAL("hexagonal")
    }

    enum class RenderOrder(val value: String) {
        RIGHT_DOWN("right-down"),
        RIGHT_UP("right-up"),
        LEFT_DOWN("left-down"),
        LEFT_UP("left-up")
    }

    enum class StaggerAxis(val value: String) {
        X("x"), Y("y")
    }

    enum class StaggerIndex(val value: String) {
        EVEN("even"), ODD("odd")
    }

    enum class ObjectAlignment(val value: String) {
        UNSPECIFIED("unspecified"),
        TOP_LEFT("topleft"),
        TOP("top"),
        TOP_RIGHT("topright"),
        LEFT("left"),
        CENTER("center"),
        RIGHT("right"),
        BOTTOM_LEFT("bottomleft"),
        BOTTOM("bottom"),
        BOTTOM_RIGHT("bottomright")
    }

    data class Grid(
        val cellWidth: Int,
        val cellHeight: Int,
        val orientation: Orientation = Orientation.ORTHOGONAL
    )

    sealed class Property {
        abstract val string: String
        val int: Int get() = number.toInt()
        val float: Float get() = number.toFloat()
        val bool: Boolean get() = float != 0f
        open val number: Number get() = string.toFloatOrNull() ?: 0f
        override fun toString(): String = string

        class StringProp(val value: String) : Property() {
            override val string: String get() = value
        }

        class IntProp(val value: Int) : Property() {
            override val number: Number get() = value
            override val string: String get() = "$value"
        }

        class FloatProp(val value: Float) : Property() {
            override val number: Number get() = value
            override val string: String get() = "$value"
        }

        class BoolProp(val value: Boolean) : Property() {
            override val number: Number get() = if (value) 1 else 0
            override val string: String get() = "$value"
        }

        class ColorProp(val value: Color) : Property() {
            override val string: String get() = "$value"
        }

        class FileProp(var path: String) : Property() {
            override val string: String get() = path
        }

        class ObjectProp(var id: Int) : Property() {
            override val string: String get() = "$id"
        }
    }

    data class Object(
        val id: Int,
        val gid: Long?,
        val name: String,
        val type: String,
        val bounds: Rect,
        val rotation: Angle,
        val visible: Boolean,
        val shape: Shape,
        val properties: Map<String, Property>
    ) {
        val x: Float get() = bounds.x
        val y: Float get() = bounds.y

        fun str(propName: String, default: String = "") = properties[propName]?.string ?: default
        fun int(propName: String, default: Int = 0) = properties[propName]?.int ?: default
        fun float(propName: String, default: Float = 0f) = properties[propName]?.float ?: default
        fun bool(propName: String, default: Boolean = false) = properties[propName]?.bool ?: default

        enum class DrawOrder(val value: String) {
            INDEX("index"), TOP_DOWN("topdown")
        }

        sealed class Shape {
            data class Rectangle(val width: Float, val height: Float) : Shape()
            data class Ellipse(val width: Float, val height: Float) : Shape()
            object Point : Shape()
            data class Polygon(val points: List<com.lehaine.littlekt.math.geom.Point>) : Shape()
            data class Polyline(val points: List<com.lehaine.littlekt.math.geom.Point>) : Shape()
            data class Text(
                val fontFamily: String,
                val pixelSize: Int,
                val wordWrap: Boolean,
                val color: Color,
                val bold: Boolean,
                val italic: Boolean,
                val underline: Boolean,
                val strikeout: Boolean,
                val kerning: Boolean,
                val hAlign: HAlign,
                val vAlign: VAlign
            ) : Shape()
        }
    }

    companion object {
        internal const val FLAG_FLIP_HORIZONTALLY = 0x80000000.toInt()
        internal const val FLAG_FLIP_VERTICALLY = 0x40000000
        internal const val FLAG_FLIP_DIAGONALLY = 0x20000000
        internal const val MASK_CLEAR = 0xE0000000.toInt()
    }
}