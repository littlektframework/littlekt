package com.lehaine.littlekt.graph

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graph.node.CanvasLayer
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.ui.Control
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.input.*
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.datastructure.Pool
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.seconds
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
    viewport: Viewport = ScreenViewport(context.graphics.width,
        context.graphics.height),
    batch: Batch? = null,
    controller: InputMapController<String>? = null,
    callback: @SceneGraphDslMarker SceneGraph<String>.() -> Unit = {},
): SceneGraph<String> {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    val signals = SceneGraph.UiInputSignals(
        "ui_accept",
        "ui_select",
        "ui_cancel",
        "ui_focus_next",
        "ui_focus_prev",
        "ui_left",
        "ui_right",
        "ui_up",
        "ui_down",
        "ui_home",
        "ui_end"
    )
    return SceneGraph(
        context,
        viewport,
        batch,
        signals,
        controller ?: createDefaultSceneGraphController(context.input, signals)
    ).also(callback)
}

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
inline fun <InputSignal> sceneGraph(
    context: Context,
    viewport: Viewport = ScreenViewport(context.graphics.width,
        context.graphics.height),
    batch: Batch? = null,
    uiInputSignals: SceneGraph.UiInputSignals<InputSignal> = SceneGraph.UiInputSignals(),
    controller: InputMapController<InputSignal> = InputMapController(context.input),
    callback: @SceneGraphDslMarker SceneGraph<InputSignal>.() -> Unit = {},
): SceneGraph<InputSignal> {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return SceneGraph(
        context,
        viewport,
        batch,
        uiInputSignals,
        controller
    ).also(callback)
}

fun <InputSignal> InputMapController<InputSignal>.addDefaultUiInput(uiInputSignals: SceneGraph.UiInputSignals<InputSignal>) {
    uiInputSignals.uiAccept?.let {
        addBinding(
            it,
            keys = listOf(Key.SPACE, Key.ENTER),
            buttons = listOf(GameButton.XBOX_A)
        )
    }
    uiInputSignals.uiSelect?.let { addBinding(it, keys = listOf(Key.SPACE), buttons = listOf(GameButton.XBOX_Y)) }
    uiInputSignals.uiCancel?.let { addBinding(it, keys = listOf(Key.ESCAPE), buttons = listOf(GameButton.XBOX_B)) }
    uiInputSignals.uiFocusNext?.let { addBinding(it, keys = listOf(Key.TAB)) }
    uiInputSignals.uiFocusPrev?.let {
        addBinding(
            it,
            keys = listOf(Key.TAB),
            keyModifiers = listOf(InputMapController.KeyModifier.SHIFT)
        )
    }
    uiInputSignals.uiUp?.let { addBinding(it, keys = listOf(Key.ARROW_UP), buttons = listOf(GameButton.UP)) }
    uiInputSignals.uiDown?.let { addBinding(it, keys = listOf(Key.ARROW_DOWN), buttons = listOf(GameButton.DOWN)) }
    uiInputSignals.uiLeft?.let { addBinding(it, keys = listOf(Key.ARROW_LEFT), buttons = listOf(GameButton.LEFT)) }
    uiInputSignals.uiRight?.let {
        addBinding(
            it,
            keys = listOf(Key.ARROW_RIGHT),
            buttons = listOf(GameButton.RIGHT)
        )
    }
    uiInputSignals.uiHome?.let { addBinding(it, keys = listOf(Key.HOME)) }
    uiInputSignals.uiEnd?.let { addBinding(it, keys = listOf(Key.END)) }
}

fun <InputSignal> createDefaultSceneGraphController(
    input: Input,
    uiInputSignals: SceneGraph.UiInputSignals<InputSignal>,
): InputMapController<InputSignal> =
    InputMapController<InputSignal>(input).also { it.addDefaultUiInput(uiInputSignals) }

