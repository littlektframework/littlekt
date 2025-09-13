package com.littlekt.file

import com.littlekt.Context as LittleKtContext
import android.content.Context as AndroidContext
import com.littlekt.log.Logger
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidUrlVfs(
    littleKtContext: LittleKtContext,
    androidContext: AndroidContext,
    logger: Logger
) : UrlVfs(littleKtContext, logger) {

    private val client: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClientProvider.new(
            cacheDirPath = androidContext.cacheDir.absolutePath, log = logger
        )
    }

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        check(!rawRef.isLocal) { "Only http resource assets may be loaded!" }

        val data: ByteBufferImpl? = if (rawRef.url.startsWith("data:", ignoreCase = true)) {
            decodeDataUrl(rawRef.url)
        } else {
            withContext(Dispatchers.IO) {
                val req = Request.Builder().url(rawRef.url).cacheControl(CacheControl.Builder().build()).build()
                try {
                    client.newCall(req).await().use { resp ->
                        if (resp.isSuccessful.not()) {
                            logger.error { "Failed downloading ${rawRef.url}: HTTP ${resp.code}" }
                            null
                        } else {
                            val bytes = resp.body.bytes()
                            ByteBufferImpl(bytes)
                        }
                    }
                } catch (e: Exception) {
                    logger.error { "Failed loading asset ${rawRef.url}: $e" }
                    null
                }
            }
        }

        return LoadedRawAsset(rawRef, data)
    }

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        val sequence: ByteSequenceStream? = withContext(Dispatchers.IO) {
            try {
                val stream = openUrlStream(sequenceRef.url)
                JvmByteSequenceStream(stream)
            } catch (e: Exception) {
                logger.error { "Failed creating buffered sequence for ${sequenceRef.url}: $e" }
                null
            }
        }
        return SequenceStreamCreatedAsset(sequenceRef, sequence)
    }

    private suspend fun openUrlStream(url: String): InputStream {
        return if (url.startsWith("data:", ignoreCase = true)) {
            val buffer = decodeDataUrl(url)
            ByteArrayInputStream(buffer.toArray())
        } else {
            val req = Request.Builder().url(url).cacheControl(CacheControl.Builder().build()).build()

            val resp = client.newCall(req).await()
            if (resp.isSuccessful.not()) {
                resp.close()
                throw IllegalStateException("Failed downloading $url: HTTP ${resp.code}")
            }

            val body = resp.body

            object : FilterInputStream(body.byteStream()) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        body.close()
                        resp.close()
                    }
                }
            }
        }
    }

    private fun decodeDataUrl(dataUrl: String): ByteBufferImpl {
        val marker = ";base64,"
        val idx = dataUrl.indexOf(marker)
        require(idx >= 0) { "Unsupported data URL: missing $marker" }
        val base64 = dataUrl.substring(idx + marker.length)
        val bytes = Base64.getDecoder().decode(base64.toByteArray())
        return ByteBufferImpl(bytes)
    }
}

private object OkHttpClientProvider {
    fun new(cacheDirPath: String, log: Logger): OkHttpClient {
        val cacheSizeBytes = 64L * 1024L * 1024L // 64MB cache

        return OkHttpClient.Builder().cache(Cache(java.io.File(cacheDirPath, "okhttp-cache"), cacheSizeBytes))
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true)
            .followRedirects(true).followSslRedirects(true)
            .addInterceptor { chain ->
                val ua = "LittleKt/AndroidUrlVfs (+okhttp)"
                chain.proceed(
                    chain.request().newBuilder().header("User-Agent", ua).build()
                )
            }.build()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            if (cont.isCancelled.not()) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isCancelled) {
                try { response.close() } catch (_: Throwable) {}
                return
            }
            cont.resume(response)
        }
    })

    cont.invokeOnCancellation {
        try { cancel() } catch (_: Throwable) {}
    }
}
