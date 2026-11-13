package com.littlekt.math.spatial

import com.littlekt.math.Ray
import com.littlekt.util.datastructure.IntMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A uniform grid that finds items near a point, inside an area, or along a [Ray] without checking
 * every item. Each item is stored with its axis-aligned bounds in every cell they overlap.
 *
 * Works in 2D and 3D. For 2D, use the `Rect` & `Circle` functions, which keep `z` at `0`. For a 3D
 * game on the ground, a large [cellSizeY] keeps every item in one layer.
 *
 * Pick a cell size around the size of the most common item, or the most common query. Items much
 * larger than a cell are stored in many cells, and queries much larger than a cell visit many.
 *
 * Queries fill a list passed in and don't allocate. Items are compared by equality, so use items
 * whose `equals` & `hashCode` don't change while stored.
 *
 * @param cellSize the width & depth of a cell
 * @param cellSizeY the height of a cell
 * @author Colton Daily
 * @date 11/12/2026
 */
class SpatialHash<T : Any>(val cellSize: Float, val cellSizeY: Float = cellSize) {
    private val entries = HashMap<T, Entry<T>>()
    private val cells = IntMap<ArrayList<Entry<T>>>()
    private val bucketPool = ArrayList<ArrayList<Entry<T>>>()
    private val entryPool = ArrayList<Entry<T>>()
    private val invCell = 1f / cellSize
    private val invCellY = 1f / cellSizeY
    private var stamp = 0
    private var minCellX = Int.MAX_VALUE
    private var minCellY = Int.MAX_VALUE
    private var minCellZ = Int.MAX_VALUE
    private var maxCellX = Int.MIN_VALUE
    private var maxCellY = Int.MIN_VALUE
    private var maxCellZ = Int.MIN_VALUE

    init {
        require(cellSize > 0f && cellSizeY > 0f) { "cellSize must be greater than 0!" }
    }

    /** The number of items. */
    val size: Int
        get() = entries.size

    /** The distance to the item found by the last [raycast], or [Ray.NO_HIT]. */
    var lastHitDistance: Float = Ray.NO_HIT
        private set

    /** @return `true` if [item] is stored. */
    operator fun contains(item: T): Boolean = entries.containsKey(item)

    /** Adds [item] with the given bounds, or moves it if it is already stored. */
    fun put(item: T, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) {
        val x0 = cellX(minX)
        val y0 = cellY(minY)
        val z0 = cellX(minZ)
        val x1 = cellX(maxX)
        val y1 = cellY(maxY)
        val z1 = cellX(maxZ)
        var entry = entries[item]
        if (entry == null) {
            entry = obtainEntry(item)
            entries[item] = entry
        } else if (
            entry.x0 != x0 ||
                entry.y0 != y0 ||
                entry.z0 != z0 ||
                entry.x1 != x1 ||
                entry.y1 != y1 ||
                entry.z1 != z1
        ) {
            removeFromCells(entry)
        } else {
            entry.setBounds(minX, minY, minZ, maxX, maxY, maxZ)
            return
        }
        entry.setBounds(minX, minY, minZ, maxX, maxY, maxZ)
        entry.x0 = x0
        entry.y0 = y0
        entry.z0 = z0
        entry.x1 = x1
        entry.y1 = y1
        entry.z1 = z1
        addToCells(entry)
    }

    /** Adds [item] with [bounds], or moves it if it is already stored. */
    fun put(item: T, bounds: BoundingBox) {
        put(item, bounds.min.x, bounds.min.y, bounds.min.z, bounds.max.x, bounds.max.y, bounds.max.z)
    }

    /** Adds [item] at a point, or moves it if it is already stored. */
    fun putPoint(item: T, x: Float, y: Float, z: Float = 0f) {
        put(item, x, y, z, x, y, z)
    }

    /** Adds [item] with 2D bounds at `z = 0`, or moves it if it is already stored. */
    fun putRect(item: T, x: Float, y: Float, width: Float, height: Float) {
        put(item, x, y, 0f, x + width, y + height, 0f)
    }

    /** Removes [item]. @return `true` if it was stored. */
    fun remove(item: T): Boolean {
        val entry = entries.remove(item) ?: return false
        removeFromCells(entry)
        freeEntry(entry)
        return true
    }

