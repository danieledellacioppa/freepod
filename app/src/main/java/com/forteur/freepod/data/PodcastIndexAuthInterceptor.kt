package com.forteur.freepod.data

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

class PodcastIndexAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val timestampSeconds = (System.currentTimeMillis() / 1000L).toString()
        val authorizationHash = sha1(
            "${PodcastIndexConfig.API_KEY}${PodcastIndexConfig.API_SECRET}$timestampSeconds"
        )

        val request = chain.request().newBuilder()
            .addHeader("X-Auth-Key", PodcastIndexConfig.API_KEY)
            .addHeader("X-Auth-Date", timestampSeconds)
            .addHeader("Authorization", authorizationHash)
            .addHeader("User-Agent", "FreePod/1.0")
            .build()

        return chain.proceed(request)
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
