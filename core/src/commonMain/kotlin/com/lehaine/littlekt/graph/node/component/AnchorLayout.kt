package com.lehaine.littlekt.graph.node.component

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
enum class AnchorLayout {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER_LEFT,
    CENTER_TOP,
    CENTER_RIGHT,
    CENTER_BOTTOM,
    CENTER,
    LEFT_WIDE,
    TOP_WIDE,
    RIGHT_WIDE,
    BOTTOM_WIDE,
    VCENTER_WIDE,
    HCENTER_WIDE,
    WIDE,

    /**
     * Anchors will need to be set manually.
     */
    NONE
}