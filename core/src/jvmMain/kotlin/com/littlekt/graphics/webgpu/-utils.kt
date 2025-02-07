package com.littlekt.graphics.webgpu

import com.littlekt.wgpu.WGPU
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout

internal fun <T> Array<T>.mapToNativeEntries(
    scope: SegmentAllocator,
    nativeSize: Long,
    allocator: (Long, SegmentAllocator) -> MemorySegment,
    action: (entry: T, nativeEntry: MemorySegment) -> Unit,
): MemorySegment {
    val nativeArray = allocator(this.size.toLong(), scope)
    forEachIndexed { index, jvmEntry ->
        val nativeEntry = nativeArray.asSlice(nativeSize * index)

        action(jvmEntry, nativeEntry)
    }

    return nativeArray
}

internal fun <T> Collection<T>.mapToNativeEntries(
    scope: SegmentAllocator,
    nativeSize: Long,
    allocator: (Long, SegmentAllocator) -> MemorySegment,
    action: (entry: T, nativeEntry: MemorySegment) -> Unit,
): MemorySegment {
    val nativeArray = allocator(this.size.toLong(), scope)
    forEachIndexed { index, jvmEntry ->
        val nativeEntry = nativeArray.asSlice(nativeSize * index)

        action(jvmEntry, nativeEntry)
    }

    return nativeArray
}

internal val WGPU_NULL: MemorySegment = WGPU.NULL()

internal fun List<MemorySegment>.toNativeArray(scope: SegmentAllocator) =
    scope.allocateFrom(ValueLayout.JAVA_LONG, *map { it.address() }.toLongArray())

internal fun String.toNativeString(scope: SegmentAllocator) = scope.allocateFrom(this)

internal fun Boolean.toInt() = if (this) 1 else 0
