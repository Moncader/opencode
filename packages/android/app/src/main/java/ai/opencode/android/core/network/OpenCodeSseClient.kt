package ai.opencode.android.core.network

import ai.opencode.android.core.model.GlobalEventDto
import java.net.URI
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class OpenCodeSseClient(
  private val client: OkHttpClient,
  private val json: Json,
) {
  fun stream(baseUrl: String): Flow<GlobalEventDto> = callbackFlow {
    val endpoint = runCatching {
      val parsed = URI(baseUrl)
      val base = URI(
        parsed.scheme,
        null,
        parsed.host,
        parsed.port,
        null,
        null,
        null,
      )
      val event = base.resolve("/global/event").toString()
      val auth = when {
        parsed.userInfo.isNullOrBlank() -> null
        else -> {
          val chunks = parsed.userInfo.split(":", limit = 2)
          Credentials.basic(chunks.getOrElse(0) { "" }, chunks.getOrElse(1) { "" })
        }
      }
      event to auth
    }.getOrElse {
      close(OpenCodeApiException("Invalid SSE URL: $baseUrl"))
      return@callbackFlow
    }

    val request = Request.Builder()
      .url(endpoint.first)
      .header("Accept", "text/event-stream")
      .apply {
        endpoint.second?.let { header("Authorization", it) }
      }
      .build()

    val source = EventSources.createFactory(client).newEventSource(
      request,
      object : EventSourceListener() {
        override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
          val parsed = runCatching { json.decodeFromString(GlobalEventDto.serializer(), data) }.getOrNull() ?: return
          trySend(parsed)
        }

        override fun onFailure(source: EventSource, t: Throwable?, response: okhttp3.Response?) {
          if (isClosedForSend) return
          val code = response?.code
          val message = buildString {
            append("SSE disconnected")
            if (code != null) append(" (HTTP $code)")
            if (t?.message != null) append(": ${t.message}")
          }
          close(OpenCodeApiException(message))
        }
      },
    )

    awaitClose {
      source.cancel()
    }
  }
}
