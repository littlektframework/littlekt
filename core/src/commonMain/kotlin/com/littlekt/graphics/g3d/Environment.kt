package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.log.Logger
import com.littlekt.util.UniqueId
import kotlin.time.Duration

/**
 * @param buffers the buffers used for the first view of each frame.
 * @param viewBuffersFactory creates the buffers for any additional views within a frame.
 * @author Colton Daily
 * @date 1/5/2025
 */
open class Environment(
    open val buffers: CameraBuffersViaCamera,
    private val viewBuffersFactory: (() -> CameraBuffersViaCamera)? = null,
) : Releasable {
    val id = UniqueId.next<Environment>()

    /** The max amount of different views that may be rendered within a single frame. */
    var maxViewsPerFrame: Int = 8

    internal var flushId = -1L
    internal var flushView: CameraBuffersViaCamera? = null

    private val extraViews = mutableListOf<CameraBuffersViaCamera>()
    private val viewStates = mutableListOf<ViewState>()
    private var viewFrame = -1L
    private var viewIndex = 0

    /**
     * Begins a view for the given [camera] for the current frame. If the [camera] has already been
     * used in the current frame, and it hasn't changed, then that existing view is reused.
     *
     * @return the [CameraBuffersViaCamera] for the view.
     */
    fun beginView(camera: Camera, dt: Duration): CameraBuffersViaCamera {
        val frame = EngineStats.frame.value
        if (viewFrame != frame) {
            viewFrame = frame
            viewIndex = 0
        }
        for (i in 0 until viewIndex) {
            if (viewStates[i].matches(camera)) return getView(i)
        }
        if (viewIndex >= maxViewsPerFrame) {
            logger.warn {
                "Environment $id has exceeded maxViewsPerFrame ($maxViewsPerFrame). Reusing the last view which will overwrite its data!"
            }
            viewIndex = maxViewsPerFrame - 1
        }
        val index = viewIndex++
        if (index >= viewStates.size) {
            viewStates += ViewState()
        }
        viewStates[index].set(camera)
        val view = getView(index)
        updateView(index, view, camera, dt)
        return view
    }

    /** Updates the given [view] buffers with the [camera]. */
    protected open fun updateView(
        index: Int,
        view: CameraBuffersViaCamera,
        camera: Camera,
        dt: Duration,
    ) {
        view.update(camera, dt)
    }

    /** Creates a new [CameraBuffersViaCamera] for an additional view. */
    protected open fun createViewBuffers(): CameraBuffersViaCamera =
        viewBuffersFactory?.invoke()
            ?: error(
                "Environment $id is unable to create buffers for additional views. Pass in a viewBuffersFactory."
            )

    /** @return the buffers of the view at the given [index]. */
    protected fun getView(index: Int): CameraBuffersViaCamera {
        if (index == 0) return buffers
        while (extraViews.size < index) {
            extraViews += createViewBuffers()
        }
        return extraViews[index - 1]
    }

    /** The total amount of views that have been created. */
    protected val totalViews: Int
        get() = extraViews.size + 1

    override fun release() {
        buffers.release()
        extraViews.forEach { it.release() }
        extraViews.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Environment

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    private class ViewState {
        private var camera: Camera? = null
        private val view = FloatArray(16)
        private val projection = FloatArray(16)
        private var width = 0f
        private var height = 0f

        fun set(camera: Camera) {
            this.camera = camera
            camera.view.data.copyInto(view)
            camera.projection.data.copyInto(projection)
            width = camera.virtualWidth
            height = camera.virtualHeight
        }

        fun matches(camera: Camera): Boolean {
            if (this.camera !== camera) return false
            if (width != camera.virtualWidth || height != camera.virtualHeight) return false
            val viewData = camera.view.data
            val projData = camera.projection.data
            for (i in 0 until 16) {
                if (view[i] != viewData[i] || projection[i] != projData[i]) return false
            }
            return true
        }
    }

    companion object {
        private val logger = Logger<Environment>()
    }
}
