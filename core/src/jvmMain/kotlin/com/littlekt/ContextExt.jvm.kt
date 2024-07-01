package com.littlekt

import com.littlekt.file.Vfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.input.Input
import com.littlekt.log.Logger
import com.littlekt.util.Clipboard

internal actual var ownedContext: Context =
    object : Context() {
        override val stats: AppStats
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val configuration: ContextConfiguration
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val graphics: Graphics
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val input: Input
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val logger: Logger
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val vfs: Vfs
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val resourcesVfs: VfsFile
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val storageVfs: VfsFile
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val platform: Platform
            get() = error("Internal Context that shouldn't ever be referenced!")

        override val clipboard: Clipboard
            get() = error("Internal Context that shouldn't ever be referenced!")

        override fun start(build: (app: Context) -> ContextListener) {
            error("Internal Context that shouldn't ever be referenced!")
        }

        override fun close() {
            error("Internal Context that shouldn't ever be referenced!")
        }
    }
