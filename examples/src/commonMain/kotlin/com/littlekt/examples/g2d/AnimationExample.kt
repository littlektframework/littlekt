package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.examples.colorAttachment
import com.littlekt.examples.configureSurfaceOnResize
import com.littlekt.examples.renderFrame
import com.littlekt.examples.surfaceColor
import com.littlekt.file.vfs.readAtlas
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Animation
import com.littlekt.graphics.g2d.AnimationPlayer
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.getAnimation
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.input.Key
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration.Companion.milliseconds

/**
 * An example of playing back [Animation]s from a [TextureAtlas] with an [AnimationPlayer] for a
 * lineup of characters. Press `1` to idle, `2` to run, `3` or `SPACE` to attack, and the arrow keys
 * to turn around.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class AnimationExample(context: Context) : ContextListener(context) {

    private class Character(
        val idle: Animation<TextureSlice>,
        val run: Animation<TextureSlice>,
        val attack: Animation<TextureSlice>,
    ) {
        val player = AnimationPlayer<TextureSlice>()
        var running = false
        val width = idle.firstFrame.width

        init {
            player.registerState(run, 5) { running }
            player.registerState(idle, 0)
        }
    }

    private fun TextureAtlas.character(idle: String, run: String, attack: String) =
        Character(
            idle = getAnimation(idle, 250.milliseconds),
            run = getAnimation(run),
            attack = getAnimation(attack, 70.milliseconds),
        )

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(240, 135)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.fromHex("2b2d42"))
        val gap = 16
        val characters =
            listOf(
                atlas.character("heroIdle", "heroRun", "heroBroomAttack1"),
                atlas.character("bossIdle", "bossRun", "bossAttack"),
                atlas.character("dustBunnyIdle", "dustBunnyJump", "dustBunnyAttack"),
                atlas.character("longArmIdle", "longArmWalk", "longArmSwing"),
                atlas.character("sheepIdle", "sheepWalk", "sheepAttack"),
            )
        val lineupWidth = characters.sumOf { it.width } + gap * (characters.size - 1)
        var facingLeft = false

        onResize { width, height -> viewport.update(width, height, true) }

        onUpdate { dt ->
            when {
                input.isKeyJustPressed(Key.NUM1) -> characters.fastForEach { it.running = false }
                input.isKeyJustPressed(Key.NUM2) -> characters.fastForEach { it.running = true }
                input.isKeyJustPressed(Key.NUM3) || input.isKeyJustPressed(Key.SPACE) ->
                    characters.fastForEach { it.player.play(it.attack) }
                input.isKeyJustPressed(Key.ARROW_LEFT) -> facingLeft = true
                input.isKeyJustPressed(Key.ARROW_RIGHT) -> facingLeft = false
            }
            characters.fastForEach { it.player.update(dt) }

            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, clearColor)))
                    )
                camera.update()
                batch.use(renderPass, camera.viewProjection) {
                    var x = (viewport.virtualWidth - lineupWidth) * 0.5f
                    val y = viewport.virtualHeight * 0.4f
                    characters.fastForEach { character ->
                        character.player.currentKeyFrame?.let { slice ->
                            val offset = (character.width - slice.width) * 0.5f
                            batch.draw(slice, x + offset, y, flipX = facingLeft)
                        }
                        x += character.width + gap
                    }
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            atlas.release()
        }
    }
}