/**
 * A class for creating a scene graph of nodes.
 * @param context the current context
 * @param viewport the viewport that the camera of the scene graph will own
 * @param batch an option sprite batch. If omitted, the scene graph will create and manage its own.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class SceneGraph<InputType>(
    val context: Context,
    viewport: Viewport = ScreenViewport(context.graphics.width,
        context.graphics.height),
    batch: Batch? = null,
    val uiInputSignals: UiInputSignals<InputType> = UiInputSignals(),
    val controller: InputMapController<InputType> = createDefaultSceneGraphController(
        context.input,
        uiInputSignals
    ),
) : InputMapProcessor<InputType>, Disposable {
    private var ownsBatch = true
    val batch: Batch = batch?.also { ownsBatch = false } ?: SpriteBatch(context)

    val sceneCanvas: CanvasLayer by lazy {
        CanvasLayer().apply {
            name = "Scene Viewport"
            this.viewport = viewport
        }
    }

    val root: Node by lazy {
        Node().apply { name = "Root" }.addTo(sceneCanvas)
    }

    val width: Int get() = sceneCanvas.virtualWidth
    val height: Int get() = sceneCanvas.virtualHeight

    var targetFPS = 60
    var tmod: Float = 1f
        private set

    var dt: Duration = Duration.ZERO
        private set

    private var frameCount = 0

    // scene input related fields
    private var mouseScreenX: Float = 0f
    private var mouseScreenY: Float = 0f
    private var mouseOverControl: Control? = null
    private var keyboardFocus: Control? = null
    private val touchFocusPool = Pool(reset = { it.reset() }, preallocate = 1) { TouchFocus() }
    private val inputEventPool = Pool(reset = { it.reset() }, preallocate = 10) { InputEvent<InputType>() }
    private val touchFocuses = ArrayList<TouchFocus>(4)
    private val pointerScreenX = FloatArray(20)
    private val pointerScreenY = FloatArray(20)
    private val pointerOverControls = arrayOfNulls<Control>(20)
    private val pointerTouched = BooleanArray(20)
    private val viewportsApplied = mutableListOf<Viewport>()

    private val tempVec = MutableVec2f()

    private var initialized = false

    private val unhandledInputQueue = ArrayDeque<InputEvent<InputType>>(20)

    /**
     * Resizes the internal graph's [OrthographicCamera] and [CanvasLayer].
     * @param centerCamera if true will center the graphs internal camera after resizing the viewport
     */
    open fun resize(width: Int, height: Int, centerCamera: Boolean = false) {
        sceneCanvas.propagateResize(width, height, centerCamera)
    }

    /**
     * Initializes the root [Node] and [InputProcessor]. This must be called before an [update] or [render] calls.
     */
    open suspend fun initialize() {
        controller.addInputMapProcessor(this)
        context.input.addInputProcessor(this)
        sceneCanvas.scene = this
        root.initialize()
        onStart()
        initialized = true
    }

    /**
     * Renders the entire tree.
     * @param onNodeRender invoked when a node receives a `render` call. Useful when extending the [SceneGraph]
     * and providing custom logic before each render such as camera culling.
     */
    open fun render(onNodeRender: ((Node, Batch, Camera) -> Unit)? = null) {
        if (!initialized) error("You need to call 'initialize()'once before doing any rendering or updating!")
        sceneCanvas.render(batch, onNodeRender)
        if (batch.drawing) batch.end()
    }

    /**
     * Lifecycle method. This is called whenever the [SceneGraph] is set before [initialize] is called.
     * Any nodes added to this [Node] context won't be added until the next frame update.
     */
    open suspend fun Node.initialize() = Unit

    /**
     * Lifecycle method. This is called when this scene becomes the active scene.
     */
    open fun onStart() = Unit

    /**
     * Open method that is triggered whenever a [Control] node receives an input event.
     */
    open fun uiInput(control: Control, event: InputEvent<InputType>) {}

    /**
     * Request a [Control] to receive keyboard focus.
     */
    fun requestFocus(control: Control) {
        if (keyboardFocus == control) return
        if (!control.enabled) return
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
        this.dt = dt
        tmod = dt.seconds * targetFPS
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
                    overLast.callUiInput(event)
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

        unhandledInputQueue.forEach {
            callUnhandledInput(it)
            inputEventPool.free(it)
        }
        unhandledInputQueue.clear()

        if (root.enabled && (root.updateInterval == 1 || frameCount % root.updateInterval == 0)) {
            root.propagateUpdate()
        }
        frameCount++
    }

    internal fun pushViewport(viewport: Viewport) {
        viewportsApplied += viewport
        viewport.apply(context)
    }

    internal fun popViewport() {
        if (viewportsApplied.isNotEmpty()) {
            viewportsApplied.removeLast()
            if (viewportsApplied.isNotEmpty()) {
                viewportsApplied.last().apply(context)
            }
        }
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (!isInsideViewport(screenX.toInt(), screenY.toInt())) return false
        if (controller.touchDown(screenX, screenY, pointer)) return true

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

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        val target = callHitTest(tempVec.x, tempVec.y)
        target?.let {
            if (pointer == Pointer.MOUSE_LEFT && it.focusMode != Control.FocusMode.NONE) {
                it.grabFocus()
                keyboardFocus = it
            }
            it.callUiInput(event)
            uiInput(it, event)
            addTouchFocus(it, pointer)
        }
        if (event.handled) {
            inputEventPool.free(event)
            return true
        }

        unhandledInputQueue += event
        return false
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (controller.touchUp(screenX, screenY, pointer)) return true

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

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
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
                it.callUiInput(event)
                uiInput(it, event)
                event.handle()
            }
            touchFocusPool.free(focus)
        }

        if (event.handled) {
            inputEventPool.free(event)
            return true
        }

        unhandledInputQueue += event
        return false
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (controller.touchDragged(screenX, screenY, pointer)) return true

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

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
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
                it.callUiInput(event)
                uiInput(it, event)
                event.handle()
            }
        }

        if (event.handled) {
            inputEventPool.free(event)
            return true
        }

        unhandledInputQueue += event
        return false
    }

    override fun mouseMoved(screenX: Float, screenY: Float): Boolean {
        mouseScreenX = screenX
        mouseScreenY = screenY

        return false
    }

    override fun onActionDown(inputType: InputType): Boolean {
        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.ACTION_DOWN
            this.inputType = inputType
        }

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            var handled = event.handled

            var next: Control? = null
            when (inputType) {
                uiInputSignals.uiFocusNext -> {
                    next = it.findNextValidFocus()
                    handled = true
                }
                uiInputSignals.uiFocusPrev -> {
                    next = it.findPreviousValidFocus()
                    handled = true
                }
                uiInputSignals.uiUp -> {
                    next = it.getFocusNeighbor(Control.Side.TOP)
                    handled = true
                }
                uiInputSignals.uiRight -> {
                    next = it.getFocusNeighbor(Control.Side.RIGHT)
                    handled = true
                }
                uiInputSignals.uiDown -> {
                    next = it.getFocusNeighbor(Control.Side.BOTTOM)
                    handled = true
                }
                uiInputSignals.uiLeft -> {
                    next = it.getFocusNeighbor(Control.Side.LEFT)
                    handled = true
                }
                else -> Unit
            }

            next?.grabFocus()
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }

        unhandledInputQueue += event
        return false
    }

    override fun onActionUp(inputType: InputType): Boolean {
        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.ACTION_UP
            this.inputType = inputType
        }

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }

        unhandledInputQueue += event
        return false
    }

    override fun onActionRepeat(inputType: InputType): Boolean {
        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.ACTION_REPEAT
            this.inputType = inputType
        }

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }
        unhandledInputQueue += event
        return false
    }

    override fun keyDown(key: Key): Boolean {
        if (controller.keyDown(key)) return true

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.KEY_DOWN
            this.key = key
        }
        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }

        unhandledInputQueue += event
        return false
    }

    override fun keyUp(key: Key): Boolean {
        if (controller.keyUp(key)) return true

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.KEY_UP
            this.key = key
        }

        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }

        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }
        unhandledInputQueue += event
        return false
    }

    override fun keyRepeat(key: Key): Boolean {
        if (controller.keyRepeat(key)) return true

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.KEY_REPEAT
            this.key = key
        }
        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }
        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
            }
        }
        unhandledInputQueue += event
        return false
    }

    override fun charTyped(character: Char): Boolean {
        if (controller.charTyped(character)) return true

        val event = inputEventPool.alloc().apply {
            type = InputEvent.Type.CHAR_TYPED
            char = character
        }
        // InputEvents go: input -> ui input -> unhandled input
        if (callInput(event)) {
            inputEventPool.free(event)
            return true
        }
        keyboardFocus?.let {
            it.callUiInput(event)
            uiInput(it, event)
            val handled = event.handled
            if (handled) {
                inputEventPool.free(event)
                return true
            }
        }
        unhandledInputQueue += event
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return controller.scrolled(amountX, amountY)
    }

    override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        return controller.gamepadButtonPressed(button, pressure, gamepad)
    }

    override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
        return controller.gamepadButtonReleased(button, gamepad)
    }

    override fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int): Boolean {
        return controller.gamepadJoystickMoved(stick, xAxis, yAxis, gamepad)
    }

    override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        return controller.gamepadTriggerChanged(button, pressure, gamepad)
    }

    private fun fireEnterAndExit(overLast: Control?, screenX: Float, screenY: Float, pointer: Pointer): Control? {
        screenToSceneCoordinates(tempVec.set(screenX, screenY))

        val sceneX = tempVec.x
        val sceneY = tempVec.y
        val over = callHitTest(tempVec.x, tempVec.y)
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
                it.callUiInput(event)
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
                it.callUiInput(event)
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

    private fun callHitTest(hx: Float, hy: Float): Control? {
        root.nodes.forEachReversed {
            val target = it.propagateHit(hx, hy)
            if (target != null) {
                return target
            }
        }
        return null
    }

    private fun callInput(event: InputEvent<InputType>): Boolean {
        root.nodes.forEachReversed {
            if (it.propagateInput(event)) {
                return true
            }
        }
        return false
    }

    private fun Node.propagateInput(event: InputEvent<InputType>): Boolean {
        nodes.forEachReversed {
            it.propagateInput(event)
            if (event.handled) {
                return true
            }
        }
        callInput(event)
        return event.handled
    }

    private fun callUnhandledInput(event: InputEvent<InputType>): Boolean {
        root.nodes.forEachReversed {
            if (it.propagateUnhandledInput(event)) {
                return true
            }
        }
        return false
    }

    private fun Node.propagateUnhandledInput(event: InputEvent<InputType>): Boolean {
        nodes.forEachReversed {
            it.propagateUnhandledInput(event)
            if (event.handled) {
                return true
            }
        }
        callUnhandledInput(event)
        return event.handled
    }

    fun screenToSceneCoordinates(inOut: MutableVec2f) = sceneCanvas.screenToCanvasCoordinates(inOut)

    fun sceneToScreenCoordinates(inOut: MutableVec2f) = sceneCanvas.canvasToScreenCoordinates(inOut)

    private fun isInsideViewport(x: Int, y: Int): Boolean {
        val x0 = sceneCanvas.x
        val x1 = x0 + sceneCanvas.width
        val y0 = sceneCanvas.y
        val y1 = y0 + sceneCanvas.height
        val screenY = context.graphics.height - 1 - y
        return x in x0 until x1 && screenY in y0 until y1
    }

    /**
     * Lifecycle method. Do any necessary unloading / disposing here. This is called when this scene is removed
     * from the active slot.
     */
    override fun dispose() {
        sceneCanvas.destroy()
        if (ownsBatch) {
            batch.dispose()
        }
        controller.removeInputMapProcessor(this)
        context.input.removeInputProcessor(this)
    }

    data class UiInputSignals<InputType>(
        val uiAccept: InputType? = null,
        val uiSelect: InputType? = null,
        val uiCancel: InputType? = null,
        val uiFocusNext: InputType? = null,
        val uiFocusPrev: InputType? = null,
        val uiLeft: InputType? = null,
        val uiRight: InputType? = null,
        val uiUp: InputType? = null,
        val uiDown: InputType? = null,
        val uiHome: InputType? = null,
        val uiEnd: InputType? = null,
    )

    private class TouchFocus {
        var target: Control? = null
        var pointer: Pointer = Pointer.POINTER1

        fun reset() {
            target = null
        }
    }
}