package com.littlekt.graph.node.annotation

/**
 * Marks the [com.littlekt.graph.node.resource.theme] builder DSL so that outer theme scopes can't
 * be used by mistake in an inner one.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@DslMarker annotation class ThemeDslMarker
