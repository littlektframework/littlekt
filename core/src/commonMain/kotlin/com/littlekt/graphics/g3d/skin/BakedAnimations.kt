package com.littlekt.graphics.g3d.skin

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.MeshPrimitiveInstance
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.webgpu.Device
import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec3f
import com.littlekt.math.spatial.BoundingBox
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Bakes every animation of a skinned [Model] into joint matrices on the GPU, so any number of
 * [BakedAnimationInstance] can play them with their own clip and time in a single draw call per
 * mesh primitive. Each instance only uploads which frames it shows, so the CPU cost doesn't grow
 * with the joint count.
 *
 * ```
 * val enemies = BakedAnimations(device, foxModel)
 * val enemy = enemies.createInstance().apply { play("Run") }
 * horde.addChild(enemy)
 * ```
 *
 * The mesh primitives share the meshes and materials of [model], which must stay loaded while
 * this is used. Release this when done to free the baked frames.
 *
 * @param model the skinned model to bake. Its current pose and transform are restored after.
 * @param fps the number of frames sampled per second of each animation
 * @author Colton Daily
 * @date 11/11/2026
 */
class BakedAnimations(val device: Device, val model: Model, val fps: Float = 30f) : Releasable {
    /** The baked animations, in the same order as [Model.animations]. */
    val clips: List<Clip>

    /** The model space bounds that cover every baked frame. */
    val bounds = BoundingBox()

    /** The total number of frames across all [clips]. */
    val totalFrames: Int

    internal val palette: JointPalette

    private val primitives: List<MeshPrimitive>
    private val jointBase: IntArray
    private val jointCount: IntArray

    init {
        require(fps > 0f) { "BakedAnimations.fps must be greater than 0!" }
        val sourceInstances = mutableListOf<MeshPrimitiveInstance>()
        collectSkinnedInstances(model, sourceInstances)
        val sources = sourceInstances.mapNotNull { it.instanceOf }.distinct()
        require(sources.isNotEmpty()) { "BakedAnimations requires a model with skinned meshes!" }
        val skins = sources.mapNotNull { it.skin }.distinct()

        val clipList = mutableListOf<Clip>()
        var frames = 0
        if (model.animations.isEmpty()) {
            clipList += Clip(null, 0, 0, 1, 0f, fps)
            frames = 1
        } else {
            model.animations.forEachIndexed { index, animation ->
                val count = ceil(animation.duration * fps).toInt() + 1
                clipList += Clip(animation.name, index, frames, count, animation.duration, fps)
                frames += count
            }
        }
        clips = clipList
        totalFrames = frames

        val skinBase = IntArray(skins.size)
        var matrices = 0
        skins.forEachIndexed { i, skin ->
            skinBase[i] = matrices
            matrices += skin.nodes.size * totalFrames
        }
        jointBase = IntArray(sources.size) { skinBase[skins.indexOf(sources[it].skin)] }
        jointCount = IntArray(sources.size) { sources[it].skin?.nodes?.size ?: 0 }

        val data = FloatBuffer(matrices * 16)
        bake(skins, skinBase, sourceInstances, data)
        palette = JointPalette(device, "Baked animations", data)

        primitives =
            sources.map { source ->
                MeshPrimitive(
                    source.mesh,
                    source.material,
                    source.topology,
                    source.stripIndexFormat,
                    source.skin,
                )
                    .also { it.bakedAnimations = this }
            }
    }

    /** @return the clip with the given [name] or `null` if there isn't one. */
    fun clip(name: String): Clip? = clips.firstOrNull { it.name == name }

    /** Creates a new instance that is drawn with these baked animations. */
    fun createInstance(): BakedAnimationInstance = BakedAnimationInstance(this)

    internal val primitiveCount: Int
        get() = primitives.size

    internal fun primitive(index: Int): MeshPrimitive = primitives[index]

    /** @return the index of the first joint matrix of [frame] for the primitive at [index]. */
    internal fun frameOffset(index: Int, frame: Int): Int =
        jointBase[index] + frame * jointCount[index]

