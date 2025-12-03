package io.embrace.shoppingcart.telemetry

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Custom SpanExporter that logs spans in a human-readable format showing:
 * - Span name
 * - Start time
 * - End time
 * - Duration
 * - Relevant attributes (HTTP method, URL, status code, etc.)
 */
class CustomSpanExporter : SpanExporter {
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.US)

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        spans.forEach { span ->
            logSpan(span)
        }
        return CompletableResultCode.ofSuccess()
    }

    private fun logSpan(span: SpanData) {
        val startTime = span.startEpochNanos
        val endTime = span.endEpochNanos
        val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

        val startDate = dateFormat.format(Date(TimeUnit.NANOSECONDS.toMillis(startTime)))
        val endDate = dateFormat.format(Date(TimeUnit.NANOSECONDS.toMillis(endTime)))

        val attributes = span.attributes.asMap()
        val method = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("http.request.method")]
        val url = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("url.full")]
        val statusCode = attributes[io.opentelemetry.api.common.AttributeKey.longKey("http.response.status_code")]
        val requestSize = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("http.request.body.size")]
        val responseSize = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("http.response.body.size")]
        val traceparent = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("emb.w3c_traceparent")]
        val sessionId = attributes[io.opentelemetry.api.common.AttributeKey.stringKey("session.id")]

        val log = buildString {
            appendLine("┌─────────────────────────────────────────────────────────")
            appendLine("│ Span: ${span.name}")
            appendLine("├─────────────────────────────────────────────────────────")
            appendLine("│ Start:    $startDate")
            appendLine("│ End:      $endDate")
            appendLine("│ Duration: ${durationMs}ms")
            if (method != null || url != null) {
                appendLine("├─────────────────────────────────────────────────────────")
                if (method != null) appendLine("│ Method:   $method")
                if (url != null) appendLine("│ URL:      $url")
                if (statusCode != null) appendLine("│ Status:   $statusCode")
                if (requestSize != null) appendLine("│ Request:  ${requestSize} bytes")
                if (responseSize != null) appendLine("│ Response: ${responseSize} bytes")
                if (traceparent != null) appendLine("│ Traceparent: $traceparent")
                if (sessionId != null) appendLine("│ Session ID:  $sessionId")
            }
            appendLine("└─────────────────────────────────────────────────────────")
        }

        Timber.d(log)
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}
