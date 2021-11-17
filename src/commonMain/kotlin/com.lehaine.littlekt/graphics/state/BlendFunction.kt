package com.lehaine.littlekt.graphics.state

import com.lehaine.littlekt.GL

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
enum class BlendFunction(val glFlag: Int) {
    /**
     * The function will add destination to the source. (srcColor * srcBlend) + (destColor * destBlend)
     */
    ADD(GL.FUNC_ADD),

    /**
     * The function will subtract destination from source. (srcColor * srcBlend) - (destColor * destBlend)
     */
    SUBTRACT(GL.FUNC_SUBTRACT),

    /**
     * The function will subtract source from destination. (destColor * destBlend) - (srcColor * srcBlend)
     */
    REVERSE_SUBTRACT(GL.FUNC_REVERSE_SUBTRACT),

    /**
     * The function will extract maximum of the source and destination. max((srcColor * srcBlend),(destColor * destBlend))
     */
    MIN(GL.MIN),

    /**
     * The function will extract minimum of the source and destination. min((srcColor * srcBlend),(destColor * destBlend))
     */
    MAX(GL.MAX)
}