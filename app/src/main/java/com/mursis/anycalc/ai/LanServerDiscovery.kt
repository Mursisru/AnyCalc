package com.mursis.anycalc.ai

import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ищет OpenCode Server в той же Wi-Fi сети по порту 28894.
 * Сервер с паролем отвечает 401 — этого достаточно, чтобы считать его найденным.
 */
@Singleton
class LanServerDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val fastClient: OkHttpClient = client.newBuilder()
        .connectTimeout(5000, TimeUnit.MILLISECONDS)
        .readTimeout(5000, TimeUnit.MILLISECONDS)
        .build()

    suspend fun find(port: Int = 28894): String? = withContext(Dispatchers.IO) {
        val prefix = subnet()
        android.util.Log.d("LanScan", "subnet=$prefix")
        if (prefix == null) return@withContext null
        try {
            withTimeout(25000) {
                val probes = (1..254).map { host ->
                    async(Dispatchers.IO) { probe("$prefix.$host", port) }
                }
                for (probe in probes) {
                    val found = probe.await()
                    if (found != null) return@withTimeout found
                }
                null
            }
        } catch (_: TimeoutCancellationException) {
            null
        }
    }

    private fun subnet(): String? = try {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        val props = cm.getLinkProperties(network) ?: return null
        val address = props.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?: return null
        val parts = address.hostAddress?.split(".") ?: return null
        if (parts.size != 4) null else "${parts[0]}.${parts[1]}.${parts[2]}"
    } catch (_: Exception) {
        null
    }

    private fun probe(host: String, port: Int): String? = try {
        val request = Request.Builder()
            .url("http://$host:$port/api/session")
            .get()
            .build()
        fastClient.newCall(request).execute().use { response ->
            if (host.endsWith(".2")) android.util.Log.d("LanScan", "probe $host -> ${response.code}")
            // 200 — сервер без пароля, 401 — с паролем. Оба означают: сервер найден.
            if (response.code in 200..499) "http://$host:$port" else null
        }
    } catch (e: Exception) {
        if (host.endsWith(".2")) android.util.Log.d("LanScan", "probe $host error=${e.message}")
        null
    }
}
