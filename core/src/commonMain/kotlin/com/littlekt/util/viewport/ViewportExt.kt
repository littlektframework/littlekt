package com.littlekt.util.viewport

import com.littlekt.graphics.webgpu.RenderPassEncoder

/**
 * Sets the viewport used during the rasterization stage to linear map from normalized device
 * coordinates to viewport coordinates.
 *
 * @param viewport viewport data use on the render pass
 */
fun RenderPassEncoder.setViewport(viewport: Viewport) {
    setViewport(viewport.x, viewport.y, viewport.width, viewport.height)
}
