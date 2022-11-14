package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.node.component.AlignMode
import com.lehaine.littlekt.graph.node.component.Theme

/**
 * A [Container] that handles both vertical and horizontal alignments of its children.
 * @see VBoxContainer
 * @see HBoxContainer
 * @author Colton Daily
 * @date 1/2/2022
 */
abstract class BoxContainer : Container() {

    class ThemeVars {
        val separation = "separation"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [BoxContainer]
         */
        val themeVars = ThemeVars()
    }

    protected var vertical: Boolean = false

    var separation: Int
        get() = getThemeConstant(themeVars.separation)
        set(value) {
            constantOverrides[themeVars.separation] = value
        }
    var align: AlignMode = AlignMode.BEGIN

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var first = true
        _internalMinWidth = 0f
        _internalMinHeight = 0f
        nodes.forEach {
            if (it is Control && it.enabled && !it.isDestroyed) {
                if (vertical) {
                    if (it.combinedMinWidth > _internalMinWidth) {
                        _internalMinWidth = it.combinedMinWidth
                    }
                    _internalMinHeight += it.combinedMinHeight + if (first) 0f else separation.toFloat()
                } else {
                    if (it.combinedMinHeight > _internalMinHeight) {
                        _internalMinHeight = it.combinedMinHeight
                    }
                    _internalMinWidth += it.combinedMinWidth + if (first) 0f else separation.toFloat()
                }
                first = false
            }

        }
        minSizeInvalid = false
    }

    private val minSizeCache = mutableMapOf<Control, MinSizeCache>()

    override fun onSortChildren() {
        /** First pass, determine minimum size AND amount of stretchable elements */
        var first: Boolean

        var childrenCount = 0
        var stretchMin = 0
        var stretchAvail = 0
        var stretchRatioTotal = 0f
        minSizeCache.clear()

        nodes.forEach {
            if (it is Control) {
                if (it.enabled && it.visible && !it.isDestroyed) {
                    val minSize: Int
                    val willStretch: Boolean

                    if (vertical) {
                        stretchMin += it.combinedMinHeight.toInt()
                        minSize = it.combinedMinHeight.toInt()
                        willStretch = it.verticalSizeFlags.isFlagSet(SizeFlag.EXPAND)
                    } else {
                        stretchMin += it.combinedMinWidth.toInt()
                        minSize = it.combinedMinWidth.toInt()
                        willStretch = it.horizontalSizeFlags.isFlagSet(SizeFlag.EXPAND)
                    }

                    if (willStretch) {
                        stretchAvail += minSize
                        stretchRatioTotal += it.stretchRatio
                    }
                    minSizeCache[it] = MinSizeCache(minSize = minSize, willStretch = willStretch, finalSize = minSize)
                }
                childrenCount++
            }
        }

        if (childrenCount == 0) {
            return
        }

        val stretchMax = (if (vertical) height.toInt() else width.toInt()) - (childrenCount - 1) * separation
        var stretchDiff = stretchMax - stretchMin
        if (stretchDiff < 0) {
            stretchDiff = 0
        }
        stretchAvail += stretchDiff

        /** Second, pass successively to discard elements that can't be stretched, this will run while stretchable
        elements exist */

        var hasStretched = false

        while (stretchRatioTotal > 0) {
            hasStretched = true
            var refitSuccessful = true  //assume refit-test will go well
            var error = 0f  // Keep track of accumulated error in pixels

            nodes.forEach {
                if (it is Control && it.enabled && it.visible && !it.isDestroyed) {
                    val msc = minSizeCache[it] ?: return@forEach

                    if (msc.willStretch) {  //wants to stretch
                        // let's see if it can really stretch
                        val finalPixelSize = stretchAvail * it.stretchRatio / stretchRatioTotal
                        // add leftover fractional pixels to error accumulator
                        error += finalPixelSize - finalPixelSize.toInt()
                        if (finalPixelSize < msc.minSize) {
                            // if available stretching area is too small for widget,
                            // then remove it from stretching area
                            msc.willStretch = false
                            stretchRatioTotal -= it.stretchRatio
                            refitSuccessful = false
                            stretchAvail -= msc.minSize
                            msc.finalSize = msc.minSize
                            return@forEach
                        } else {
                            msc.finalSize = finalPixelSize.toInt()
                            if (error >= 1f) {
                                msc.finalSize += 1
                                error -= 1
                            }
                        }
                    }
                }
            }

            if (refitSuccessful) { // uf refit went well, break
                break
            }
        }

        /** Final pass, draw and stretch elements **/
        var ofs = 0

        if (!hasStretched) {
            if (vertical) {
                when (align) {
                    AlignMode.BEGIN -> {
                        // do nothing
                    }

                    AlignMode.CENTER -> ofs = stretchDiff / 2
                    AlignMode.END -> {
                        ofs = stretchDiff
                    }
                }
            } else { // horizontal
                when (align) {
                    AlignMode.BEGIN -> {
                        // todo handle rtl: ofs = stretchDiff
                    }

                    AlignMode.CENTER -> ofs = stretchDiff / 2
                    AlignMode.END -> {
                        // todo handle if !rtl
                        ofs = stretchDiff
                    }
                }
            }
        }

        first = true
        var idx = 0

        if (childrenCount > 0) {
            nodes.forEach { child ->
                if (child is Control && child.enabled && child.visible && !child.isDestroyed) {
                    val msc = minSizeCache[child] ?: return@forEach
                    if (first) {
                        first = false
                    } else {
                        ofs += separation
                    }

                    val from = ofs
                    var to = ofs + msc.finalSize

                    if (msc.willStretch && idx == childrenCount - 1) {
                        // adjust so the last one always fits perfect
                        // compensating for numerical imprecision
                        to = if (vertical) height.toInt() else width.toInt()
                    }

                    val size = to - from
                    val tx: Float
                    val ty: Float
                    val tWidth: Float
                    val tHeight: Float

                    if (vertical) {
                        tx = 0f
                        ty = from.toFloat()
                        tWidth = width
                        tHeight = size.toFloat()
                    } else {
                        tx = from.toFloat()
                        ty = 0f
                        tWidth = size.toFloat()
                        tHeight = height
                    }

                    fitChild(child, tx, ty, tWidth, tHeight)
                    ofs = to
                    idx++
                }
            }
        }

    }


    private data class MinSizeCache(var minSize: Int = 0, var willStretch: Boolean = false, var finalSize: Int = 0)
}