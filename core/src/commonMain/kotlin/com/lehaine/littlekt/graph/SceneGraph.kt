package com.lehaine.littlekt.graph

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.node2d.ui.Control
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.datastructure.Pool
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.viewport.ScreenViewport
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * Create a new scene graph with a [callback] with the [SceneGraph] in context.
 * @param context the current context
 * @param viewport the viewport that the camera of the scene graph will own
 * @param batch an option sprite batch. If omitted, the scene graph will create and manage its own.
 * @param callback the callback that is invoked with a [SceneGraph] context
 * in order to initialize any values and create nodes
 * @return the newly created [SceneGraph]
 */
@OptIn(ExperimentalContracts::class)
inline fun sceneGraph(
    context: Context,
    viewport: Viewport = ScreenViewport(context.graphics.width, context.graphics.height),
    batch: Batch? = null,
    callback: @SceneGraphDslMarker SceneGraph.() -> Unit = {}
): SceneGraph {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return SceneGraph(context, viewport, batch).also(callback)
}

/**
 * A class for creating a scene graph of nodes.
 * @param context the current context
 * @param viewport the viewport that the camera of the scene graph will own
 * @param batch an option sprite batch. If omitted, the scene graph will create and manage its own.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class SceneGraph(
    val context: Context,
    val viewport: Viewport = ScreenViewport(context.graphics.width, context.graphics.height),
    batch: Batch? = null,
) : InputProcessor, Disposable {

    private var ownsBatch = true
    val batch: Batch = batch?.also { ownsBatch = false } ?: SpriteBatch(context)

    val root: Node by lazy {
        Node().apply {
            scene = this@SceneGraph
            this.viewport = this@SceneGraph.viewport
        }
    }

    val width: Int get() = viewport.virtualWidth
    val height: Int get() = viewport.virtualHeight

    private val camera = OrthographicCamera(context.graphics.width, context.graphics.height).apply {
        this.viewport = this@SceneGraph.viewport
    }

    private var frameCount = 0

    // scene input related fields
    private var mouseScreenX: Float = 0f
    private var mouseScreenY: Float = 0f
    private var mouseOverControl: Control? = null
    private var keyboardFocus: Control? = null
    private val touchFocusPool = Pool(reset = { it.reset() }, preallocate = 1) { TouchFocus() }
    private val inputEventPool = Pool(reset = { it.reset() }, preallocate = 10) { InputEvent() }
    private val touchFocuses = ArrayList<TouchFocus>(4)
    private val pointerScreenX = FloatArray(20)
    private val pointerScreenY = FloatArray(20)
    private val pointerOverControls = arrayOfNulls<Control>(20)
    private val pointerTouched = BooleanArray(20)

    private val tempVec = MutableVec2f()

    private var initialized = false

    private var shift = false

    /**
     * Resizes the internal graph's [OrthographicCamera] and [Viewport].
     */
    fun resize(width: Int, height: Int) {
        camera.update(width, height, context)
    }

    /**
     * Initializes the root [Node] and [InputProcessor]. This must be called before an [update] or [render] calls.
     */
    fun initialize() {
        context.input.addInputProcessor(this)
        root.initialize()
        onStart()
        root._onPostEnterScene()
        initialized = true
    }

    /**
     * Renders the entire tree.
     */
    fun render() {
        if (!initialized) error("You need to call 'initialize()' once before doing any rendering or updating!")
        viewport.apply(context)
        batch.use(camera.viewProjection) {
            root._render(batch, camera)
        }
    }


    /**
     * Lifecycle method. This is called whenever the [SceneGraph] is set before [initialize] is called.
     * Any nodes added to this [Node] context won't be added until the next frame update.
     */
    open fun Node.initialize() = Unit

    /**
     * Lifecycle method. This is called when this scene becomes the active scene.
     */
    open fun onStart() = Unit

    /**
     * Open method that is triggered whenever a [Control] node receives an input event.
     */
    open fun uiInput(control: Control, event: InputEvent) {}

    /**
     * Request a [Control] to receive keyboard focus.
     */
    fun requestFocus(control: Control) {
        if (keyboardFocus == control) return
        val oldFocus = keyboardFocus
        keyboardFocus = control
        oldFocus?._onFocusLost()
        control._onFocus()
    }

    /**
     * Releases any current keyboard focus.
     */
    fun releaseFocus() {
        val control = keyboardFocus
        keyboardFocus = null
        control?._onFocusLost()
    }

    /**
     * Checks if the [Control] has the current keyboard focus.
     */
    fun hasFocus(control: Control) = keyboardFocus == control

    /**
     * Updates all the nodes in the tree.
     */
    open fun update(dt: Duration) {
        if (!initialized) error("You need to call 'initialize()' once before doing any rendering or updating!")

        pointerOverControls.forEachIndexed { index, overLast ->
            if (!pointerTouched[index]) {
                if (overLast != null) {
                    pointerOverControls[index] = null
                    screenToSceneCoordinates(
                        tempVec.set(
                            pointerScreenX[index],
                            pointerScreenY[index]
                        )
                    )
                    val event = inputEventPool.alloc().apply {
                        sceneX = tempVec.x
                        sceneY = tempVec.y
                        overLast.toLocal(tempVec, tempVec)
                        localX = tempVec.x
                        localY = tempVec.y
                        pointer = Pointer.cache[index]
                    }
                    overLast._uiInput(event)
                    uiInput(overLast, event)
                    inputEventPool.free(event)
                }
                return@forEachIndexed
            }
            pointerOverControls[index] =
                fireEnterAndExit(overLast, pointerScreenX[index], pointerScreenY[index], Pointer.cache[index])
        }

        when (context.platform) {
            Context.Platform.DESKTOP, Context.Platform.WEBGL, Context.Platform.WEBGL2 -> {
                mouseOverControl = fireEnterAndExit(mouseOverControl, mouseScreenX, mouseScreenY, Pointer.POINTER1)
            }
            else -> {
                // do nothing
            }
        }

        camera.update()
        if (root.enabled && (root.updateInterval == 1 || frameCount % root.updateInterval == 0)) {
            root._update(dt)
        }
        frameCount++
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (!isInsideViewport(screenX.toInt(), screenY.toInt())) return false

        pointerTouched[pointer.ordinal] = true
        pointerScreenX[pointer.ordinal] = screenX
        pointerScreenY[pointer.ordinal] = screenY

        screenToSceneCoordinates(tempVec.set(screenX, screenY))

        val sceneX = tempVec.x
        val sceneY = tempVec.y

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.TOUCH_DOWN
            this.sceneX = sceneX
            this.sceneY = sceneY
            this.pointer = pointer
        }

        val target = hit(tempVec.x, tempVec.y)
        target?.let {
            if (pointer == Pointer.MOUSE_LEFT && it.focusMode != Control.FocusMode.NONE) {
                it.grabFocus()
                keyboardFocus = it
            }
            it.toLocal(sceneX, sceneY, tempVec)
            event.apply {
                localX = tempVec.x
                localY = tempVec.y
            }
            it._uiInput(event)
            uiInput(it, event)
            addTouchFocus(it, pointer)
        }
        val handled = event.handled
        inputEventPool.free(event)
        return handled
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        pointerTouched[pointer.ordinal] = false
        pointerScreenX[pointer.ordinal] = screenX
        pointerScreenY[pointer.ordinal] = screenY

        if (touchFocuses.isEmpty()) {
            return false
        }

        screenToSceneCoordinates(tempVec.set(screenX, screenY))

        val sceneX = tempVec.x
        val sceneY = tempVec.y

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.TOUCH_UP
            this.sceneX = sceneX
            this.sceneY = sceneY
            this.pointer = pointer
        }

        touchFocuses.fastForEach { focus ->
            if (focus.pointer != pointer) {
                return@fastForEach
            }
            if (!touchFocuses.contains(focus)) { // focus already gone
                return@fastForEach
            }
            focus.target?.let {
                it.toLocal(sceneX, sceneY, tempVec)
                event.apply {
                    localX = tempVec.x
                    localY = tempVec.y
                }
                it._uiInput(event)
                uiInput(it, event)
                event.handle()
            }
            touchFocusPool.free(focus)
        }

        val handled = event.handled
        inputEventPool.free(event)
        return handled
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        pointerScreenX[pointer.ordinal] = screenX
        pointerScreenY[pointer.ordinal] = screenY
        mouseScreenX = screenX
        mouseScreenY = screenY

        if (touchFocuses.isEmpty()) {
            return false
        }

        screenToSceneCoordinates(tempVec.set(screenX, screenY))

        val sceneX = tempVec.x
        val sceneY = tempVec.y

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.TOUCH_DRAGGED
            this.sceneX = sceneX
            this.sceneY = sceneY
            this.pointer = pointer
        }

        touchFocuses.fastForEach { focus ->
            if (focus.pointer != pointer) {
                return@fastForEach
            }
            if (!touchFocuses.contains(focus)) { // focus already gone
                return@fastForEach
            }
            focus.target?.let {
                it.toLocal(sceneX, sceneY, tempVec)
                event.apply {
                    localX = tempVec.x
                    localY = tempVec.y
                }
                it._uiInput(event)
                uiInput(it, event)
                event.handle()
            }
        }

        val handled = event.handled
        inputEventPool.free(event)
        return handled
    }

    override fun mouseMoved(screenX: Float, screenY: Float): Boolean {
        mouseScreenX = screenX
        mouseScreenY = screenY

        return false
    }

    override fun keyDown(key: Key): Boolean {
        if (key == Key.SHIFT_LEFT) {
            shift = true
        }

        keyboardFocus?.let {
            val event = inputEventPool.alloc().apply {
                type = InputEvent.Type.KEY_DOWN
                this.key = key
            }
            it._uiInput(event)
            uiInput(it, event)
            val handled = event.handled
            inputEventPool.free(event)

            if (handled) return true

            var next: Control? = null
            when (key) {
                Key.TAB -> {
                    next = if (shift) {
                        it.findPreviousValidFocus()
                    } else {
                        it.findNextValidFocus()
                    }
                }
                Key.ARROW_UP -> {
                    next = it.getFocusNeighbor(Control.Side.TOP)
                }
                Key.ARROW_RIGHT -> {
                    next = it.getFocusNeighbor(Control.Side.RIGHT)
                }
                Key.ARROW_DOWN -> {
                    next = it.getFocusNeighbor(Control.Side.BOTTOM)
                }
                Key.ARROW_LEFT -> {
                    next = it.getFocusNeighbor(Control.Side.LEFT)
                }
                else -> Unit
            }

            next?.grabFocus()
        }

        return false
    }

    override fun keyUp(key: Key): Boolean {
        if (key == Key.SHIFT_LEFT) {
            shift = false
        }

        var handled = false
        keyboardFocus?.let {
            val event = inputEventPool.alloc().apply {
                type = InputEvent.Type.KEY_UP
                this.key = key
            }
            it._uiInput(event)
            uiInput(it, event)
            handled = event.handled
            inputEventPool.free(event)
        }
        return handled
    }

    override fun keyRepeat(key: Key): Boolean {
        var handled = false
        keyboardFocus?.let {
            val event = inputEventPool.alloc().apply {
                type = InputEvent.Type.KEY_REPEAT
                this.key = key
            }
            it._uiInput(event)
            uiInput(it, event)
            handled = event.handled
            inputEventPool.free(event)
        }
        return handled
    }

    override fun charTyped(character: Char): Boolean {
        var handled = false
        keyboardFocus?.let {
            val event = inputEventPool.alloc().apply {
                type = InputEvent.Type.CHAR_TYPED
                char = character
            }
            it._uiInput(event)
            uiInput(it, event)
            handled = event.handled
            inputEventPool.free(event)
        }
        return handled
    }

    private fun fireEnterAndExit(overLast: Control?, screenX: Float, screenY: Float, pointer: Pointer): Control? {
        screenToSceneCoordinates(tempVec.set(screenX, screenY))

        val sceneX = tempVec.x
        val sceneY = tempVec.y
        val over = hit(tempVec.x, tempVec.y)
        if (over == overLast) return overLast

        if (overLast != null) {
            val event = inputEventPool.alloc().apply {
                this.sceneX = sceneX
                this.sceneY = sceneY
                this.pointer = pointer
                overLast.toLocal(sceneX, sceneY, tempVec)
                localX = tempVec.x
                localY = tempVec.y
                type = InputEvent.Type.MOUSE_EXIT
            }
            overLast.let {
                it._uiInput(event)
                uiInput(it, event)
            }
            inputEventPool.free(event)
        }

        if (over != null) {
            val event = inputEventPool.alloc().apply {
                this.sceneX = sceneX
                this.sceneY = sceneY
                this.pointer = pointer
                over.toLocal(sceneX, sceneY, tempVec)
                localX = tempVec.x
                localY = tempVec.y
                type = InputEvent.Type.MOUSE_ENTER
            }
            over.let {
                it._uiInput(event)
                uiInput(it, event)
            }
            inputEventPool.free(event)
        }
        return over
    }


    private fun addTouchFocus(target: Control, pointer: Pointer) {
        touchFocusPool.alloc().apply {
            this.target = target
            this.pointer = pointer
        }.also { touchFocuses.add(it) }
    }

    private fun hit(hx: Float, hy: Float): Control? {
        root.nodes.forEachReversed {
            if (it !is Control) return@forEachReversed
            val target = it.hit(hx, hy)
            if (target != null) {
                return target
            }
        }
        return null
    }

    private fun screenToSceneCoordinates(vector2: MutableVec2f): MutableVec2f {
        camera.unProjectScreen(vector2, context, vector2)
        return vector2
    }

    private fun isInsideViewport(x: Int, y: Int): Boolean {
        val x0 = viewport.x
        val x1 = x0 + viewport.width
        val y0 = viewport.y
        val y1 = y0 + viewport.height
        val screenY = context.graphics.height - 1 - y
        return x in x0 until x1 && screenY in y0 until y1
    }

    /**
     * Lifecycle method. Do any necessary unloading / disposing here. This is called when this scene is removed
     * from the active slot.
     */
    override fun dispose() {
        root.destroy()
        if (ownsBatch) {
            batch.dispose()
        }
        context.input.removeInputProcessor(this)
    }

    private class TouchFocus {
        var target: Control? = null
        var pointer: Pointer = Pointer.POINTER1

        fun reset() {
            target = null
        }
    }
}