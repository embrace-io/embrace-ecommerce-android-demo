package io.embrace.shoppingcart.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

/**
 * Interceptor that handles empty responses (200 OK with no body) by returning a default JSON response.
 * This prevents EOFException when the server returns an empty body.
 */
class EmptyResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check if response is successful (200) and has empty or null body
        if (response.isSuccessful && response.body?.contentLength() == 0L) {
            Timber.d("Empty response detected for ${request.url}, returning default JSON")

            // Return a default JSON response for successful empty responses
            val defaultJson = """{"orderId":"unknown","status":"confirmed"}"""
            val contentType = "application/json; charset=utf-8".toMediaType()
            val body = defaultJson.toResponseBody(contentType)

            return response.newBuilder()
                .body(body)
                .build()
        }

        return response
    }
}
