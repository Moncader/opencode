package ai.opencode.android

import android.app.Application
import ai.opencode.android.core.network.OpenCodeApiFactory
import ai.opencode.android.core.network.OpenCodePtySocket
import ai.opencode.android.core.network.OpenCodeSseClient
import ai.opencode.android.core.repo.OpenCodeRepository
import ai.opencode.android.core.storage.ServerStore
import ai.opencode.android.core.storage.UiPrefsStore
import ai.opencode.android.ui.theme.OpenCodeThemeCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class OpenCodeApp : Application() {
  lateinit var graph: AppGraph
    private set

  override fun onCreate() {
    super.onCreate()
    graph = AppGraph(this)
    graph.repo.start()
  }
}

class AppGraph(app: Application) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
  }

  private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    })
    .build()

  private val store = ServerStore(app)
  val uiStore = UiPrefsStore(app)
  val themeCatalog = OpenCodeThemeCatalog(app)
  private val api = OpenCodeApiFactory(client = client, json = json)
  private val sse = OpenCodeSseClient(client = client, json = json)
  private val pty = OpenCodePtySocket(client = client)

  val repo = OpenCodeRepository(
    serverStore = store,
    apiFactory = api,
    sseClient = sse,
    ptySocket = pty,
    json = json,
    scope = scope,
  )
}
