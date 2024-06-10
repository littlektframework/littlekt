package com.littlekt.examples

val availableExamples =
    mapOf(
        "-triangle" to Pair("Triangle", ::TriangleExample),
        "-texture" to Pair("Texture", ::TextureExample),
        "-textureMesh" to Pair("Texture Mesh", ::TextureMeshExample),
        "-textureMeshAndCamera" to Pair("Texture Mesh And Camera", ::TextureMeshAndCameraExample),
        "-textureViaCommand" to Pair("Texture Via Command", ::TextureViaCommandEncoderExample),
        "-tiledMesh" to Pair("Tiled Mesh", ::TiledMeshExample),
        "-spriteCacheQuads" to Pair("Sprite Cache Quads", ::SpriteCacheQuadsExample),
        "-rotatingCube" to Pair("Rotating Cube", ::RotatingCubeExample),
        "-multipleTextures" to Pair("Multiple Textures", ::MultipleTexturesExample),
        "-textureBlends" to Pair("Texture Blends", ::TextureBlendsExample),
        "-spriteBatchMultiShader" to
            Pair("SpriteBatch Multi Shader", ::RenderSpriteBatchAndMultipleShadersExample),
        "-simpleCamera" to Pair("Simple Camera", ::SimpleCameraExample),
        "-ldtk" to Pair("LDtk", ::LDtkTileMapExample),
        "-ldtkCache" to Pair("LDtk Cache", ::LDtkTileMapCacheExample),
        "-tiled" to Pair("Tiled", ::TiledTileMapExample),
        "-tiledCache" to Pair("Tiled Cache", ::TiledTileMapCacheExample),
        "-font" to Pair("Font", ::FontExample),
        "-helloSceneGraph" to Pair("Hello Scene Graph", ::HelloSceneGraphExample),
        "-renderTarget" to Pair("Render Target", ::RenderTargetExample),
        "-computeBoids" to Pair("Compute Boids", ::ComputeBoidsExample)
    )