    private fun bake(
        skins: List<Skin>,
        skinBase: IntArray,
        sourceInstances: List<MeshPrimitiveInstance>,
        data: FloatBuffer,
    ) {
        val nodes = mutableListOf<Node3D>()
        collectNodes(model, nodes)
        val rest = nodes.map { Mat4().set(it.transform) }
        val progress = model.animations.map { it.progress }
        val relative = Mat4()
        val point = MutableVec3f()

        bounds.batchUpdate {
            clear()
            val inverseModel = model.globalInverseTransform
            sourceInstances.forEach { instance ->
                val prim = instance.instanceOf ?: return@forEach
                relative.set(inverseModel).mul(instance.globalTransform)
                val meshBounds = prim.mesh.geometry.bounds
                if (meshBounds.isEmpty) return@forEach
                for (corner in 0 until 8) {
                    point.set(
                        if (corner and 1 == 0) meshBounds.min.x else meshBounds.max.x,
                        if (corner and 2 == 0) meshBounds.min.y else meshBounds.max.y,
                        if (corner and 4 == 0) meshBounds.min.z else meshBounds.max.z,
                    )
                    add(relative.transform(point))
                }
            }

            clips.forEach { clip ->
                for (frame in 0 until clip.frameCount) {
                    nodes.forEachIndexed { i, node -> node.transform = rest[i] }
                    model.animations.getOrNull(clip.index)?.sample(clip.frameTime(frame))
                    val inverse = model.globalInverseTransform
                    skins.forEachIndexed { s, skin ->
                        val first = skinBase[s] + (clip.firstFrame + frame) * skin.nodes.size
                        skin.nodes.forEachIndexed { j, node ->
                            relative.set(inverse).mul(node.joint.globalTransform)
                            add(relative.getTranslation(point))
                            relative.mul(node.inverseBindMatrix)
                            data.put(relative.data, dstOffset = (first + j) * 16)
                        }
                    }
                }
            }
            if (!isEmpty) {
                val padding = size.length() * 0.1f
                expand(point.set(padding, padding, padding))
            }
        }

        nodes.forEachIndexed { i, node -> node.transform = rest[i] }
        model.animations.forEachIndexed { i, animation -> animation.progress = progress[i] }
    }

    private fun collectSkinnedInstances(node: Node3D, out: MutableList<MeshPrimitiveInstance>) {
        if (node is MeshPrimitiveInstance && node.instanceOf?.skin != null) out += node
        node.children.forEach { collectSkinnedInstances(it, out) }
    }

    private fun collectNodes(node: Node3D, out: MutableList<Node3D>) {
        node.children.forEach {
            out += it
            collectNodes(it, out)
        }
    }

    override fun release() {
        primitives.forEach { it.release() }
        palette.release()
    }

    /**
     * A baked animation.
     *
     * @property name the name of the animation
     * @property index the index of the animation in [Model.animations]
     * @property firstFrame the first baked frame of this clip
     * @property frameCount the number of baked frames
     * @property duration the duration in seconds
     * @property fps the number of frames sampled per second
     */
    class Clip
    internal constructor(
        val name: String?,
        val index: Int,
        val firstFrame: Int,
        val frameCount: Int,
        val duration: Float,
        val fps: Float,
    ) {
        /** @return the animation time of the baked [frame], from `0` to [duration]. */
        fun frameTime(frame: Int): Float = minOf(frame / fps, duration)

        /** @return the baked frame at or before [time]. */
        fun frameAt(time: Float): Int {
            if (frameCount < 2) return 0
            return floor(time.coerceIn(0f, duration) * fps).toInt().coerceIn(0, frameCount - 2)
        }

        /** @return how far [time] is from [frameAt] to the next frame, from `0` to `1`. */
        fun blendAt(time: Float): Float {
            if (frameCount < 2) return 0f
            val frame = frameAt(time)
            val start = frameTime(frame)
            val end = frameTime(frame + 1)
            if (end <= start) return 0f
            return ((time.coerceIn(0f, duration) - start) / (end - start)).coerceIn(0f, 1f)
        }

        override fun toString(): String = "Clip($name, frames=$frameCount, duration=$duration)"
    }
}
