package com.littlekt.audio

import com.littlekt.Releasable
import com.littlekt.util.datastructure.fastForEach
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicBoolean
import org.lwjgl.openal.*

/**
 * The OpenAL audio output on desktop.
 *
 * @param maxSources the maximum number of sounds that can play at once. Sources are created as
 *   they are needed. Once all are playing, a new clip takes the source of the oldest playing clip.
 * @author Colton Daily
 * @date 12/27/2021
 */
class OpenALAudioContext(val maxSources: Int = DEFAULT_MAX_SOURCES) : Releasable, Audio {
    val device =
        try {
            val byteBuffer: ByteBuffer? = null
            ALC10.alcOpenDevice(byteBuffer)
        } catch (ex: Exception) {
            ex.printStackTrace()
            -1
        }
    val context: Long =
        try {
            val deviceCaps = ALC.createCapabilities(device)
            val attributes =
                if (maxSources > OPENAL_DEFAULT_SOURCES) {
                    intArrayOf(
                        ALC11.ALC_MONO_SOURCES,
                        maxSources,
                        ALC11.ALC_STEREO_SOURCES,
                        maxSources,
                        0,
                    )
                } else {
                    null
                }
            val context = ALC10.alcCreateContext(device, attributes)
            ALC10.alcMakeContextCurrent(context)
            AL.createCapabilities(deviceCaps)
            context
        } catch (ex: Exception) {
            ex.printStackTrace()
            -1
        }

    val NO_DEVICE
        get() = context == -1L || device == -1L

    internal val audioStreams = mutableListOf<OpenALAudioStream>()

    private val devicesChanged = AtomicBoolean(false)
    private val defaultDeviceChanged = AtomicBoolean(false)
    private var selectedDevice: String? = null
    private var eventCallback: SOFTSystemEventProc? = null

    private val enumerateAll: Boolean by lazy {
        ALC10.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT")
    }

    private val deviceSpecifier: Int
        get() = if (enumerateAll) EnumerateAllExt.ALC_ALL_DEVICES_SPECIFIER else ALC10.ALC_DEVICE_SPECIFIER

