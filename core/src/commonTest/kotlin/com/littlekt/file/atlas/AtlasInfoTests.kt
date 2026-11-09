package com.littlekt.file.atlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class AtlasInfoTests {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private val pageJson =
        """
        {
          "frames": [
            {
              "filename": "hero_idle0",
              "frame": {"x": 1, "y": 2, "w": 16, "h": 24},
              "rotated": false,
              "trimmed": true,
              "spriteSourceSize": {"x": 3, "y": 4, "w": 16, "h": 24},
              "sourceSize": {"w": 32, "h": 32},
              "pivot": {"x": 0.5, "y": 0.5}
            },
            {
              "filename": "hero_idle1",
              "frame": {"x": 20, "y": 0, "w": 24, "h": 16},
              "rotated": true,
              "trimmed": false,
              "spriteSourceSize": {"x": 0, "y": 0, "w": 16, "h": 24},
              "sourceSize": {"w": 16, "h": 24}
            }
          ],
          "meta": {
            "app": "littlekt",
            "image": "tiles.png",
            "format": "RGBA8888",
            "scale": "2",
            "size": {"w": 64, "h": 32},
            "frameTags": [{"name": "idle", "from": 0, "to": 1, "direction": "forward"}],
            "related_multi_packs": ["tiles1.json"]
          }
        }
        """
            .trimIndent()

    private fun page(image: String, vararg names: String): AtlasPage {
        val frames =
            names.joinToString(",") {
                """{"filename":"$it","frame":{"x":0,"y":0,"w":1,"h":1},"rotated":false,"trimmed":false,"spriteSourceSize":{"x":0,"y":0,"w":1,"h":1},"sourceSize":{"w":1,"h":1}}"""
            }
        return json.decodeFromString("""{"frames":[$frames],"meta":{"image":"$image"}}""")
    }

    @Test
    fun test_decode_frame_rects() {
        val frame = json.decodeFromString<AtlasPage>(pageJson).frames[0]
        assertEquals("hero_idle0", frame.name)
        assertEquals(AtlasPage.Rect(1, 2, 16, 24), frame.frame)
        assertEquals(AtlasPage.Rect(3, 4, 16, 24), frame.spriteSourceSize)
        assertEquals(AtlasPage.Size(32, 32), frame.sourceSize)
    }

    @Test
    fun test_decode_trimmed_and_rotated_flags() {
        val frames = json.decodeFromString<AtlasPage>(pageJson).frames
        assertTrue(frames[0].trimmed)
        assertFalse(frames[0].rotated)
        assertTrue(frames[1].rotated)
        assertFalse(frames[1].trimmed)
    }

    @Test
    fun test_decode_meta() {
        val meta = json.decodeFromString<AtlasPage>(pageJson).meta
        assertEquals("littlekt", meta.app)
        assertEquals("tiles.png", meta.image)
        assertEquals("RGBA8888", meta.format)
        assertEquals(2f, meta.scale)
        assertEquals(AtlasPage.Size(64, 32), meta.size)
        assertEquals(listOf(AtlasPage.FrameTag("idle", 0, 1, "forward")), meta.frameTags)
        assertEquals(listOf("tiles1.json"), meta.relatedMultiPacks)
    }

    @Test
    fun test_decode_meta_defaults() {
        val meta = json.decodeFromString<AtlasPage>("""{"frames":[],"meta":{}}""").meta
        assertEquals("", meta.app)
        assertEquals("1.0.0", meta.version)
        assertEquals(1f, meta.scale)
        assertEquals(AtlasPage.Size(0, 0), meta.size)
        assertTrue(meta.frameTags.isEmpty())
        assertTrue(meta.relatedMultiPacks.isEmpty())
    }

    @Test
    fun test_decode_page_filter_defaults() {
        val page = json.decodeFromString<AtlasPage>(pageJson)
        assertFalse(page.filterMin)
        assertFalse(page.filterMag)
        assertFalse(page.repeatX)
        assertFalse(page.repeatY)
    }

    @Test
    fun test_decode_slices() {
        val meta =
            json
                .decodeFromString<AtlasPage>(
                    """{"frames":[],"meta":{"slices":[{"name":"btn","color":"#ff0000","key":[{"frame":0,"bound":{"x":1,"y":2,"w":3,"h":4}}]}]}}"""
                )
                .meta
        val slice = meta.slices.single()
        assertEquals("btn", slice.name)
        assertEquals(AtlasPage.Key(0, AtlasPage.Rect(1, 2, 3, 4)), slice.key.single())
    }

    @Test
    fun test_atlas_info_flattens_frames_across_pages() {
        val info = AtlasInfo(AtlasPage.Meta(), listOf(page("a.png", "a0", "a1"), page("b.png", "b0")))
        assertEquals(listOf("a0", "a1", "b0"), info.frames.map { it.name })
    }

    @Test
    fun test_atlas_info_frames_map_by_name() {
        val info = AtlasInfo(AtlasPage.Meta(), listOf(page("a.png", "a0"), page("b.png", "b0")))
        assertEquals("b0", info.framesMap["b0"]?.name)
        assertEquals(null, info.framesMap["missing"])
    }

    @Test
    fun test_atlas_info_frames_map_last_duplicate_wins() {
        val first = page("a.png", "dup")
        val second = page("b.png", "dup")
        val info = AtlasInfo(AtlasPage.Meta(), listOf(first, second))
        assertEquals(2, info.frames.size)
        assertTrue(info.framesMap["dup"] === second.frames[0])
    }
}
