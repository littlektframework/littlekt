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
 * typedef void (*WGPUProcQueueWriteTexture)(WGPUQueue, const WGPUImageCopyTexture *, const void *, size_t, const WGPUTextureDataLayout *, const WGPUExtent3D *)
 * }
 */
public class WGPUProcQueueWriteTexture {

    WGPUProcQueueWriteTexture() {
        // Should not be called directly
    }

    /**
     * The function pointer signature, expressed as a functional interface
     */
    public interface Function {
        void apply(MemorySegment queue, MemorySegment destination, MemorySegment data, long dataSize, MemorySegment dataLayout, MemorySegment writeSize);
    }

    private static final FunctionDescriptor $DESC = FunctionDescriptor.ofVoid(
        WGPU.C_POINTER,
        WGPU.C_POINTER,
        WGPU.C_POINTER,
        WGPU.C_LONG_LONG,
        WGPU.C_POINTER,
        WGPU.C_POINTER
    );

    /**
     * The descriptor of this function pointer
     */
    public static FunctionDescriptor descriptor() {
        return $DESC;
    }

    private static final MethodHandle UP$MH = WGPU.upcallHandle(WGPUProcQueueWriteTexture.Function.class, "apply", $DESC);

    /**
     * Allocates a new upcall stub, whose implementation is defined by {@code fi}.
     * The lifetime of the returned segment is managed by {@code arena}
     */
    public static MemorySegment allocate(WGPUProcQueueWriteTexture.Function fi, Arena arena) {
        return Linker.nativeLinker().upcallStub(UP$MH.bindTo(fi), $DESC, arena);
    }

    private static final MethodHandle DOWN$MH = Linker.nativeLinker().downcallHandle($DESC);

    /**
     * Invoke the upcall stub {@code funcPtr}, with given parameters
     */
    public static void invoke(MemorySegment funcPtr,MemorySegment queue, MemorySegment destination, MemorySegment data, long dataSize, MemorySegment dataLayout, MemorySegment writeSize) {
        try {
             DOWN$MH.invokeExact(funcPtr, queue, destination, data, dataSize, dataLayout, writeSize);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}

