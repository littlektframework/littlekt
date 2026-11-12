package com.littlekt.file.gltf

import com.littlekt.file.ByteBuffer
import com.littlekt.file.put
import com.littlekt.graphics.webgpu.AddressMode
import com.littlekt.graphics.webgpu.FilterMode
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.math.MutableVec3f
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class GltfJsonTests {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private val triangleJson =
        """
        {
          "asset": {"version": "2.0", "generator": "test"},
          "scene": 0,
          "scenes": [{"nodes": [0]}],
          "nodes": [
            {"name": "root", "children": [1], "translation": [1, 2, 3]},
            {"name": "tri", "mesh": 0}
          ],
          "meshes": [
            {"name": "triangle", "primitives": [{"attributes": {"POSITION": 1, "TEXCOORD_0": 2}, "indices": 0, "material": 0}]}
          ],
          "materials": [{"name": "mat"}],
          "buffers": [{"byteLength": 44}],
          "bufferViews": [
            {"buffer": 0, "byteOffset": 0, "byteLength": 6, "target": 34963},
            {"buffer": 0, "byteOffset": 8, "byteLength": 36, "target": 34962}
          ],
          "accessors": [
            {"bufferView": 0, "componentType": 5123, "count": 3, "type": "SCALAR"},
            {"bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC3", "min": [0, 0, 0], "max": [1, 1, 0]},
            {"componentType": 5126, "count": 3, "type": "VEC2"}
          ]
        }
        """
            .trimIndent()

    private fun triangle(): GltfData {
        val gltf = json.decodeFromString<GltfData>(triangleJson)
        val bytes = ByteArray(44)
        bytes.put(0.toShort(), 0)
        bytes.put(1.toShort(), 2)
        bytes.put(2.toShort(), 4)
        floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f).forEachIndexed { i, f -> bytes.put(f, 8 + i * 4) }
        gltf.buffers[0].data = ByteBuffer(bytes)
        return gltf
    }

    private fun accessor(componentType: Int, type: String, count: Int, bytes: ByteArray, byteStride: Int = 0, byteOffset: Int = 0): GltfAccessor {
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "buffers": [{"byteLength": ${bytes.size}}],
                  "bufferViews": [{"buffer": 0, "byteLength": ${bytes.size}, "byteStride": $byteStride}],
                  "accessors": [{"bufferView": 0, "byteOffset": $byteOffset, "componentType": $componentType, "count": $count, "type": "$type"}]
                }
                """
                    .trimIndent()
            )
        gltf.buffers[0].data = ByteBuffer(bytes)
        return gltf.accessors[0]
    }

    private fun floats(vararg values: Float): ByteArray {
        val bytes = ByteArray(values.size * 4)
        values.forEachIndexed { i, f -> bytes.put(f, i * 4) }
        return bytes
    }

    @Test
    fun test_decode_asset() {
        val gltf = triangle()
        assertEquals("2.0", gltf.asset.version)
        assertEquals("test", gltf.asset.generator)
        assertNull(gltf.asset.copyright)
    }

    @Test
    fun test_decode_accessors() {
        val accessors = triangle().accessors
        assertEquals(GltfComponentType.UnsignedShort, accessors[0].componentType)
        assertEquals(GltfAccessorType.Scalar, accessors[0].type)
        assertEquals(GltfComponentType.Float, accessors[1].componentType)
        assertEquals(GltfAccessorType.Vec3, accessors[1].type)
        assertEquals(listOf(1f, 1f, 0f), accessors[1].max)
        assertEquals(-1, accessors[2].bufferView)
        assertFalse(accessors[1].normalized)
    }

    @Test
    fun test_accessor_buffer_view_refs() {
        val gltf = triangle()
        assertSame(gltf.bufferViews[1], gltf.accessors[1].bufferViewRef)
        assertNull(gltf.accessors[2].bufferViewRef)
        assertSame(gltf.buffers[0], gltf.bufferViews[0].bufferRef)
    }

    @Test
    fun test_decode_buffer_views() {
        val view = triangle().bufferViews[1]
        assertEquals(8, view.byteOffset)
        assertEquals(36, view.byteLength)
        assertEquals(0, view.byteStride)
        assertEquals(34962, view.target)
    }

    @Test
    fun test_buffer_view_get_data_copies_range() {
        val data = triangle().bufferViews[0].getData()
        assertEquals(6, data.capacity)
        assertContentEquals(byteArrayOf(0, 0, 1, 0, 2, 0), data.toArray())
    }

    @Test
    fun test_decode_mesh_primitive() {
        val gltf = triangle()
        val primitive = gltf.meshes[0].primitives[0]
        assertEquals(mapOf(GltfAttribute.Position to 1, GltfAttribute.TexCoord0 to 2), primitive.attributes)
        assertEquals(0, primitive.indices)
        assertEquals(GltfRenderMode.Triangles, primitive.mode)
        assertSame(gltf.materials[0], primitive.materialRef)
    }

    @Test
    fun test_node_refs() {
        val gltf = triangle()
        val root = gltf.nodes[0]
        assertEquals(listOf(1f, 2f, 3f), root.translation)
        assertSame(gltf.nodes[1], root.childRefs.single())
        assertNull(root.meshRef)
        assertSame(gltf.meshes[0], gltf.nodes[1].meshRef)
        assertSame(root, gltf.scenes[0].nodeRefs.single())
    }

    @Test
    fun test_node_defaults() {
        val node = triangle().nodes[1]
        assertTrue(node.children.isEmpty())
        assertTrue(node.rotation.isEmpty())
        assertTrue(node.matrix.isEmpty())
        assertEquals(-1, node.skin)
        assertEquals(-1, node.camera)
    }

    @Test
    fun test_material_defaults() {
        val material = triangle().materials[0]
        assertEquals(0.5f, material.alphaCutoff)
        assertEquals(GltfAlphaMode.Opaque, material.alphaMode)
        assertFalse(material.doubleSided)
        assertNull(material.normalTexture)
        assertEquals(1f, material.pbrMetallicRoughness.metallicFactor)
        assertEquals(1f, material.pbrMetallicRoughness.roughnessFactor)
    }

    @Test
    fun test_pbr_defaults_when_present() {
        val pbr = json.decodeFromString<GltfMaterial>("""{"pbrMetallicRoughness": {}}""").pbrMetallicRoughness
        assertEquals(listOf(1f, 1f, 1f, 1f), pbr.baseColorFactor)
        assertNull(pbr.baseColorTexture)
    }

    @Test
    fun test_decode_material_textures() {
        val material =
            json.decodeFromString<GltfMaterial>(
                """
                {
                  "alphaMode": "MASK", "alphaCutoff": 0.25, "doubleSided": true,
                  "pbrMetallicRoughness": {"baseColorFactor": [1, 0, 0, 1], "baseColorTexture": {"index": 2, "texCoord": 1}, "metallicFactor": 0},
                  "normalTexture": {"index": 3, "scale": 0.5},
                  "occlusionTexture": {"index": 4, "strength": 0.75},
                  "emissiveFactor": [1, 1, 0]
                }
                """
                    .trimIndent()
            )
        assertEquals(GltfAlphaMode.Mask, material.alphaMode)
        assertEquals(0.25f, material.alphaCutoff)
        assertTrue(material.doubleSided)
        assertEquals(GltfTextureInfo(index = 2, texCoord = 1), material.pbrMetallicRoughness.baseColorTexture)
        assertEquals(0f, material.pbrMetallicRoughness.metallicFactor)
        assertEquals(0.5f, material.normalTexture?.scale)
        assertEquals(0.75f, material.occlusionTexture?.strength)
        assertEquals(listOf(1f, 1f, 0f), material.emissiveFactor)
    }

    @Test
    fun test_decode_material_extensions_object() {
        val material =
            json.decodeFromString<GltfMaterial>(
                """{"extensions": {"KHR_materials_ior": {"ior": 1.5}, "KHR_materials_emissive_strength": {"emissiveStrength": 4}}}"""
            )
        val extensions = assertNotNull(material.extensions).single()
        assertEquals(1.5f, extensions.khrMaterialsIor?.ior)
        assertEquals(4, extensions.khrMaterialsEmissiveStrength?.emissiveStrength)
    }

    @Test
    fun test_decode_texture_info_extensions_object() {
        val info =
            json.decodeFromString<GltfTextureInfo>(
                """{"index": 0, "extensions": {"KHR_texture_transform": {"offset": [0.5, 0], "scale": [2, 2]}}}"""
            )
        val transform = assertNotNull(info.extensions).single().khrTextureTransform
        assertEquals(listOf(0.5f, 0f), transform.offset)
        assertEquals(listOf(2f, 2f), transform.scale)
    }

    @Test
    fun test_decode_extensions_used_and_required() {
        val gltf =
            json.decodeFromString<GltfData>(
                """{"asset": {"version": "2.0"}, "extensionsUsed": ["KHR_materials_ior"], "extensionsRequired": ["KHR_texture_transform"]}"""
            )
        assertEquals(listOf("KHR_materials_ior"), gltf.extensionsUsed)
        assertEquals(listOf("KHR_texture_transform"), gltf.extensionRequired)
    }

    @Test
    fun test_texture_refs() {
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "images": [{"uri": "a.png"}, {"uri": "b.png", "mimeType": "image/png"}],
                  "samplers": [{"magFilter": 9728, "minFilter": 9987, "wrapS": 33071, "wrapT": 33648}],
                  "textures": [{"sampler": 0, "source": 1}]
                }
                """
                    .trimIndent()
            )
        val texture = gltf.textures.single()
        assertSame(gltf.images[1], texture.imageRef)
        assertEquals(GltfMIMEType.ImagePNG, texture.imageRef.mimeType)
        assertSame(gltf.samplers[0], texture.samplerRef)
        assertEquals(GltfTextureFilter.Nearest, texture.samplerRef.magFilter)
        assertEquals(GltfTextureFilter.LinearMipMapLinear, texture.samplerRef.minFilter)
        assertEquals(GltfTextureWrap.ClampToEdge, texture.samplerRef.wrapS)
        assertEquals(GltfTextureWrap.MirroredRepeat, texture.samplerRef.wrapT)
    }

    @Test
    fun test_texture_without_sampler_uses_default_sampler() {
        val gltf =
            json.decodeFromString<GltfData>(
                """{"asset": {"version": "2.0"}, "images": [{"uri": "a.png"}], "textures": [{"source": 0}]}"""
            )
        val sampler = gltf.textures.single().samplerRef
        assertNull(sampler.magFilter)
        assertNull(sampler.wrapS)
    }

    @Test
    fun test_image_buffer_view_ref() {
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "buffers": [{"byteLength": 4}],
                  "bufferViews": [{"buffer": 0, "byteLength": 4}],
                  "images": [{"bufferView": 0, "mimeType": "image/jpeg"}]
                }
                """
                    .trimIndent()
            )
        assertSame(gltf.bufferViews[0], gltf.images[0].bufferViewRef)
        assertEquals(GltfMIMEType.ImageJPEG, gltf.images[0].mimeType)
    }

    @Test
    fun test_skin_and_animation_refs() {
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "nodes": [{"skin": 0, "children": [1]}, {"name": "joint"}],
                  "skins": [{"joints": [1], "inverseBindMatrices": 0}],
                  "accessors": [
                    {"componentType": 5126, "count": 1, "type": "MAT4"},
                    {"componentType": 5126, "count": 2, "type": "SCALAR"},
                    {"componentType": 5126, "count": 2, "type": "VEC4"}
                  ],
                  "animations": [{
                    "channels": [{"sampler": 0, "target": {"node": 1, "path": "rotation"}}],
                    "samplers": [{"input": 1, "output": 2, "interpolation": "STEP"}]
                  }]
                }
                """
                    .trimIndent()
            )
        val skin = gltf.skins.single()
        assertSame(skin, gltf.nodes[0].skinRef)
        assertSame(gltf.nodes[1], skin.jointRefs.single())
        assertSame(gltf.accessors[0], skin.inverseBindMatrixAccessorRef)
        val channel = gltf.animations.single().channels.single()
        assertEquals(GltfAnimationPath.Rotation, channel.target.path)
        assertSame(gltf.nodes[1], channel.target.nodeRef)
        assertEquals(GltfInterpolation.Step, channel.samplerRef.interpolation)
        assertSame(gltf.accessors[1], channel.samplerRef.inputAccessorRef)
        assertSame(gltf.accessors[2], channel.samplerRef.outputAccessorRef)
    }

    @Test
    fun test_animation_sampler_default_interpolation() {
        val sampler = json.decodeFromString<GltfAnimationSampler>("""{"input": 0, "output": 1}""")
        assertEquals(GltfInterpolation.Linear, sampler.interpolation)
    }

    @Test
    fun test_decode_camera() {
        val camera =
            json.decodeFromString<GltfCamera>(
                """{"type": "perspective", "perspective": {"yfov": 0.8, "znear": 0.1, "zfar": 100}}"""
            )
        assertEquals(GltfCameraType.Perspective, camera.type)
        assertEquals(0.8f, camera.perspective.yfov)
        assertNull(camera.perspective.aspectRatio)
    }

    @Test
    fun test_component_type_byte_sizes() {
        assertEquals(1, GltfComponentType.Byte.byteSize)
        assertEquals(1, GltfComponentType.UnsignedByte.byteSize)
        assertEquals(2, GltfComponentType.Short.byteSize)
        assertEquals(2, GltfComponentType.UnsignedShort.byteSize)
        assertEquals(4, GltfComponentType.UnsignedInt.byteSize)
        assertEquals(4, GltfComponentType.Float.byteSize)
        assertFalse(GltfComponentType.Float in GltfComponentType.IntTypes)
    }

    @Test
    fun test_accessor_type_num_components() {
        assertEquals(1, GltfAccessorType.Scalar.numComponents)
        assertEquals(3, GltfAccessorType.Vec3.numComponents)
        assertEquals(4, GltfAccessorType.Mat2.numComponents)
        assertEquals(9, GltfAccessorType.Mat3.numComponents)
        assertEquals(16, GltfAccessorType.Mat4.numComponents)
    }

    @Test
    fun test_attribute_values_match_serial_names() {
        assertEquals("COLOR_1", GltfAttribute.Color1.value)
        assertEquals("COLOR_2", GltfAttribute.Color2.value)
        assertEquals("COLOR_3", GltfAttribute.Color3.value)
        assertEquals("COLOR_4", GltfAttribute.Color4.value)
        GltfAttribute.entries.forEach {
            assertEquals(it, json.decodeFromString<GltfAttribute>("\"${it.value}\""))
        }
    }

    @Test
    fun test_to_index_format() {
        assertEquals(IndexFormat.UINT16, GltfComponentType.UnsignedShort.toIndexFormat())
        assertEquals(IndexFormat.UINT32, GltfComponentType.UnsignedInt.toIndexFormat())
        assertNull(GltfComponentType.UnsignedByte.toIndexFormat())
        assertNull(GltfComponentType.Float.toIndexFormat())
    }

    @Test
    fun test_render_mode_to_topology() {
        assertEquals(PrimitiveTopology.POINT_LIST, GltfRenderMode.Points.toTopology())
        assertEquals(PrimitiveTopology.LINE_LIST, GltfRenderMode.Lines.toTopology())
        assertEquals(PrimitiveTopology.LINE_STRIP, GltfRenderMode.LineLoop.toTopology())
        assertEquals(PrimitiveTopology.LINE_STRIP, GltfRenderMode.LineStrip.toTopology())
        assertEquals(PrimitiveTopology.TRIANGLE_LIST, GltfRenderMode.Triangles.toTopology())
        assertEquals(PrimitiveTopology.TRIANGLE_STRIP, GltfRenderMode.TriangleStrip.toTopology())
    }

    @Test
    fun test_unsupported_render_mode_throws() {
        assertFailsWith<IllegalStateException> { GltfRenderMode.TriangleFan.toTopology() }
    }

    @Test
    fun test_wrap_to_address_mode() {
        assertEquals(AddressMode.REPEAT, GltfTextureWrap.Repeat.toAddressMode())
        assertEquals(AddressMode.CLAMP_TO_EDGE, GltfTextureWrap.ClampToEdge.toAddressMode())
        assertEquals(AddressMode.MIRROR_REPEAT, GltfTextureWrap.MirroredRepeat.toAddressMode())
        assertEquals(AddressMode.REPEAT, (null as GltfTextureWrap?).toAddressMode())
    }

    @Test
    fun test_filter_to_filter_mode() {
        assertEquals(FilterMode.NEAREST to FilterMode.NEAREST, GltfTextureFilter.Nearest.toFilterMode())
        assertEquals(FilterMode.LINEAR to FilterMode.LINEAR, GltfTextureFilter.LinearMipMapLinear.toFilterMode())
        assertEquals(FilterMode.LINEAR to FilterMode.NEAREST, GltfTextureFilter.LinearMipMapNearest.toFilterMode())
        assertEquals(FilterMode.NEAREST to FilterMode.LINEAR, GltfTextureFilter.NearestMipMapLinear.toFilterMode())
        assertEquals(FilterMode.LINEAR to FilterMode.LINEAR, (null as GltfTextureFilter?).toFilterMode())
    }

    @Test
    fun test_int_accessor_reads_indices() {
        val accessor = GltfIntAccessor(triangle().accessors[0])
        assertEquals(listOf(0, 1, 2), List(3) { accessor.next() })
    }

    @Test
    fun test_int_accessor_overflow_throws() {
        val accessor = GltfIntAccessor(triangle().accessors[0])
        repeat(3) { accessor.next() }
        assertFailsWith<IndexOutOfBoundsException> { accessor.next() }
    }

    @Test
    fun test_vec3_accessor_reads_positions() {
        val accessor = GltfVec3fAccessor(triangle().accessors[1])
        val result = MutableVec3f()
        assertEquals(MutableVec3f(0f, 0f, 0f), accessor.next(result))
        assertEquals(MutableVec3f(1f, 0f, 0f), accessor.next(result))
        assertEquals(MutableVec3f(0f, 1f, 0f), accessor.next(result))
    }

    @Test
    fun test_accessor_without_buffer_view_reads_zeros() {
        val accessor = GltfVec2fAccessor(triangle().accessors[2])
        val v = accessor.next()
        assertEquals(0f, v.x)
        assertEquals(0f, v.y)
    }

    @Test
    fun test_accessor_type_mismatch_throws() {
        assertFailsWith<IllegalArgumentException> { GltfVec3fAccessor(triangle().accessors[0]) }
        assertFailsWith<IllegalArgumentException> { GltfIntAccessor(triangle().accessors[1]) }
    }

    @Test
    fun test_float_accessor_respects_byte_stride() {
        val accessor = GltfFloatAccessor(accessor(5126, "SCALAR", 3, floats(1f, 9f, 2f, 9f, 3f, 9f), byteStride = 8))
        assertEquals(listOf(1f, 2f, 3f), List(3) { accessor.next() })
    }

    @Test
    fun test_float_accessor_respects_accessor_byte_offset() {
        val accessor = GltfFloatAccessor(accessor(5126, "SCALAR", 2, floats(9f, 4f, 5f), byteOffset = 4))
        assertEquals(listOf(4f, 5f), List(2) { accessor.next() })
    }

    @Test
    fun test_sparse_accessor_uses_count_and_byte_offsets() {
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "buffers": [{"byteLength": 32}],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 16},
                    {"buffer": 0, "byteOffset": 16, "byteLength": 4},
                    {"buffer": 0, "byteOffset": 20, "byteLength": 12}
                  ],
                  "accessors": [{
                    "bufferView": 0, "componentType": 5126, "count": 4, "type": "SCALAR",
                    "sparse": {
                      "count": 1,
                      "indices": {"bufferView": 1, "byteOffset": 1, "componentType": 5121},
                      "values": {"bufferView": 2, "byteOffset": 4}
                    }
                  }]
                }
                """
                    .trimIndent()
            )
        val bytes = ByteArray(32)
        floats(1f, 2f, 3f, 4f).copyInto(bytes)
        bytes[17] = 1
        bytes[18] = 3
        floats(99f, 10f, 20f).copyInto(bytes, 20)
        gltf.buffers[0].data = ByteBuffer(bytes)
        val accessor = GltfFloatAccessor(gltf.accessors[0])
        assertEquals(listOf(1f, 10f, 3f, 4f), List(4) { accessor.next() })
    }

    @Test
    fun test_float_accessor_normalizes_unsigned_byte() {
        val accessor = GltfFloatAccessor(accessor(5121, "SCALAR", 2, byteArrayOf(0xFF.toByte(), 0)))
        assertEquals(1f, accessor.next())
        assertEquals(0f, accessor.next())
    }

    @Test
    fun test_float_accessor_normalizes_unsigned_short() {
        val bytes = ByteArray(2).put(65535.toShort(), 0)
        assertEquals(1f, GltfFloatAccessor(accessor(5123, "SCALAR", 1, bytes)).next())
    }

    @Test
    fun test_vec4i_accessor_reads_unsigned_bytes() {
        val accessor = GltfVec4iAccessor(accessor(5121, "VEC4", 1, byteArrayOf(1, 2, 3, 0xFF.toByte())))
        val v = accessor.next()
        assertEquals(1, v.x)
        assertEquals(2, v.y)
        assertEquals(3, v.z)
        assertEquals(255, v.w)
    }

    @Test
    fun test_int_accessor_reads_signed_short() {
        val bytes = ByteArray(2).put((-3).toShort(), 0)
        assertEquals(-3, GltfIntAccessor(accessor(5122, "SCALAR", 1, bytes)).next())
    }

    @Test
    fun test_mat4_accessor_reads_column_major_data() {
        val values = FloatArray(16) { it.toFloat() }
        val mat = GltfMat4Accessor(accessor(5126, "MAT4", 1, floats(*values))).next()
        assertContentEquals(values, mat.data)
    }

    @Test
    fun test_sparse_accessor_overrides_values() {
        val bytes = ByteArray(12 + 2 + 2 + 8)
        floatArrayOf(1f, 2f, 3f).forEachIndexed { i, f -> bytes.put(f, i * 4) }
        bytes.put(2.toShort(), 12)
        bytes.put(8f, 16)
        val gltf =
            json.decodeFromString<GltfData>(
                """
                {
                  "asset": {"version": "2.0"},
                  "buffers": [{"byteLength": ${bytes.size}}],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 12},
                    {"buffer": 0, "byteOffset": 12, "byteLength": 2},
                    {"buffer": 0, "byteOffset": 16, "byteLength": 4}
                  ],
                  "accessors": [{
                    "bufferView": 0, "componentType": 5126, "count": 3, "type": "SCALAR",
                    "sparse": {"count": 1, "indices": {"bufferView": 1, "componentType": 5123}, "values": {"bufferView": 2}}
                  }]
                }
                """
                    .trimIndent()
            )
        gltf.buffers[0].data = ByteBuffer(bytes)
        val accessor = GltfFloatAccessor(gltf.accessors[0])
        assertEquals(listOf(1f, 2f, 8f), List(3) { accessor.next() })
    }
}
