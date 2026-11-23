package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextureControlTests {

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = Theme()
    }

    private val texture = TestTexture(64, 64)
    private val slice = TextureSlice(texture)
    private val batch = TestBatch()
    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer(batch, slice)

    private fun progress(mode: TextureProgress.FillMode, ratio: Float): SliceDraw {
        val bar =
            TextureProgress().apply {
                progressBar = slice
                fillMode = mode
                this.ratio = ratio
                size(128f, 128f)
            }
        batch.draws.clear()
        bar.render(batch, camera, shapes)
        assertEquals(1, batch.draws.size)
        return batch.draws[0]
    }

    @Test
    fun test_texture_progress_bottom_to_top_crops_the_bottom_rows() {
        val draw = progress(TextureProgress.FillMode.BOTTOM_TO_TOP, 0.25f)
        assertEquals(SliceDraw(0f, 0f, 0f, 0f, 128f, 32f, 0, 48, 64, 16), draw)
    }

    @Test
    fun test_texture_progress_top_to_bottom_crops_the_top_rows() {
        val draw = progress(TextureProgress.FillMode.TOP_TO_BOTTOM, 0.25f)
        assertEquals(SliceDraw(0f, 0f, 0f, -96f, 128f, 32f, 0, 0, 64, 16), draw)
    }

    @Test
    fun test_texture_progress_left_to_right_crops_the_left_columns() {
        val draw = progress(TextureProgress.FillMode.LEFT_TO_RIGHT, 0.5f)
        assertEquals(SliceDraw(0f, 0f, 0f, 0f, 64f, 128f, 0, 0, 32, 64), draw)
    }

    @Test
    fun test_texture_progress_right_to_left_crops_the_right_columns() {
        val draw = progress(TextureProgress.FillMode.RIGHT_TO_LEFT, 0.5f)
        assertEquals(SliceDraw(0f, 0f, -64f, 0f, 64f, 128f, 32, 0, 32, 64), draw)
    }

    @Test
    fun test_texture_progress_draws_background_at_control_size() {
        val bar =
            TextureProgress().apply {
                background = slice
                size(32f, 16f)
            }
        batch.draws.clear()
        bar.render(batch, camera, shapes)
        assertEquals(32f, batch.draws[0].width)
        assertEquals(16f, batch.draws[0].height)
    }

    @Test
    fun test_texture_progress_empty_draws_nothing() {
        val bar =
            TextureProgress().apply {
                progressBar = slice
                ratio = 0f
            }
        batch.draws.clear()
        bar.render(batch, camera, shapes)
        assertTrue(batch.draws.isEmpty())
    }

    @Test
    fun test_texture_rect_ignore_size_drops_texture_min_size() {
        val rect = TextureRect().apply { this.slice = this@TextureControlTests.slice }
        assertEquals(64f, rect.combinedMinWidth)
        rect.expandMode = TextureRect.ExpandMode.IGNORE_SIZE
        rect.minWidth = 16f
        assertEquals(16f, rect.combinedMinWidth)
        assertEquals(0f, rect.combinedMinHeight)
    }

    @Test
    fun test_texture_progress_ignore_size_drops_texture_min_size() {
        val bar = TextureProgress().apply { background = slice }
        assertEquals(64f, bar.combinedMinHeight)
        bar.expandMode = TextureRect.ExpandMode.IGNORE_SIZE
        assertEquals(0f, bar.combinedMinHeight)
    }
}
