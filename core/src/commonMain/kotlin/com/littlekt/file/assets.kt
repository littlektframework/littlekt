package com.littlekt.file

sealed class AssetRef

data class RawAssetRef(val url: String, val isLocal: Boolean) : AssetRef()

data class SequenceAssetRef(val url: String) : AssetRef()

sealed class LoadedAsset(val ref: AssetRef, val successful: Boolean)

class LoadedRawAsset(ref: AssetRef, val data: ByteBuffer?) : LoadedAsset(ref, data != null)

class SequenceStreamCreatedAsset(ref: AssetRef, val sequence: ByteSequenceStream?) :
    LoadedAsset(ref, sequence != null)

class FileNotFoundException(path: String) :
    Exception("File ($path) could not be found! Check to make sure it exists and is not corrupt.")

class UnsupportedFileTypeException(message: String) : Exception("Unsupported file: $message")