    /** Removes every item. */
    fun clear() {
        cells.forEachValue {
            it.clear()
            bucketPool += it
        }
        cells.clear()
        for (entry in entries.values) freeEntry(entry)
        entries.clear()
        minCellX = Int.MAX_VALUE
        minCellY = Int.MAX_VALUE
        minCellZ = Int.MAX_VALUE
        maxCellX = Int.MIN_VALUE
        maxCellY = Int.MIN_VALUE
        maxCellZ = Int.MIN_VALUE
    }

    /** Adds each item whose bounds overlap the given box to [result]. @return [result] */
    fun query(
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        result: MutableList<T>,
    ): MutableList<T> {
        val current = nextStamp()
        val x0 = max(cellX(minX), minCellX)
        val y0 = max(cellY(minY), minCellY)
        val z0 = max(cellX(minZ), minCellZ)
        val x1 = min(cellX(maxX), maxCellX)
        val y1 = min(cellY(maxY), maxCellY)
        val z1 = min(cellX(maxZ), maxCellZ)
        for (cx in x0..x1) {
            for (cy in y0..y1) {
                for (cz in z0..z1) {
                    val bucket = cells[key(cx, cy, cz)] ?: continue
                    for (i in 0 until bucket.size) {
                        val entry = bucket[i]
                        if (entry.stamp == current) continue
                        entry.stamp = current
                        if (entry.overlaps(minX, minY, minZ, maxX, maxY, maxZ)) result += entry.item
                    }
                }
            }
        }
        return result
    }

    /** Adds each item whose bounds overlap [bounds] to [result]. @return [result] */
    fun query(bounds: BoundingBox, result: MutableList<T>): MutableList<T> =
        query(
            bounds.min.x,
            bounds.min.y,
            bounds.min.z,
            bounds.max.x,
            bounds.max.y,
            bounds.max.z,
            result,
        )

    /** Adds each item whose bounds overlap the 2D rectangle at `z = 0` to [result]. */
    fun queryRect(x: Float, y: Float, width: Float, height: Float, result: MutableList<T>): MutableList<T> =
        query(x, y, 0f, x + width, y + height, 0f, result)

    /** Adds each item whose bounds overlap the sphere to [result]. @return [result] */
    fun queryRadius(x: Float, y: Float, z: Float, radius: Float, result: MutableList<T>): MutableList<T> {
        val current = nextStamp()
        val r2 = radius * radius
        val x0 = max(cellX(x - radius), minCellX)
        val y0 = max(cellY(y - radius), minCellY)
        val z0 = max(cellX(z - radius), minCellZ)
        val x1 = min(cellX(x + radius), maxCellX)
        val y1 = min(cellY(y + radius), maxCellY)
        val z1 = min(cellX(z + radius), maxCellZ)
        for (cx in x0..x1) {
            for (cy in y0..y1) {
                for (cz in z0..z1) {
                    val bucket = cells[key(cx, cy, cz)] ?: continue
                    for (i in 0 until bucket.size) {
                        val entry = bucket[i]
                        if (entry.stamp == current) continue
                        entry.stamp = current
                        if (entry.distanceSqr(x, y, z) <= r2) result += entry.item
                    }
                }
            }
        }
        return result
    }

    /** Adds each item whose bounds overlap the 2D circle at `z = 0` to [result]. */
    fun queryCircle(x: Float, y: Float, radius: Float, result: MutableList<T>): MutableList<T> =
        queryRadius(x, y, 0f, radius, result)

    /**
     * Adds each item whose bounds the ray hits within [maxDistance] to [result], in no particular
     * order. @return [result]
     */
    fun queryRay(ray: Ray, maxDistance: Float, result: MutableList<T>): MutableList<T> {
        walkRay(ray, maxDistance) { entry, _ ->
            result += entry.item
            maxDistance
        }
        return result
    }

