package io.embrace.shoppingcart.telemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

// =============================================================================
// PII SCRUBBING AT THE EXPORTER LAYER (Wrapper Use Cases §5).
//
// WHY HERE AND NOT AT CALL SITES
//   Feature code can always forget. An exporter-layer scrub is the one place
//   every span flows through on its way OUT of the process, so a missed
//   call-site review can't leak an email into a third-party destination.
//
// WHAT THIS COVERS — AND WHAT IT DOESN'T
//   This wraps exporters added via Embrace.addJavaSpanExporter(...), i.e.
//   spans forwarded to EXTERNAL OTel destinations (the demo's Timber logger,
//   a customer's collector, Grafana, etc.).
//
//   It does NOT sit in front of Embrace's own ingestion path — the Embrace
//   SDK has its own redaction for sensitive keys. The belt-and-suspenders
//   posture for a real app is:
//     1. Don't put PII in attributes in the first place (wrapper docs, §5).
//     2. Scrub at this layer for any external exporter.
//
// PATTERNS are deliberately conservative: emails, 13–19 digit card-like runs,
// and phone-like runs. Tune for your domain (order IDs that look like card
// numbers will get redacted — better safe).
// =============================================================================

class PiiScrubbingSpanExporter(private val delegate: SpanExporter) : SpanExporter {

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        return delegate.export(spans.map(::scrub))
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    private fun scrub(span: SpanData): SpanData {
        var dirty = false
        val builder = Attributes.builder()
        span.attributes.forEach { key, value ->
            if (value is String) {
                val scrubbed = scrubValue(value)
                if (scrubbed !== value) dirty = true
                @Suppress("UNCHECKED_CAST")
                builder.put(key as io.opentelemetry.api.common.AttributeKey<String>, scrubbed)
            } else {
                @Suppress("UNCHECKED_CAST")
                builder.put(key as io.opentelemetry.api.common.AttributeKey<Any>, value)
            }
        }
        if (!dirty) return span
        val scrubbedAttributes = builder.build()
        return object : DelegatingSpanData(span) {
            override fun getAttributes(): Attributes = scrubbedAttributes
        }
    }

    private fun scrubValue(value: String): String {
        var result = value
        for (pattern in PII_PATTERNS) {
            result = pattern.replace(result, REDACTED)
        }
        return if (result == value) value else result
    }

    private companion object {
        const val REDACTED = "<redacted>"

        val PII_PATTERNS = listOf(
            // Email addresses.
            Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""),
            // Card-like digit runs (13–19 digits, optional space/dash separators).
            Regex("""\b(?:\d[ -]?){13,19}\b"""),
            // Phone-like: international prefix + 7-15 digits.
            Regex("""\+\d[\d ()-]{6,14}\d"""),
        )
    }
}
