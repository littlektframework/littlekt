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
 * struct WGPUPrimitiveDepthClipControl {
 *     WGPUChainedStruct chain;
 *     WGPUBool unclippedDepth;
 * }
 * }
 */
public class WGPUPrimitiveDepthClipControl {

    WGPUPrimitiveDepthClipControl() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        WGPUChainedStruct.layout().withName("chain"),
        WGPU.C_INT.withName("unclippedDepth"),
        MemoryLayout.paddingLayout(4)
    ).withName("WGPUPrimitiveDepthClipControl");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final GroupLayout chain$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("chain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUChainedStruct chain
     * }
     */
    public static final GroupLayout chain$layout() {
        return chain$LAYOUT;
    }

    private static final long chain$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUChainedStruct chain
     * }
     */
    public static final long chain$offset() {
        return chain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUChainedStruct chain
     * }
     */
    public static MemorySegment chain(MemorySegment struct) {
        return struct.asSlice(chain$OFFSET, chain$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUChainedStruct chain
     * }
     */
    public static void chain(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, chain$OFFSET, chain$LAYOUT.byteSize());
    }

    private static final OfInt unclippedDepth$LAYOUT = (OfInt)$LAYOUT.select(groupElement("unclippedDepth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUBool unclippedDepth
     * }
     */
    public static final OfInt unclippedDepth$layout() {
        return unclippedDepth$LAYOUT;
    }

    private static final long unclippedDepth$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUBool unclippedDepth
     * }
     */
    public static final long unclippedDepth$offset() {
        return unclippedDepth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUBool unclippedDepth
     * }
     */
    public static int unclippedDepth(MemorySegment struct) {
        return struct.get(unclippedDepth$LAYOUT, unclippedDepth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUBool unclippedDepth
     * }
     */
    public static void unclippedDepth(MemorySegment struct, int fieldValue) {
        struct.set(unclippedDepth$LAYOUT, unclippedDepth$OFFSET, fieldValue);
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

