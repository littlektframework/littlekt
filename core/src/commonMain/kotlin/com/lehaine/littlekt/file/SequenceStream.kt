package com.lehaine.littlekt.file

/**
 * @author Colton Daily
 * @date 1/12/2022
 */
interface SequenceStream: Sequence<Byte> {

    fun close()
}