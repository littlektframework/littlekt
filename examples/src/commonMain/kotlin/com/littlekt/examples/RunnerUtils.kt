package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.examples.g2d.AnimationExample
import com.littlekt.examples.g2d.AssetProviderExample
import com.littlekt.examples.g2d.FontExample
import com.littlekt.examples.g2d.TtfFontExample
import com.littlekt.examples.g2d.ParticlesExample
import com.littlekt.examples.g2d.RenderTargetExample
import com.littlekt.examples.g2d.ShapesExample
import com.littlekt.examples.g2d.SpriteBatchExample
import com.littlekt.examples.g2d.SpriteCacheQuadsExample
import com.littlekt.examples.g2d.tilemap.LDtkTileMapExample
import com.littlekt.examples.g2d.tilemap.TiledTileMapExample
import com.littlekt.examples.g3d.BloomExample
import com.littlekt.examples.g3d.IBLExample
import com.littlekt.examples.g3d.ModelInstancingExample
import com.littlekt.examples.g3d.SkinnedInstancingExample
import com.littlekt.examples.g3d.MultiPassExample
import com.littlekt.examples.g3d.PBRExample
import com.littlekt.examples.g3d.ShadowsExample
import com.littlekt.examples.g3d.SimpleGltfExample
import com.littlekt.examples.g3d.TransparencyExample
import com.littlekt.examples.input.InputExample
import com.littlekt.examples.ui.DisplaySettingsExample
import com.littlekt.examples.ui.GameWorldAndUiViewportsExample
import com.littlekt.examples.ui.HelloSceneGraphExample
import com.littlekt.examples.ui.SceneGraphPlaygroundExample
import com.littlekt.examples.webgpu.ComputeBoidsExample
import com.littlekt.examples.webgpu.RotatingCubeExample
import com.littlekt.examples.webgpu.TextureExample
import com.littlekt.examples.webgpu.TriangleExample

/**
 * An example that can be ran by its [key], e.g. `-triangle`.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class Example(
    val key: String,
    val category: String,
    val title: String,
    val create: (Context) -> ContextListener,
)

private class ExamplesBuilder {
    val examples = mutableListOf<Example>()

    fun category(name: String, block: CategoryBuilder.() -> Unit) {
        CategoryBuilder(name).block()
    }

    inner class CategoryBuilder(private val category: String) {
        fun example(key: String, title: String, create: (Context) -> ContextListener) {
            examples += Example(key, category, title, create)
        }
    }
}

private fun examples(block: ExamplesBuilder.() -> Unit): Map<String, Example> =
    ExamplesBuilder().apply(block).examples.associateBy { it.key }

/** Every example by its key, in the order they are listed. */
val availableExamples: Map<String, Example> = examples {
    category("WebGPU") {
        example("-triangle", "Triangle", ::TriangleExample)
        example("-texture", "Texture", ::TextureExample)
        example("-rotatingCube", "Rotating Cube", ::RotatingCubeExample)
        example("-computeBoids", "Compute Boids", ::ComputeBoidsExample)
    }
    category("2D") {
        example("-spriteBatch", "SpriteBatch", ::SpriteBatchExample)
        example("-spriteCacheQuads", "Sprite Cache Quads", ::SpriteCacheQuadsExample)
        example("-animation", "Animation", ::AnimationExample)
        example("-particles", "Particles", ::ParticlesExample)
        example("-shapes", "Shapes", ::ShapesExample)
        example("-font", "Font", ::FontExample)
        example("-ttfFont", "TrueType Font", ::TtfFontExample)
        example("-renderTarget", "Render Target", ::RenderTargetExample)
        example("-assetProvider", "Asset Provider", ::AssetProviderExample)
    }
    category("Tilemaps") {
        example("-ldtk", "LDtk", ::LDtkTileMapExample)
        example("-tiled", "Tiled", ::TiledTileMapExample)
    }
    category("UI") {
        example("-helloSceneGraph", "Hello Scene Graph", ::HelloSceneGraphExample)
        example("-sceneGraphPlayground", "Scene Graph Playground", ::SceneGraphPlaygroundExample)
        example("-displaySettings", "Display Settings", ::DisplaySettingsExample)
        example(
            "-worldAndUiViewports",
            "Game World and UI Viewports",
            ::GameWorldAndUiViewportsExample,
        )
    }
    category("3D") {
        example("-simpleGltf", "Simple glTF", ::SimpleGltfExample)
        example("-modelInstancing", "Model Instancing", ::ModelInstancingExample)
        example("-skinnedInstancing", "Skinned Instancing", ::SkinnedInstancingExample)
        example("-pbr", "PBR", ::PBRExample)
        example("-shadows", "Shadows", ::ShadowsExample)
        example("-transparency", "Transparency", ::TransparencyExample)
        example("-ibl", "IBL", ::IBLExample)
        example("-bloom", "Bloom", ::BloomExample)
        example("-multiPass", "Multi Pass", ::MultiPassExample)
    }
    category("Input") {
        example("-input", "Input", ::InputExample)
    }
}
