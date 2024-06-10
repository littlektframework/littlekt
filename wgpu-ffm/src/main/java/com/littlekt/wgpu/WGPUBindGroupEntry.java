// Generated by jextract

package com.littlekt.wgpu;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct WGPUBindGroupEntry {
 *     const WGPUChainedStruct *nextInChain;
 *     uint32_t binding;
 *     WGPUBuffer buffer;
 *     uint64_t offset;
 *     uint64_t size;
 *     WGPUSampler sampler;
 *     WGPUTextureView textureView;
 * }
 * }
 */
public class WGPUBindGroupEntry {

    WGPUBindGroupEntry() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        WGPU.C_POINTER.withName("nextInChain"),
        WGPU.C_INT.withName("binding"),
        MemoryLayout.paddingLayout(4),
        WGPU.C_POINTER.withName("buffer"),
        WGPU.C_LONG_LONG.withName("offset"),
        WGPU.C_LONG_LONG.withName("size"),
        WGPU.C_POINTER.withName("sampler"),
        WGPU.C_POINTER.withName("textureView")
    ).withName("WGPUBindGroupEntry");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final AddressLayout nextInChain$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("nextInChain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static final AddressLayout nextInChain$layout() {
        return nextInChain$LAYOUT;
    }

    private static final long nextInChain$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static final long nextInChain$offset() {
        return nextInChain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static MemorySegment nextInChain(MemorySegment struct) {
        return struct.get(nextInChain$LAYOUT, nextInChain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static void nextInChain(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(nextInChain$LAYOUT, nextInChain$OFFSET, fieldValue);
    }

    private static final OfInt binding$LAYOUT = (OfInt)$LAYOUT.select(groupElement("binding"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint32_t binding
     * }
     */
    public static final OfInt binding$layout() {
        return binding$LAYOUT;
    }

    private static final long binding$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint32_t binding
     * }
     */
    public static final long binding$offset() {
        return binding$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint32_t binding
     * }
     */
    public static int binding(MemorySegment struct) {
        return struct.get(binding$LAYOUT, binding$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint32_t binding
     * }
     */
    public static void binding(MemorySegment struct, int fieldValue) {
        struct.set(binding$LAYOUT, binding$OFFSET, fieldValue);
    }

    private static final AddressLayout buffer$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("buffer"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUBuffer buffer
     * }
     */
    public static final AddressLayout buffer$layout() {
        return buffer$LAYOUT;
    }

    private static final long buffer$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUBuffer buffer
     * }
     */
    public static final long buffer$offset() {
        return buffer$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUBuffer buffer
     * }
     */
    public static MemorySegment buffer(MemorySegment struct) {
        return struct.get(buffer$LAYOUT, buffer$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUBuffer buffer
     * }
     */
    public static void buffer(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(buffer$LAYOUT, buffer$OFFSET, fieldValue);
    }

    private static final OfLong offset$LAYOUT = (OfLong)$LAYOUT.select(groupElement("offset"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint64_t offset
     * }
     */
    public static final OfLong offset$layout() {
        return offset$LAYOUT;
    }

    private static final long offset$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint64_t offset
     * }
     */
    public static final long offset$offset() {
        return offset$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint64_t offset
     * }
     */
    public static long offset(MemorySegment struct) {
        return struct.get(offset$LAYOUT, offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint64_t offset
     * }
     */
    public static void offset(MemorySegment struct, long fieldValue) {
        struct.set(offset$LAYOUT, offset$OFFSET, fieldValue);
    }

    private static final OfLong size$LAYOUT = (OfLong)$LAYOUT.select(groupElement("size"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint64_t size
     * }
     */
    public static final OfLong size$layout() {
        return size$LAYOUT;
    }

    private static final long size$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint64_t size
     * }
     */
    public static final long size$offset() {
        return size$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint64_t size
     * }
     */
    public static long size(MemorySegment struct) {
        return struct.get(size$LAYOUT, size$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint64_t size
     * }
     */
    public static void size(MemorySegment struct, long fieldValue) {
        struct.set(size$LAYOUT, size$OFFSET, fieldValue);
    }

    private static final AddressLayout sampler$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("sampler"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUSampler sampler
     * }
     */
    public static final AddressLayout sampler$layout() {
        return sampler$LAYOUT;
    }

    private static final long sampler$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUSampler sampler
     * }
     */
    public static final long sampler$offset() {
        return sampler$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUSampler sampler
     * }
     */
    public static MemorySegment sampler(MemorySegment struct) {
        return struct.get(sampler$LAYOUT, sampler$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUSampler sampler
     * }
     */
    public static void sampler(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(sampler$LAYOUT, sampler$OFFSET, fieldValue);
    }

    private static final AddressLayout textureView$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("textureView"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUTextureView textureView
     * }
     */
    public static final AddressLayout textureView$layout() {
        return textureView$LAYOUT;
    }

    private static final long textureView$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUTextureView textureView
     * }
     */
    public static final long textureView$offset() {
        return textureView$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUTextureView textureView
     * }
     */
    public static MemorySegment textureView(MemorySegment struct) {
        return struct.get(textureView$LAYOUT, textureView$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUTextureView textureView
     * }
     */
    public static void textureView(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(textureView$LAYOUT, textureView$OFFSET, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}

