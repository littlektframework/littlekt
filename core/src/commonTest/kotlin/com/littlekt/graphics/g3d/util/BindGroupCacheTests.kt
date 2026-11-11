package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BindGroupCacheTests {

    private class TestResource : Releasable {
        var releases = 0

        override fun release() {
            releases++
        }
    }

    private fun BindGroupCache<TestResource>.put(id: Int, notifier: ReleaseNotifier) =
        TestResource().also { put(id, notifier, notifier.version, it) }

    @Test
    fun test_put_and_get() {
        val cache = BindGroupCache<TestResource>()
        val resource = cache.put(1, ReleaseNotifier())
        assertSame(resource, cache[1])
    }

    @Test
    fun test_owner_release_evicts_on_sync() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val resource = cache.put(1, notifier)
        notifier.notifyReleased(1)
        assertEquals(0, resource.releases)
        cache.sync()
        assertEquals(1, resource.releases)
        assertNull(cache[1])
        assertEquals(0, cache.size)
    }

    @Test
    fun test_reuse_after_release_recreates() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        cache.put(1, notifier)
        notifier.notifyReleased(1)
        cache.sync()
        val recreated = cache.put(1, notifier)
        assertSame(recreated, cache[1])
        assertEquals(0, recreated.releases)
        assertEquals(1, notifier.listenerCount)
    }

    @Test
    fun test_put_with_stale_version_releases_value() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val version = notifier.version
        notifier.notifyReleased(1)
        val resource = TestResource()
        assertNull(cache.put(1, notifier, version, resource))
        assertEquals(1, resource.releases)
        assertNull(cache[1])
    }

    @Test
    fun test_put_keeps_existing_value() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val first = cache.put(1, notifier)
        val second = TestResource()
        assertSame(first, cache.put(1, notifier, notifier.version, second))
        assertEquals(1, second.releases)
    }

    @Test
    fun test_pending_added_on_sync() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val resource = TestResource()
        assertTrue(cache.markPrepared(1))
        assertFalse(cache.markPrepared(1))
        cache.addPending(1, notifier, notifier.version, resource)
        assertNull(cache[1])
        cache.sync()
        assertSame(resource, cache[1])
    }

    @Test
    fun test_pending_released_when_owner_released() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val resource = TestResource()
        cache.markPrepared(1)
        cache.addPending(1, notifier, notifier.version, resource)
        notifier.notifyReleased(1)
        assertEquals(1, resource.releases)
        cache.sync()
        assertNull(cache[1])
        assertTrue(cache.markPrepared(1))
    }

    @Test
    fun test_pending_with_stale_version_released() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        cache.markPrepared(1)
        val version = notifier.version
        notifier.notifyReleased(1)
        val resource = TestResource()
        cache.addPending(1, notifier, version, resource)
        assertEquals(1, resource.releases)
        cache.sync()
        assertNull(cache[1])
        assertTrue(cache.markPrepared(1))
    }

    @Test
    fun test_release_releases_all_once() {
        val cache = BindGroupCache<TestResource>()
        val notifierA = ReleaseNotifier()
        val notifierB = ReleaseNotifier()
        val a = cache.put(1, notifierA)
        val b = cache.put(2, notifierB)
        val pending = TestResource()
        cache.addPending(3, notifierB, notifierB.version, pending)
        cache.release()
        assertEquals(1, a.releases)
        assertEquals(1, b.releases)
        assertEquals(1, pending.releases)
        assertEquals(0, cache.size)
        assertEquals(0, notifierA.listenerCount)
    }

    @Test
    fun test_owner_release_after_cache_release_does_not_double_release() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val resource = cache.put(1, notifier)
        cache.release()
        notifier.notifyReleased(1)
        cache.sync()
        assertEquals(1, resource.releases)
    }

    @Test
    fun test_owner_release_before_cache_release_does_not_double_release() {
        val cache = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val resource = cache.put(1, notifier)
        notifier.notifyReleased(1)
        cache.release()
        assertEquals(1, resource.releases)
    }

    @Test
    fun test_owner_shared_by_caches_evicts_both() {
        val first = BindGroupCache<TestResource>()
        val second = BindGroupCache<TestResource>()
        val notifier = ReleaseNotifier()
        val a = first.put(1, notifier)
        val b = second.put(1, notifier)
        notifier.notifyReleased(1)
        first.sync()
        second.sync()
        assertEquals(1, a.releases)
        assertEquals(1, b.releases)
    }

    @Test
    fun test_evicting_one_owner_keeps_others() {
        val cache = BindGroupCache<TestResource>()
        val notifierA = ReleaseNotifier()
        val notifierB = ReleaseNotifier()
        cache.put(1, notifierA)
        val b = cache.put(2, notifierB)
        notifierA.notifyReleased(1)
        cache.sync()
        assertNull(cache[1])
        assertSame(b, cache[2])
    }
}