    override var masterVolume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (!NO_DEVICE) AL10.alListenerf(AL10.AL_GAIN, field)
        }

    override val canSelectOutputDevice: Boolean by lazy {
        !NO_DEVICE && ALC10.alcIsExtensionPresent(device, "ALC_SOFT_reopen_device")
    }

    override val outputDevices: List<String>
        get() = if (NO_DEVICE) emptyList() else ALUtil.getStringList(0L, deviceSpecifier) ?: emptyList()

    override val outputDevice: String?
        get() = if (NO_DEVICE) null else ALC10.alcGetString(device, deviceSpecifier)

    override fun setOutputDevice(name: String?): Boolean {
        if (!canSelectOutputDevice) return false
        val reopened = SOFTReopenDevice.alcReopenDeviceSOFT(device, name as CharSequence?, null as IntBuffer?)
        if (reopened) selectedDevice = name
        return reopened
    }

    init {
        if (!NO_DEVICE && ALC10.alcIsExtensionPresent(0L, "ALC_SOFT_system_events")) {
            val events =
                intArrayOf(
                    SOFTSystemEvents.ALC_EVENT_TYPE_DEFAULT_DEVICE_CHANGED_SOFT,
                    SOFTSystemEvents.ALC_EVENT_TYPE_DEVICE_ADDED_SOFT,
                    SOFTSystemEvents.ALC_EVENT_TYPE_DEVICE_REMOVED_SOFT,
                )
            if (SOFTSystemEvents.alcEventControlSOFT(events, true)) {
                eventCallback =
                    SOFTSystemEventProc.create { eventType, deviceType, _, _, _, _ ->
                        if (deviceType == SOFTSystemEvents.ALC_PLAYBACK_DEVICE_SOFT) {
                            if (
                                eventType ==
                                    SOFTSystemEvents.ALC_EVENT_TYPE_DEFAULT_DEVICE_CHANGED_SOFT
                            ) {
                                defaultDeviceChanged.set(true)
                            }
                            devicesChanged.set(true)
                        }
                    }
                SOFTSystemEvents.alcEventCallbackSOFT(eventCallback, 0L)
            }
        }
    }

    /**
     * @return `true` once after an output device was added or removed or the default changed. Follows
     *   the default device if no device was selected with [setOutputDevice].
     */
    internal fun pollDevicesChanged(): Boolean {
        if (defaultDeviceChanged.getAndSet(false) && selectedDevice == null && canSelectOutputDevice) {
            SOFTReopenDevice.alcReopenDeviceSOFT(device, null as CharSequence?, null as IntBuffer?)
        }
        return devicesChanged.getAndSet(false)
    }

    private val sources = IntArray(maxSources)
    private val reserved = BooleanArray(maxSources)
    private val playOrder = LongArray(maxSources)
    private var sourceCount = 0
    private var playCounter = 0L

    init {
        require(maxSources > 0) { "maxSources must be greater than 0!" }
    }

    suspend fun update() {
        if (NO_DEVICE) return
        audioStreams.fastForEach { it.update() }
    }

    /**
     * Gets a source to play a sound with, reset to its defaults. An idle source is used first, then
     * a new one is created up to [maxSources], then the oldest playing clip's source is taken.
     *
     * @param reserve `true` to keep the source from being taken until [releaseSource] is called,
     *   such as for an [AudioStream]
     * @return the source id or `-1` if every source is reserved
     */
    fun obtainSource(reserve: Boolean = false): Int {
        if (NO_DEVICE) return -1
        for (i in 0 until sourceCount) {
            if (reserved[i]) continue
            val state = AL10.alGetSourcei(sources[i], AL10.AL_SOURCE_STATE)
            if (state != AL10.AL_PLAYING && state != AL10.AL_PAUSED) return claim(i, reserve)
        }
        if (sourceCount < maxSources) {
            AL10.alGetError()
            val id = AL10.alGenSources()
            if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                sources[sourceCount] = id
                return claim(sourceCount++, reserve)
            }
        }
        var oldest = -1
        for (i in 0 until sourceCount) {
            if (reserved[i]) continue
            if (oldest == -1 || playOrder[i] < playOrder[oldest]) oldest = i
        }
        return if (oldest == -1) -1 else claim(oldest, reserve)
    }

    /** Lets a source reserved with [obtainSource] be used by other sounds again. */
    fun releaseSource(sourceId: Int) {
        for (i in 0 until sourceCount) {
            if (sources[i] == sourceId) {
                reserved[i] = false
                return
            }
        }
    }

    private fun claim(index: Int, reserve: Boolean): Int {
        val sourceId = sources[index]
        AL10.alSourceStop(sourceId)
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
        AL10.alSourcef(sourceId, AL10.AL_GAIN, 1f)
        AL10.alSourcef(sourceId, AL10.AL_PITCH, 1f)
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 1f)
        AL10.alSourcei(sourceId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
        reserved[index] = reserve
        playOrder[index] = ++playCounter
        return sourceId
    }

    fun resumeSourceViaBufferID(bufferID: Int) {
        for (i in 0 until sourceCount) {
            if (AL10.alGetSourcei(sources[i], AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePlay(sources[i])
            }
        }
    }

    fun stopSourceViaBufferID(bufferID: Int) {
        for (i in 0 until sourceCount) {
            if (AL10.alGetSourcei(sources[i], AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(sources[i])
            }
        }
    }

    fun pauseSourceViaBufferID(bufferID: Int) {
        for (i in 0 until sourceCount) {
            if (AL10.alGetSourcei(sources[i], AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePause(sources[i])
            }
        }
    }

    fun disposeSourceViaBufferID(bufferID: Int) {
        if (bufferID == -1) return

        for (i in 0 until sourceCount) {
            if (AL10.alGetSourcei(sources[i], AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(sources[i])
                AL10.alSourcei(sources[i], AL10.AL_BUFFER, 0)
            }
        }
        AL10.alDeleteBuffers(bufferID)
    }

    override fun release() {
        eventCallback?.let {
            SOFTSystemEvents.nalcEventCallbackSOFT(0L, 0L)
            it.free()
        }
        if (!NO_DEVICE) {
            for (i in 0 until sourceCount) {
                AL10.alSourceStop(sources[i])
                AL10.alSourcei(sources[i], AL10.AL_BUFFER, 0)
                AL10.alDeleteSources(sources[i])
            }
        }
        sourceCount = 0
        if (context != -1L) {
            ALC10.alcDestroyContext(context)
        }
        if (device != -1L) {
            ALC10.alcCloseDevice(device)
        }
    }

    companion object {
        /** The default maximum number of sounds that can play at once. */
        const val DEFAULT_MAX_SOURCES = 32
        private const val OPENAL_DEFAULT_SOURCES = 256
    }
}
