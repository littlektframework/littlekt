package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import com.lehaine.littlekt.log.Logger
import kotlin.reflect.KClass

/**
 * A [ContextListener] that handles commonly loading assets and preparing assets. Handles scene switching and caching.
 * @param context the context instance
 * @param firstScene the initial scene the was be used immediately by the context.
 * @param SceneType the common base interface or class of all scenes.
 * @see Scene
 * @author Colt Daily
 * @date 12/23/21
 */
open class Game<SceneType : Scene>(context: Context, firstScene: SceneType? = null) : ContextListener(context) {

    val graphics get() = context.graphics
    val input get() = context.input
    val stats get() = context.stats
    val gl get() = context.gl
    val logger get() = context.logger
    val resourcesVfs get() = context.resourcesVfs
    val storageVfs get() = context.storageVfs

    /**
     * Holds reference to all scenes registers with [addScene]. Allows to get a reference of the scene instance
     * knowing only its type.
     */
    protected val scenes: MutableMap<KClass<out SceneType>, SceneType> = mutableMapOf()

    /**
     * The currently shown scene. Unless overridden with [setScene], uses an empty mock-up implementation
     * to work around nullability and `lateinit` issues. [shownScene] is a public property exposing this
     * value as a [SceneType]
     * @see shownScene
     */
    protected var currentScene = firstScene ?: emptyScene(context)

    /**
     * Provides direct access to current [Scene] instance handling game events.
     */
    open val shownScene: SceneType
        @Suppress("UNCHECKED_CAST")
        get() = currentScene as SceneType

    private var created = false

    /**
     * Adds callbacks for resizing the [currentScene] and for disposing [scenes].
     */
    protected fun setSceneCallbacks(context: Context) {
        context.onResize { width, height ->
            currentScene.resize(width, height)
            println("resize")
        }

        context.onDispose {
            scenes.values.forEach {
                try {
                    it.dispose()
                } catch (exception: Throwable) {
                    onSceneDisposalError(it, exception)
                }
            }
        }
    }

    /**
     * Handles resizing the current scene and disposing of the current scene.
     */
    override suspend fun Context.start() {
        setSceneCallbacks(this)
    }


    /**
     * Registers an instance of [Scene].
     * @param Type concrete class of the [Scene] instance. The implementation assumes that scenes are singletons and
     * only one implementation of each class will be registered.
     * @param scene instance of [Type]. After invocation of this method, [setScene] can be used with the appropriate
     * class to change current screen to this object.
     * @throws IllegalStateException if a scene of selected type is already registered. Use [removeScene] first to replace
     * the screen.
     * @see getScene
     * @see setScene
     * @see removeScene
     */
    suspend inline fun <reified Type : SceneType> addScene(scene: Type) = addScene(Type::class, scene)

    /**
     * Registers an instance of [Scene].
     * @param Type concrete class of the [Scene] instance. The implementation assumes that scenes are singletons and
     * only one implementation of each class will be registered.
     * @param scene instance of [Type]. After invocation of this method, [setScene] can be used with the appropriate
     * class to change current screen to this object.
     * @throws IllegalStateException if a scene of selected type is already registered. Use [removeScene] first to replace
     * the screen.
     * @see getScene
     * @see setScene
     * @see removeScene
     */
    open suspend fun <Type : SceneType> addScene(type: KClass<Type>, scene: Type) {
        !scenes.containsKey(type) || error("Scene already registered to type: $type")
        scenes[type] = scene
    }


    /**
     * Replaces the current scene with the registered scene instance of the passed type.
     * @param Type concrete class of the [Scene] instance. The scene instance of this type must have been added
     * with [addScene] before calling this method.
     * @see addScene
     * @see shownScene
     */
    suspend inline fun <reified Type : SceneType> setScene() = setScene(Type::class)

