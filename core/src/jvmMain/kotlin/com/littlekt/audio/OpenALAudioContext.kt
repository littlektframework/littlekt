package com.littlekt.audio

import com.littlekt.Releasable
import com.littlekt.util.datastructure.fastForEach
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicBoolean
import org.lwjgl.openal.*

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class OpenALAudioContext : Releasable, Audio {
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
            val context = ALC10.alcCreateContext(device, null as IntBuffer?)
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

    private val sources by lazy {
        val result = mutableListOf<Int>()
        for (i in 0 until 4) { // total simultaneous sources
            val sourceID: Int = AL10.alGenSources()
            if (AL10.alGetError() != AL10.AL_NO_ERROR) break
            result.add(sourceID)
        }
        result
    }

    suspend fun update() {
        if (NO_DEVICE) return
        audioStreams.fastForEach { it.update() }
    }

    fun obtainSource(): Int {
        if (NO_DEVICE) return 0

        return sources
            .asSequence()
            .map { it to AL10.alGetSourcei(it, AL10.AL_SOURCE_STATE) }
            .firstOrNull { (_, state) -> state != AL10.AL_PLAYING && state != AL10.AL_PAUSED }
            ?.let { (sourceId, _) ->
                AL10.alSourceStop(sourceId)
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
                AL10.alSourcef(sourceId, AL10.AL_GAIN, 1f)
                AL10.alSourcef(sourceId, AL10.AL_PITCH, 1f)
                AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcei(sourceId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
                sourceId
            } ?: -1
    }

    fun resumeSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePlay(it)
            }
        }
    }

    fun stopSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
            }
        }
    }

    fun pauseSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePause(it)
            }
        }
    }

    fun disposeSourceViaBufferID(bufferID: Int) {
        if (bufferID == -1) return

        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        AL10.alDeleteBuffers(bufferID)
    }

    override fun release() {
        eventCallback?.let {
            SOFTSystemEvents.nalcEventCallbackSOFT(0L, 0L)
            it.free()
        }
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == it) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        if (context != -1L) {
            ALC10.alcDestroyContext(context)
        }
        if (device != -1L) {
            ALC10.alcCloseDevice(device)
        }
    }
}
