package com.littlekt.file

import kotlin.js.Promise
import org.w3c.dom.Image
import org.w3c.dom.ImageBitmap
import org.w3c.dom.ImageBitmapOptions
import org.w3c.fetch.Response
import org.w3c.files.Blob
import kotlin.js.definedExternally

external fun fetch(resource: String): Promise<Response>

external fun createImageBitmap(
    blob: Blob,
    options: ImageBitmapOptions = definedExternally,
): Promise<ImageBitmap>

external fun createImageBitmap(
    image: Image,
    options: ImageBitmapOptions = definedExternally,
): Promise<ImageBitmap>