package com.lehaine.littlekt.input

import com.lehaine.littlekt.input.internal.InternalInputEvent
import com.lehaine.littlekt.input.internal.InternalInputEventType
import com.lehaine.littlekt.util.datastructure.Pool

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputQueueManager {

    val currentEventTime get() = _currentEventTime

    private val eventsPool = Pool(preallocate = 10, gen = { InternalInputEvent() })

    private val queue = mutableListOf<InternalInputEvent>()
    private val processingQueue = mutableListOf<InternalInputEvent>()
    private var _currentEventTime = 0L

    fun drain(processor: InputProcessor?) {
        if (processor == null) {
            queue.clear()
            return
        }
        processingQueue.addAll(queue)
        queue.clear()

        processingQueue.forEach {
            _currentEventTime = it.queueTime
            when (it.type) {
                InternalInputEventType.KEY_DOWN -> processor.keyDown(key = it.key)
                InternalInputEventType.KEY_UP -> processor.keyUp(key = it.key)
                InternalInputEventType.KEY_TYPED -> processor.keyTyped(character = it.typedChar)
                InternalInputEventType.TOUCH_DOWN -> processor.touchDown(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_UP -> processor.touchUp(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_DRAGGED -> processor.touchDragged(it.x, it.y, it.pointer)
                InternalInputEventType.MOUSE_MOVED -> processor.mouseMoved(it.x, it.y)
                InternalInputEventType.SCROLLED -> processor.scrolled(it.x, it.y)
            }
        }
        processingQueue.clear()
    }

    fun keyDown(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_DOWN
                this.key = key
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun keyUp(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_UP
                this.key = key
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun keyTyped(character: Char, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_TYPED
                typedChar = character
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DOWN
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun touchUp(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_UP
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DRAGGED
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun mouseMoved(screenX: Float, screenY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.MOUSE_MOVED
                x = screenX
                y = screenY
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun scrolled(amountX: Float, amountY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.SCROLLED
                x = amountX
                y = amountY
                queueTime = time
            }
        }.also { queue.add(it) }
    }
}