    /**
     * Finds the closest item along [ray] within [maxDistance]. Cells are visited from near to far
     * and the walk stops once no closer item can be found. The distance is stored in
     * [lastHitDistance].
     *
     * @param test gets each item whose bounds the ray hits, with the distance to its bounds, and
     *   returns the exact distance to the item, or [Ray.NO_HIT] to skip it. `null` uses the bounds.
     * @return the closest item, or `null`
     */
    fun raycast(ray: Ray, maxDistance: Float = Float.MAX_VALUE, test: RayTest<T>? = null): T? {
        var best = maxDistance
        var bestItem: T? = null
        walkRay(ray, maxDistance) { entry, boxDistance ->
            if (boxDistance < best) {
                val distance = test?.distance(entry.item, ray, boxDistance) ?: boxDistance
                if (distance >= 0f && distance <= best) {
                    best = distance
                    bestItem = entry.item
                }
            }
            best
        }
        lastHitDistance = if (bestItem != null) best else Ray.NO_HIT
        return bestItem
    }

    /** Invokes [action] on each item. */
    inline fun forEach(action: (T) -> Unit) {
        for (item in items) action(item)
    }

    @PublishedApi
    internal val items: Set<T>
        get() = entries.keys

    /** Visits entries hit by [ray] near to far by cell. [visit] returns the distance to stop past. */
    private inline fun walkRay(ray: Ray, maxDistance: Float, visit: (Entry<T>, Float) -> Float) {
        if (entries.isEmpty()) return
        val current = nextStamp()
        val worldMinX = minCellX * cellSize
        val worldMinY = minCellY * cellSizeY
        val worldMinZ = minCellZ * cellSize
        val worldMaxX = (maxCellX + 1) * cellSize
        val worldMaxY = (maxCellY + 1) * cellSizeY
        val worldMaxZ = (maxCellZ + 1) * cellSize
        val start =
            ray.intersectBox(worldMinX, worldMinY, worldMinZ, worldMaxX, worldMaxY, worldMaxZ)
        if (start < 0f || start > maxDistance) return
        val ox = ray.origin.x
        val oy = ray.origin.y
        val oz = ray.origin.z
        val dx = ray.direction.x
        val dy = ray.direction.y
        val dz = ray.direction.z
        var cx = cellX(ox + dx * start).coerceIn(minCellX, maxCellX)
        var cy = cellY(oy + dy * start).coerceIn(minCellY, maxCellY)
        var cz = cellX(oz + dz * start).coerceIn(minCellZ, maxCellZ)
        val stepX = if (dx > 0f) 1 else -1
        val stepY = if (dy > 0f) 1 else -1
        val stepZ = if (dz > 0f) 1 else -1
        val deltaX = if (abs(dx) < EPSILON) Float.MAX_VALUE else cellSize / abs(dx)
        val deltaY = if (abs(dy) < EPSILON) Float.MAX_VALUE else cellSizeY / abs(dy)
        val deltaZ = if (abs(dz) < EPSILON) Float.MAX_VALUE else cellSize / abs(dz)
        var nextX = boundary(ox, dx, cx, stepX, cellSize)
        var nextY = boundary(oy, dy, cy, stepY, cellSizeY)
        var nextZ = boundary(oz, dz, cz, stepZ, cellSize)
        var limit = maxDistance
        while (true) {
            val bucket = cells[key(cx, cy, cz)]
            if (bucket != null) {
                for (i in 0 until bucket.size) {
                    val entry = bucket[i]
                    if (entry.stamp == current) continue
                    entry.stamp = current
                    val distance =
                        ray.intersectBox(
                            entry.minX,
                            entry.minY,
                            entry.minZ,
                            entry.maxX,
                            entry.maxY,
                            entry.maxZ,
                        )
                    if (distance < 0f || distance > maxDistance) continue
                    limit = min(limit, visit(entry, distance))
                }
            }
            if (min(nextX, min(nextY, nextZ)) > limit) return
            if (nextX <= nextY && nextX <= nextZ) {
                cx += stepX
                if (cx < minCellX || cx > maxCellX) return
                nextX += deltaX
            } else if (nextY <= nextZ) {
                cy += stepY
                if (cy < minCellY || cy > maxCellY) return
                nextY += deltaY
            } else {
                cz += stepZ
                if (cz < minCellZ || cz > maxCellZ) return
                nextZ += deltaZ
            }
        }
    }

    private fun boundary(origin: Float, direction: Float, cell: Int, step: Int, size: Float): Float {
        if (abs(direction) < EPSILON) return Float.MAX_VALUE
        val edge = (if (step > 0) cell + 1 else cell) * size
        return (edge - origin) / direction
    }

