package com.lehaine.littlekt

import com.lehaine.littlekt.file.vfs.VfsFile
import kotlin.reflect.KClass
import kotlin.time.Duration

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
    protected val assetProvider = AssetProvider(context)

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
     * You must override [update]! Render handles asset loading logic for dealing with [AssetProvider].
     * [update] is called once all assets are fully loaded. Note that [currentScene] is updated here. You do not need to call [Scene.update].
     * @see [update]
     * @see [AssetProvider.fullyLoaded]
     */
    final override fun render(dt: Duration) {
        assetProvider.update()
        if (!assetProvider.fullyLoaded) return
        if (!created) {
            created = true
            create()
        }
        update(dt)
        currentScene.update()
    }

    /**
     * Invoked exactly once after all assets have been created and prepared. Do any initialization
     * of OpenGL related items here.
     */
    open fun create() = Unit

    /**
     * Continuously invoked once all assets have been loading for this [ContextListener].
     * Override this to handle any rendering logic.
     */
    open fun update(dt: Duration) = Unit

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
    inline fun <reified Type : SceneType> addScene(scene: Type) = addScene(Type::class, scene)

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
    open fun <Type : SceneType> addScene(type: KClass<Type>, scene: Type) {
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
    inline fun <reified Type : SceneType> setScene() = setScene(Type::class)

    /**
     * Replaces the current scene with the registered scene instance of the passed type.
     * @param Type concrete class of the [Scene] instance. The scene instance of this type must have been added
     * with [addScene] before calling this method.
     * @see addScene
     * @see shownScene
     */
    open fun <Type : SceneType> setScene(type: KClass<Type>) {
        currentScene.hide()
        currentScene = getScene(type)
        currentScene.resize(graphics.width, graphics.height)
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
     * Loads an asset asynchronously.
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see FontAssetParameter
     * @see LDtkGameAssetParameter
     */
    inline fun <reified T : Any> load(
        file: VfsFile,
        parameters: GameAssetParameters = GameAssetParameters()
    ) = `access$assetProvider`.load<T>(file, parameters)


    /**
     * Prepares a value once assets have finished loading. This acts the same as [lazy] except this will
     * invoke the [action] once loading is finished to ensure everything is initialized before the first frame.
     * @param action the action to initialize this value
     * @see load
     */
    fun <T : Any> prepare(action: () -> T) = assetProvider.prepare(action)


    override fun resize(width: Int, height: Int) {
        currentScene.resize(width, height)
    }

    override fun resume() {
        currentScene.resume()
    }

    override fun pause() {
        currentScene.pause()
    }

    /**
     * Disposes of all registered scenes with [Scene.dispose]. Catches thrown error and logs them with
     * the [Context.logger] instance. Override [onSceneDisposalError] to change error handling behavior.
     */
    override fun dispose() {
        scenes.values.forEach {
            try {
                it.dispose()
            } catch (exception: Throwable) {
                onSceneDisposalError(it, exception)
            }
        }
    }

    /**
     * Invoked on scenes disposal on [dispose] if an error occurs.
     * @param scene thrown [exception] during disposal
     * @param exception unexpected scene dispose exception
     */
    protected open fun onSceneDisposalError(scene: SceneType, exception: Throwable) {
        logger.error { "Unable to dispose of ${scene::class.simpleName} scene. $exception" }
    }

    private fun emptyScene(context: Context): Scene = object : Scene(context) {}

    @PublishedApi
    internal val `access$assetProvider`: AssetProvider
        get() = assetProvider
}
