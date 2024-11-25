package com.littlekt.file.gltf

import com.littlekt.file.ByteBuffer
import com.littlekt.file.vfs.VfsFile
import com.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
internal object GltfLoader {
    private const val GLTF_MAGIC_NUMBER = 0x46546C67
    private const val GLTF_CHUNK_JSON = 0x4E4F534A
    private const val GLTF_CHUNK_BIN = 0x004E4942
    private val logger = Logger<GltfLoader>()

    suspend fun loadGlb(file: VfsFile): GltfData {
        val data = file.readStream()
        val magic = data.readUInt()
        if (magic != GLTF_MAGIC_NUMBER) {
            error("Unexpected glTF magic number: '$magic'. Expected magic should be '$GLTF_MAGIC_NUMBER'.")
        }
        val version = data.readUInt()

        if (version != 2) {
            error("Unsupported glTF version found: '$version'. Only glTF 2.0 is supported.")
        }

        var chunkLength = data.readUInt()
        var chunkType = data.readUInt()
        if (chunkType != GLTF_CHUNK_JSON) {
            error("Unexpected chunk type for chunk 0: '$chunkType'. Expected chunk type to be $GLTF_CHUNK_JSON / 'JSON'")
        }

        val gltfData = file.vfs.json.decodeFromString<GltfData>(data.readChunk(chunkLength).toString())

        var chunk = 1
        while (data.hasRemaining()) {
            chunkLength = data.readUInt()
            chunkType = data.readUInt()
            if (chunkType == GLTF_CHUNK_BIN) {
                gltfData.buffers[chunk - 1].data = ByteBuffer(data.readChunk(chunkLength))
            } else {
                logger.warn { "Unexpected chunk type for chunk $chunk: '$chunkType'. Expected chunk type to be $GLTF_CHUNK_BIN / 'BIN'" }
                data.skip(chunkLength)
            }
            chunk++
        }

        return gltfData
    }

}