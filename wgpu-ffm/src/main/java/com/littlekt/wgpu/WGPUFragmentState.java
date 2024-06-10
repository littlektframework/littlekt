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
 * struct WGPUFragmentState {
 *     const WGPUChainedStruct *nextInChain;
 *     WGPUShaderModule module;
 *     const char *entryPoint;
 *     size_t constantCount;
 *     const WGPUConstantEntry *constants;
 *     size_t targetCount;
 *     const WGPUColorTargetState *targets;
 * }
 * }
 */
public class WGPUFragmentState {

    WGPUFragmentState() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        WGPU.C_POINTER.withName("nextInChain"),
        WGPU.C_POINTER.withName("module"),
        WGPU.C_POINTER.withName("entryPoint"),
        WGPU.C_LONG_LONG.withName("constantCount"),
        WGPU.C_POINTER.withName("constants"),
        WGPU.C_LONG_LONG.withName("targetCount"),
        WGPU.C_POINTER.withName("targets")
    ).withName("WGPUFragmentState");

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

    private static final AddressLayout module$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("module"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUShaderModule module
     * }
     */
    public static final AddressLayout module$layout() {
        return module$LAYOUT;
    }

    private static final long module$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUShaderModule module
     * }
     */
    public static final long module$offset() {
        return module$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUShaderModule module
     * }
     */
    public static MemorySegment module(MemorySegment struct) {
        return struct.get(module$LAYOUT, module$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUShaderModule module
     * }
     */
    public static void module(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(module$LAYOUT, module$OFFSET, fieldValue);
    }

    private static final AddressLayout entryPoint$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("entryPoint"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const char *entryPoint
     * }
     */
    public static final AddressLayout entryPoint$layout() {
        return entryPoint$LAYOUT;
    }

    private static final long entryPoint$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const char *entryPoint
     * }
     */
    public static final long entryPoint$offset() {
        return entryPoint$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const char *entryPoint
     * }
     */
    public static MemorySegment entryPoint(MemorySegment struct) {
        return struct.get(entryPoint$LAYOUT, entryPoint$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const char *entryPoint
     * }
     */
    public static void entryPoint(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(entryPoint$LAYOUT, entryPoint$OFFSET, fieldValue);
    }

    private static final OfLong constantCount$LAYOUT = (OfLong)$LAYOUT.select(groupElement("constantCount"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * size_t constantCount
     * }
     */
    public static final OfLong constantCount$layout() {
        return constantCount$LAYOUT;
    }

    private static final long constantCount$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * size_t constantCount
     * }
     */
    public static final long constantCount$offset() {
        return constantCount$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * size_t constantCount
     * }
     */
    public static long constantCount(MemorySegment struct) {
        return struct.get(constantCount$LAYOUT, constantCount$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * size_t constantCount
     * }
     */
    public static void constantCount(MemorySegment struct, long fieldValue) {
        struct.set(constantCount$LAYOUT, constantCount$OFFSET, fieldValue);
    }

    private static final AddressLayout constants$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("constants"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPUConstantEntry *constants
     * }
     */
    public static final AddressLayout constants$layout() {
        return constants$LAYOUT;
    }

    private static final long constants$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPUConstantEntry *constants
     * }
     */
    public static final long constants$offset() {
        return constants$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPUConstantEntry *constants
     * }
     */
    public static MemorySegment constants(MemorySegment struct) {
        return struct.get(constants$LAYOUT, constants$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPUConstantEntry *constants
     * }
     */
    public static void constants(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(constants$LAYOUT, constants$OFFSET, fieldValue);
    }

    private static final OfLong targetCount$LAYOUT = (OfLong)$LAYOUT.select(groupElement("targetCount"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * size_t targetCount
     * }
     */
    public static final OfLong targetCount$layout() {
        return targetCount$LAYOUT;
    }

    private static final long targetCount$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * size_t targetCount
     * }
     */
    public static final long targetCount$offset() {
        return targetCount$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * size_t targetCount
     * }
     */
    public static long targetCount(MemorySegment struct) {
        return struct.get(targetCount$LAYOUT, targetCount$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * size_t targetCount
     * }
     */
    public static void targetCount(MemorySegment struct, long fieldValue) {
        struct.set(targetCount$LAYOUT, targetCount$OFFSET, fieldValue);
    }

    private static final AddressLayout targets$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("targets"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPUColorTargetState *targets
     * }
     */
    public static final AddressLayout targets$layout() {
        return targets$LAYOUT;
    }

    private static final long targets$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPUColorTargetState *targets
     * }
     */
    public static final long targets$offset() {
        return targets$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPUColorTargetState *targets
     * }
     */
    public static MemorySegment targets(MemorySegment struct) {
        return struct.get(targets$LAYOUT, targets$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPUColorTargetState *targets
     * }
     */
    public static void targets(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(targets$LAYOUT, targets$OFFSET, fieldValue);
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

