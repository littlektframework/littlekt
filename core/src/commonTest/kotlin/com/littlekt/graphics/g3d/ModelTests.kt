package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTests {

    private class CountingResource : Releasable {
        var releases = 0

        override fun release() {
            releases++
        }
    }

    @Test
    fun test_release_owned_resources_once() {
        val model = Model()
        val mesh = CountingResource()
        val material = CountingResource()
        model.own(mesh)
        model.own(material)
        model.own(mesh)
        model.release()
        assertEquals(1, mesh.releases)
        assertEquals(1, material.releases)
    }

    @Test
    fun test_release_twice_does_nothing() {
        val model = Model()
        val mesh = CountingResource()
        model.own(mesh)
        model.release()
        model.release()
        assertEquals(1, mesh.releases)
    }
}
