package com.littlekt.util.viewport

import com.littlekt.graphics.webgpu.RenderPassEncoder

fun RenderPassEncoder.setViewport(viewport: Viewport) {
    setViewport(viewport.x, viewport.y, viewport.width, viewport.height)
}
