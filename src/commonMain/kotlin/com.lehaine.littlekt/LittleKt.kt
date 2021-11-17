package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.util.GlobalManager
import com.lehaine.littlekt.util.TimeSpan

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
open class LittleKt(val application: Application) {

    val fileHandler get() = application.fileHandler

    /**
     * The internally handled scene
     */
    private var _scene: Scene? = null

    /**
     * The currently active [Scene].
     * Note: if set, the [Scene] will not actually change until the end of the [render]
     */
    var scene: Scene?
        get() = _scene
        set(value) {
            check(value != null) { "Scene can not be set to null!" }
            if (_scene == null) {
                _scene = value
                _scene?.apply { root.apply { initialize() } }
                onSceneChanged()
                value.begin()
            } else {
                nextScene = value
            }
        }

    private var nextScene: Scene? = null

    private var globalManagers = mutableListOf<GlobalManager>()


    fun registerGlobalManager(manager: GlobalManager) {
        globalManagers.add(manager)
        manager.enabled = true
    }

    init {
        //    registerGlobalManager(RenderTarget())
    }

    open fun create() {
    }

    fun resize(width: Int, height: Int) {
        scene?.resize(width, height)
    }

    fun render(dt: TimeSpan, input: Input) {
        Time.update(dt)

        scene?.let { _scene ->
            globalManagers.forEach {
                it.update()
            }

            _scene.update(input)

            nextScene?.let { _nextScene ->
                _scene.end()
                scene = _nextScene
                nextScene = null
                onSceneChanged()
                _nextScene.begin()
            }
        }
        scene?.render()
    }

    /**
     * Called after a [Scene] ends, before the next [Scene] begins.
     */
    fun onSceneChanged() {
        Time.sceneChanged()
    }

    /**
     * Exit and destroy the current [LittleKt] process.
     */
    fun exit() {
        application.close()
    }
}