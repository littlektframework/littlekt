package com.littlekt.graphics.g3d.skin

import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.math.Quaternion
import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AnimationTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun translationClip(name: String, node: Node3D, from: Vec3f, to: Vec3f, duration: Float = 1f): Animation {
        val anim = Animation(name)
        val channel = TranslationAnimationChannel(name, AnimatedTransformGroup(node))
        channel.keys.put(0f, TranslationKey(0f, from))
        channel.keys.put(duration, TranslationKey(duration, to))
        anim.channels += channel
        anim.prepareAnimation()
        return anim
    }

    private fun rotationClip(name: String, node: Node3D, rotation: Quaternion): Animation {
        val anim = Animation(name)
        val channel = RotationAnimationChannel(name, AnimatedTransformGroup(node))
        channel.keys.put(0f, RotationKey(0f, rotation))
        channel.keys.put(1f, RotationKey(1f, rotation))
        anim.channels += channel
        anim.prepareAnimation()
        return anim
    }

    private fun model(vararg clips: Animation) = Model().apply { animations += clips }

    @Test
    fun test_looping_wraps_progress() {
        val anim = translationClip("a", Node3D(), Vec3f(0f, 0f, 0f), Vec3f(10f, 0f, 0f))
        anim.advance(0.75.seconds)
        anim.advance(0.5.seconds)
        assertNear(0.25f, anim.progress)
        assertFalse(anim.isFinished)
    }

    @Test
    fun test_play_once_clamps_and_finishes_once() {
        val node = Node3D()
        val anim = translationClip("a", node, Vec3f(0f, 0f, 0f), Vec3f(10f, 0f, 0f))
        anim.loop = false
        var finished = 0
        anim.onFinished = { finished++ }
        anim.apply(0.75.seconds)
        anim.apply(0.75.seconds)
        anim.apply(0.75.seconds)
        assertNear(1f, anim.progress)
        assertTrue(anim.isFinished)
        assertEquals(1, finished)
        assertNear(10f, node.position.x)
    }

    @Test
    fun test_events_fire_once_when_crossed() {
        val anim = translationClip("a", Node3D(), Vec3f(0f, 0f, 0f), Vec3f(1f, 0f, 0f))
        anim.addEvent(0.5f, "mid")
        anim.addEvent(0f, "start")
        assertEquals(listOf("start", "mid"), anim.events.map { it.name })
        val fired = mutableListOf<String>()
        anim.onEvent = { fired += it.name }
        anim.advance(0.25.seconds)
        anim.advance(0.25.seconds)
        anim.advance(0.25.seconds)
        assertEquals(listOf("start", "mid"), fired)
        anim.advance(0.5.seconds)
        assertEquals(listOf("start", "mid", "start"), fired)
    }

    @Test
    fun test_events_fire_in_reverse() {
        val anim = translationClip("a", Node3D(), Vec3f(0f, 0f, 0f), Vec3f(1f, 0f, 0f))
        anim.addEvent(0.25f, "a")
        anim.addEvent(0.75f, "b")
        anim.speed = -1f
        anim.loop = false
        anim.reset()
        val fired = mutableListOf<String>()
        anim.onEvent = { fired += it.name }
        anim.advance(2.seconds)
        assertEquals(listOf("b", "a"), fired)
        assertNear(0f, anim.progress)
        assertTrue(anim.isFinished)
    }

    @Test
    fun test_crossfade_blends_and_settles() {
        val node = Node3D()
        val a = translationClip("a", node, Vec3f(0f, 0f, 0f), Vec3f(0f, 0f, 0f))
        val b = translationClip("b", node, Vec3f(10f, 0f, 0f), Vec3f(10f, 0f, 0f))
        val model = model(a, b)
        model.play("a", fade = Duration.ZERO)
        model.updateAnimations(16.milliseconds)
        assertNear(0f, node.position.x)

        model.play("b", fade = 1.seconds)
        assertSame(b, model.currentAnimation)
        model.updateAnimations(0.5.seconds)
        assertNear(0.5f, a.weight)
        assertNear(0.5f, b.weight)
        assertNear(5f, node.position.x)

        model.updateAnimations(0.5.seconds)
        assertNear(0f, a.weight)
        assertNear(1f, b.weight)
        assertNear(10f, node.position.x)
    }

    @Test
    fun test_blend_handles_opposite_quaternions() {
        val node = Node3D()
        val h = sqrt(0.5f)
        val a = rotationClip("a", node, Quaternion(0f, h, 0f, h))
        val b = rotationClip("b", node, Quaternion(0f, -h, 0f, -h))
        val model = model(a, b)
        model.setAnimationWeight(0, 0.5f)
        model.setAnimationWeight(1, 0.5f)
        model.updateAnimations(16.milliseconds)
        val r = node.rotation
        assertNear(1f, abs(r.dot(Quaternion(0f, h, 0f, h))))
    }

    @Test
    fun test_partial_weight_blends_with_rest_pose() {
        val node = Node3D()
        val a = translationClip("a", node, Vec3f(10f, 0f, 0f), Vec3f(10f, 0f, 0f))
        val model = model(a)
        model.setAnimationWeight(0, 0.25f)
        model.updateAnimations(16.milliseconds)
        assertNear(2.5f, node.position.x)
    }

    @Test
    fun test_fading_out_clip_does_not_fire_events() {
        val node = Node3D()
        val a = translationClip("a", node, Vec3f(0f, 0f, 0f), Vec3f(0f, 0f, 0f))
        val b = translationClip("b", node, Vec3f(0f, 0f, 0f), Vec3f(0f, 0f, 0f))
        a.addEvent(0.5f, "step")
        val fired = mutableListOf<String>()
        a.onEvent = { fired += it.name }
        val model = model(a, b)
        model.play(a, fade = Duration.ZERO)
        model.play(b, fade = 1.seconds)
        model.updateAnimations(0.75.seconds)
        assertTrue(fired.isEmpty())
    }

    @Test
    fun test_play_once_restarts_after_finishing() {
        val node = Node3D()
        val a = translationClip("a", node, Vec3f(0f, 0f, 0f), Vec3f(1f, 0f, 0f))
        val model = model(a)
        model.play(a, loop = false, fade = Duration.ZERO)
        model.updateAnimations(2.seconds)
        assertTrue(a.isFinished)
        model.play(a, loop = false, fade = Duration.ZERO)
        assertFalse(a.isFinished)
        assertNear(0f, a.progress)
    }
    @Test
    fun test_copy_animates_mapped_nodes_independently() {
        val node = Node3D()
        val copyNode = Node3D()
        val a = translationClip("a", node, Vec3f(0f, 0f, 0f), Vec3f(10f, 0f, 0f))
        a.addEvent(0.5f, "step")
        a.advance(0.25.seconds)
        val copy = a.copy({ if (it === node) copyNode else it })
        assertEquals("a", copy.name)
        assertNear(0.25f, copy.progress)
        assertEquals(1, copy.events.size)
        copy.apply(0.25.seconds)
        assertNear(5f, copyNode.position.x)
        assertNear(0f, node.position.x)
        a.sample(1f)
        assertNear(10f, node.position.x)
        assertNear(5f, copyNode.position.x)
    }

    @Test
    fun test_copy_keeps_rest_pose_of_source() {
        val node = Node3D().position(4f, 0f, 0f)
        val anim = translationClip("a", node, Vec3f(4f, 0f, 0f), Vec3f(4f, 0f, 0f))
        node.position(9f, 0f, 0f)
        val copyNode = Node3D().position(9f, 0f, 0f)
        val copy = anim.copy({ copyNode })
        val model = model(copy)
        model.setAnimationWeight(0, 0.5f)
        model.updateAnimations(16.milliseconds)
        assertNear(4f, copyNode.position.x)
    }

    @Test
    fun test_model_copy_plays_its_own_animations() {
        val model = Model()
        val bone = Node3D().apply { name = "bone" }
        model.addChild(bone)
        model.animations += translationClip("walk", bone, Vec3f(0f, 0f, 0f), Vec3f(10f, 0f, 0f))
        model.defaultFade = 50.milliseconds
        val copy = model.copy()
        val copyBone = copy.children[0]
        assertEquals("bone", copyBone.name)
        assertEquals(1, copy.animations.size)
        assertEquals(50.milliseconds, copy.defaultFade)
        copy.play("walk", fade = Duration.ZERO)
        copy.updateAnimations(0.5.seconds)
        assertNear(5f, copyBone.position.x)
        assertNear(0f, bone.position.x)
        assertSame(copy.animations[0], copy.currentAnimation)
        assertEquals(null, model.currentAnimation)
    }
}
