package com.littlekt.graphics.particle

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.webgpu.BufferDescriptor
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer
import com.littlekt.math.MutableVec3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A small 3D shape drawn for each particle of a [ParticleEffect] with a [ParticleEffect.mesh], such
 * as a crystal shard, a rock or a drop. Sized so [ParticleEffect.size] is about its length.
 *
 * ```
 * val shards = ParticleEffect(mesh = ParticleMesh.shard(device), alignment = ParticleAlignment.VELOCITY, blend = ParticleBlend.OPAQUE)
 * ```
 *
 * @param positions the `x, y, z` of each vertex
 * @param normals the `x, y, z` normal of each vertex
 * @param uvs the `u, v` of each vertex, or `null` to map the texture across `x` & `y`
 * @param indices three vertex indices for each triangle, or `null` if every three vertices are a
 *   triangle
 * @author Colton Daily
 * @date 11/23/2026
 */
class ParticleMesh(
    val device: Device,
    positions: FloatArray,
    normals: FloatArray,
    uvs: FloatArray? = null,
    indices: IntArray? = null,
) : Releasable {

    /** The number of vertices drawn for each particle. */
    val vertexCount: Int = indices?.size ?: (positions.size / 3)

    internal val buffer: GPUBuffer

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    init {
        require(positions.size % 3 == 0 && positions.isNotEmpty()) { "positions must be x, y, z triples!" }
        require(normals.size == positions.size) { "normals must have one x, y, z for each position!" }
        require(uvs == null || uvs.size / 2 == positions.size / 3) { "uvs must have one u, v for each position!" }
        require(vertexCount % 3 == 0) { "ParticleMesh needs whole triangles!" }
        val data = FloatBuffer(vertexCount * VERTEX_FLOATS)
        for (i in 0 until vertexCount) {
            val v = indices?.get(i) ?: i
            val o = i * VERTEX_FLOATS
            data[o] = positions[v * 3]
            data[o + 1] = positions[v * 3 + 1]
            data[o + 2] = positions[v * 3 + 2]
            data[o + 3] = 1f
            data[o + 4] = normals[v * 3]
            data[o + 5] = normals[v * 3 + 1]
            data[o + 6] = normals[v * 3 + 2]
            data[o + 7] = 0f
            data[o + 8] = uvs?.get(v * 2) ?: (positions[v * 3] + 0.5f)
            data[o + 9] = uvs?.get(v * 2 + 1) ?: (0.5f - positions[v * 3 + 1])
            data[o + 10] = 0f
            data[o + 11] = 0f
        }
        buffer =
            device.createBuffer(
                BufferDescriptor(
                    "ParticleMesh vertices",
                    vertexCount * VERTEX_FLOATS * Float.SIZE_BYTES.toLong(),
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                    false,
                )
            )
        device.queue.writeBuffer(buffer, data)
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        buffer.release()
    }

    companion object {
        internal const val VERTEX_FLOATS = 12

        /**
         * A faceted crystal pointing along `y`, from `-0.5` to `0.5`, with [sides] faces around it.
         * [width] is its thickness at the widest point.
         */
        fun shard(device: Device, sides: Int = 5, width: Float = 0.3f): ParticleMesh {
            require(sides >= 3) { "a shard needs at least 3 sides!" }
            val ring = Array(sides) { i ->
                val angle = (i.toFloat() / sides) * 2f * PI.toFloat()
                floatArrayOf(cos(angle) * width * 0.5f, -0.15f, sin(angle) * width * 0.5f)
            }
            val top = floatArrayOf(0f, 0.5f, 0f)
            val bottom = floatArrayOf(0f, -0.5f, 0f)
            val builder = FlatBuilder(sides * 2)
            for (i in 0 until sides) {
                val a = ring[i]
                val b = ring[(i + 1) % sides]
                builder.triangle(top, b, a)
                builder.triangle(bottom, a, b)
            }
            return builder.build(device)
        }

        /** A cube from `-0.5` to `0.5`. */
        fun box(device: Device): ParticleMesh {
            val builder = FlatBuilder(12)
            val c = Array(8) { i -> floatArrayOf(if (i and 1 == 0) -0.5f else 0.5f, if (i and 2 == 0) -0.5f else 0.5f, if (i and 4 == 0) -0.5f else 0.5f) }
            fun quad(a: Int, b: Int, d: Int, e: Int) {
                builder.triangle(c[a], c[b], c[d])
                builder.triangle(c[a], c[d], c[e])
            }
            quad(0, 2, 3, 1)
            quad(4, 5, 7, 6)
            quad(0, 1, 5, 4)
            quad(2, 6, 7, 3)
            quad(0, 4, 6, 2)
            quad(1, 3, 7, 5)
            return builder.build(device)
        }

        /**
         * A lumpy faceted rock about `1` across, with its corners pushed in or out by up to
         * [roughness] of its radius, picked with [seed].
         */
        fun rock(device: Device, roughness: Float = 0.3f, seed: Int = 0): ParticleMesh {
            val random = Random(seed)
            val vertices = icosahedron()
            for (i in vertices.indices) {
                val scale = 0.5f * (1f + (random.nextFloat() * 2f - 1f) * roughness)
                vertices[i][0] *= scale
                vertices[i][1] *= scale
                vertices[i][2] *= scale
            }
            val builder = FlatBuilder(ICOSAHEDRON_FACES.size / 3)
            for (f in ICOSAHEDRON_FACES.indices step 3) {
                builder.triangle(vertices[ICOSAHEDRON_FACES[f]], vertices[ICOSAHEDRON_FACES[f + 1]], vertices[ICOSAHEDRON_FACES[f + 2]])
            }
            return builder.build(device)
        }

        /** A smooth ball `1` across, such as for drops & orbs. */
        fun sphere(device: Device): ParticleMesh {
            val base = icosahedron()
            val positions = ArrayList<FloatArray>()
            val faces = ArrayList<Int>()
            base.forEach { positions += it }
            val midpoints = HashMap<Long, Int>()
            fun midpoint(a: Int, b: Int): Int {
                val key = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                return midpoints.getOrPut(key) {
                    val pa = positions[a]
                    val pb = positions[b]
                    val m = floatArrayOf((pa[0] + pb[0]) * 0.5f, (pa[1] + pb[1]) * 0.5f, (pa[2] + pb[2]) * 0.5f)
                    normalize(m)
                    positions += m
                    positions.size - 1
                }
            }
            for (f in ICOSAHEDRON_FACES.indices step 3) {
                val a = ICOSAHEDRON_FACES[f]
                val b = ICOSAHEDRON_FACES[f + 1]
                val c = ICOSAHEDRON_FACES[f + 2]
                val ab = midpoint(a, b)
                val bc = midpoint(b, c)
                val ca = midpoint(c, a)
                faces += listOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca)
            }
            val out = FloatArray(positions.size * 3)
            val normals = FloatArray(positions.size * 3)
            positions.forEachIndexed { i, p ->
                normals[i * 3] = p[0]
                normals[i * 3 + 1] = p[1]
                normals[i * 3 + 2] = p[2]
                out[i * 3] = p[0] * 0.5f
                out[i * 3 + 1] = p[1] * 0.5f
                out[i * 3 + 2] = p[2] * 0.5f
            }
            return ParticleMesh(device, out, normals, null, faces.toIntArray())
        }

        private val ICOSAHEDRON_FACES =
            intArrayOf(
                0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
                1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
                3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
                4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
            )

        private fun icosahedron(): Array<FloatArray> {
            val t = (1f + sqrt(5f)) / 2f
            val vertices = arrayOf<FloatArray>(
                floatArrayOf(-1f, t, 0f), floatArrayOf(1f, t, 0f), floatArrayOf(-1f, -t, 0f), floatArrayOf(1f, -t, 0f),
                floatArrayOf(0f, -1f, t), floatArrayOf(0f, 1f, t), floatArrayOf(0f, -1f, -t), floatArrayOf(0f, 1f, -t),
                floatArrayOf(t, 0f, -1f), floatArrayOf(t, 0f, 1f), floatArrayOf(-t, 0f, -1f), floatArrayOf(-t, 0f, 1f),
            )
            for (v in vertices) normalize(v)
            return vertices
        }

        private fun normalize(v: FloatArray) {
            val length = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            if (length > 0f) {
                v[0] /= length
                v[1] /= length
                v[2] /= length
            }
        }
    }

    private class FlatBuilder(triangles: Int) {
        private val positions = FloatArray(triangles * 9)
        private val normals = FloatArray(triangles * 9)
        private var count = 0

        fun triangle(a: FloatArray, b: FloatArray, c: FloatArray) {
            val ux = b[0] - a[0]
            val uy = b[1] - a[1]
            val uz = b[2] - a[2]
            val vx = c[0] - a[0]
            val vy = c[1] - a[1]
            val vz = c[2] - a[2]
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val length = sqrt(nx * nx + ny * ny + nz * nz).let { if (it > 0f) it else 1f }
            nx /= length
            ny /= length
            nz /= length
            for (p in arrayOf(a, b, c)) {
                val o = count * 3
                positions[o] = p[0]
                positions[o + 1] = p[1]
                positions[o + 2] = p[2]
                normals[o] = nx
                normals[o + 1] = ny
                normals[o + 2] = nz
                count++
            }
        }

        fun build(device: Device): ParticleMesh = ParticleMesh(device, positions.copyOf(count * 3), normals.copyOf(count * 3))
    }
}

/**
 * The light [ParticleEffect.mesh] particles are shaded with: one directional light and an ambient
 * color. Set it to match the scene's main light.
 *
 * @author Colton Daily
 * @date 11/23/2026
 */
class ParticleLight {
    /** The direction the light shines in, in world space. */
    val direction: MutableVec3f = MutableVec3f(-0.4f, -1f, -0.3f)

    /** The light's color, brighter than `1` for HDR targets. */
    val color: MutableColor = MutableColor(Color.WHITE)

    /** The light that reaches every side. */
    val ambient: MutableColor = MutableColor(0.3f, 0.3f, 0.35f, 1f)
}