    /**
     * Replaces the current scene with the registered scene instance of the passed type.
     * @param Type concrete class of the [Scene] instance. The scene instance of this type must have been added
     * with [addScene] before calling this method.
     * @see addScene
     * @see shownScene
     */
    open suspend fun <Type : SceneType> setScene(type: KClass<Type>) {
        currentScene.hide()
        currentScene = getScene(type)
        currentScene.resize(context.graphics.width, context.graphics.height)
        currentScene.show()
    }

    /**
     * Returns the cached instance of [Scene] of the selected type.
     * @param Type concrete class of the [Scene] instance. The scene instance of this type must have been added
     * with [addScene] before calling this method.
     * @return the instance of [Scene] extending passed [Type].
     * @throws IllegalStateException if instance of the selected [Type] is not registed with [addScene]
     * @see addScene
     */
    inline fun <reified Type : SceneType> getScene(): Type = getScene(Type::class)

    /**
     * Returns the cached instance of [Scene] of the selected type.
     * @param Type concrete class of the [Scene] instance. The scene instance of this type must have been added
     * with [addScene] before calling this method.
     * @return the instance of [Scene] extending passed [Type].
     * @throws IllegalStateException if instance of the selected [Type] is not registed with [addScene]
     * @see addScene
     */
    @Suppress("UNCHECKED_CAST")
    open fun <Type : SceneType> getScene(type: KClass<Type>): Type =
        scenes[type] as Type? ?: error("Missing scene instance of type: $type.")


    /**
     * Removes the cached instance of [Scene] of the selected type. Note that this method does not dispose
     * of the scene and will not affect [shownScene].
     * @param Type concreate class of the [Scene] instance.
     * @return removed instance of [Scene] extending passed [Type] if it was registered; otherwise `null`.
     * @see addScene
     */
    inline fun <reified Type : SceneType> removeScene(): Type? = removeScene(Type::class)

    /**
     * Removes the cached instance of [Scene] of the selected type. Note that this method does not dispose
     * of the scene and will not affect [shownScene].
     * @param Type concreate class of the [Scene] instance.
     * @return removed instance of [Scene] extending passed [Type] if it was registered; otherwise `null`.
     * @see addScene
     */
    @Suppress("UNCHECKED_CAST")
    open fun <Type : SceneType> removeScene(type: KClass<Type>): Type? = scenes.remove(type) as Type?


    /**
     * Checks if the scene of the given type is registered.
     * @param Type concreate class of scene implementation.
     * @return true if a [Scene] is registered with selected type; otherwise false.
     */
    inline fun <reified Type : SceneType> containsScene(): Boolean = containsScene(Type::class)

    /**
     * Checks if the scene of the given type is registered.
     * @param Type concreate class of scene implementation.
     * @return true if a [Scene] is registered with selected type; otherwise false.
     */
    open fun <Type : SceneType> containsScene(type: KClass<Type>): Boolean = scenes.containsKey(type)

    /**
     * Invoked on scenes disposal on [dispose] if an error occurs.
     * @param scene thrown [exception] during disposal
     * @param exception unexpected scene dispose exception
     */
    protected open fun onSceneDisposalError(scene: SceneType, exception: Throwable) {
        logger.error { "Unable to dispose of ${scene::class.simpleName} scene. $exception" }
    }

    private fun emptyScene(context: Context): Scene = object : Scene(context) {}

    companion object {
        protected val logger = Logger<Game<*>>()
    }
}

/**
 * Creates a new [ShaderProgram] for the specified shaders.
 * @param vertexShader the vertex shader to use. Defaults to [DefaultVertexShader].
 * @param fragmentShader the fragment shader to use. Defaults to [DefaultFragmentShader].
 */
fun <T : ContextListener> T.createShader(
    vertexShader: VertexShader = DefaultVertexShader(),
    fragmentShader: FragmentShader = DefaultFragmentShader()
) =
    ShaderProgram(vertexShader, fragmentShader).also { it.prepare(context) }