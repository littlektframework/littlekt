package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class AssetManager(private val application: Application) {

    private val assets = mutableListOf<Asset>()

    fun add(asset: Asset) {
        assets.add(asset)
    }

    fun update() {
        assets.forEach { asset ->
            asset.load(application)
        }
        assets.clear()
    }
}