    private fun addToCells(entry: Entry<T>) {
        minCellX = min(minCellX, entry.x0)
        minCellY = min(minCellY, entry.y0)
        minCellZ = min(minCellZ, entry.z0)
        maxCellX = max(maxCellX, entry.x1)
        maxCellY = max(maxCellY, entry.y1)
        maxCellZ = max(maxCellZ, entry.z1)
        for (cx in entry.x0..entry.x1) {
            for (cy in entry.y0..entry.y1) {
                for (cz in entry.z0..entry.z1) {
                    val key = key(cx, cy, cz)
                    val bucket = cells[key] ?: obtainBucket().also { cells[key] = it }
                    bucket += entry
                }
            }
        }
    }

    private fun removeFromCells(entry: Entry<T>) {
        for (cx in entry.x0..entry.x1) {
            for (cy in entry.y0..entry.y1) {
                for (cz in entry.z0..entry.z1) {
                    val key = key(cx, cy, cz)
                    val bucket = cells[key] ?: continue
                    val index = bucket.indexOf(entry)
                    if (index < 0) continue
                    val last = bucket.lastIndex
                    bucket[index] = bucket[last]
                    bucket.removeAt(last)
                    if (bucket.isEmpty()) {
                        cells.remove(key)
                        bucketPool += bucket
                    }
                }
            }
        }
    }

    private fun obtainBucket(): ArrayList<Entry<T>> =
        if (bucketPool.isEmpty()) ArrayList() else bucketPool.removeAt(bucketPool.lastIndex)

    private fun obtainEntry(item: T): Entry<T> {
        val entry = if (entryPool.isEmpty()) Entry(item) else entryPool.removeAt(entryPool.lastIndex)
        entry.item = item
        entry.stamp = 0
        return entry
    }

    @Suppress("UNCHECKED_CAST")
    private fun freeEntry(entry: Entry<T>) {
        entry.item = PLACEHOLDER as T
        entryPool += entry
    }

    private fun nextStamp(): Int {
        stamp++
        if (stamp == 0) {
            for (entry in entries.values) entry.stamp = 0
            stamp = 1
        }
        return stamp
    }

    private fun cellX(value: Float): Int = floor(value * invCell).toInt()

    private fun cellY(value: Float): Int = floor(value * invCellY).toInt()

    private fun key(x: Int, y: Int, z: Int): Int = (x * 73856093) xor (y * 19349663) xor (z * 83492791)

    /** Gets the exact distance along a ray to an item. */
    fun interface RayTest<T> {
        /**
         * @param boxDistance the distance along [ray] to the item's bounds
         * @return the distance along [ray] to [item], or [Ray.NO_HIT] if it misses
         */
        fun distance(item: T, ray: Ray, boxDistance: Float): Float
    }

    private class Entry<T : Any>(var item: T) {
        var minX = 0f
        var minY = 0f
        var minZ = 0f
        var maxX = 0f
        var maxY = 0f
        var maxZ = 0f
        var x0 = 0
        var y0 = 0
        var z0 = 0
        var x1 = 0
        var y1 = 0
        var z1 = 0
        var stamp = 0

        fun setBounds(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) {
            this.minX = min(minX, maxX)
            this.minY = min(minY, maxY)
            this.minZ = min(minZ, maxZ)
            this.maxX = max(minX, maxX)
            this.maxY = max(minY, maxY)
            this.maxZ = max(minZ, maxZ)
        }

        fun overlaps(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) =
            this.minX <= maxX &&
                this.maxX >= minX &&
                this.minY <= maxY &&
                this.maxY >= minY &&
                this.minZ <= maxZ &&
                this.maxZ >= minZ

        fun distanceSqr(x: Float, y: Float, z: Float): Float {
            val dx = max(0f, max(minX - x, x - maxX))
            val dy = max(0f, max(minY - y, y - maxY))
            val dz = max(0f, max(minZ - z, z - maxZ))
            return dx * dx + dy * dy + dz * dz
        }
    }

    private companion object {
        const val EPSILON = 1e-8f
        val PLACEHOLDER = Any()
    }
}
