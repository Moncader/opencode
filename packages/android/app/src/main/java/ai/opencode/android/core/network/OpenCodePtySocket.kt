package ai.opencode.android.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OpenCodePtySocket(
  private val client: OkHttpClient,
) {
  fun connect(
    baseUrl: String,
    directory: String,
    ptyId: String,
    onOpen: () -> Unit,
    onText: (String) -> Unit,
    onError: (Throwable) -> Unit,
    onClosed: (Int) -> Unit,
  ): WebSocket {
    val parsed = baseUrl.toHttpUrlOrNull() ?: throw OpenCodeApiException("Invalid server URL: $baseUrl")
    val auth = if (parsed.username.isNotBlank()) okhttp3.Credentials.basic(parsed.username, parsed.password) else null
    val clean = parsed.newBuilder().username("").password("").build()
    val httpUrl = clean.newBuilder()
      .addPathSegment("pty")
      .addPathSegment(ptyId)
      .addPathSegment("connect")
      .addQueryParameter("directory", directory)
      .addQueryParameter("cursor", "0")
      .build()
    val url = if (httpUrl.isHttps) {
      httpUrl.toString().replaceFirst("https://", "wss://")
    } else {
      httpUrl.toString().replaceFirst("http://", "ws://")
    }

    val request = Request.Builder()
      .url(url)
      .apply {
        auth?.let { header("Authorization", it) }
      }
      .build()

    return client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        onOpen()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        onText(text)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val data = bytes.toByteArray()
        if (data.isEmpty()) return
        if (data[0] == 0.toByte()) return
        onText(bytes.utf8())
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        onError(t)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onClosed(code)
      }
    })
  }
}
