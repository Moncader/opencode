package ai.opencode.android.core.repo

import ai.opencode.android.core.model.AgentDto
import ai.opencode.android.core.model.ConfigDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.FileStatusDto
import ai.opencode.android.core.model.GlobalEventDto
import ai.opencode.android.core.model.HealthDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.model.PartDto
import ai.opencode.android.core.model.PathDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.ProviderListDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.SessionMessageBundleDto
import ai.opencode.android.core.model.TodoDto
import ai.opencode.android.core.network.OpenCodeApiException
import ai.opencode.android.core.network.PromptAttachment
import ai.opencode.android.core.network.OpenCodeApiFactory
import ai.opencode.android.core.network.OpenCodePtySocket
import ai.opencode.android.core.network.OpenCodeSseClient
import ai.opencode.android.core.storage.ServerConfig
import ai.opencode.android.core.storage.ServerState
import ai.opencode.android.core.storage.ServerStore
import ai.opencode.android.core.sync.EventReducer
import ai.opencode.android.core.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.WebSocket

data class RepositoryState(
  val servers: ServerState = ServerState(),
  val health: HealthDto? = null,
  val connected: Boolean = false,
  val activeServer: String? = null,
  val path: PathDto? = null,
  val providerList: ProviderListDto? = null,
  val providerAuth: Map<String, JsonElement> = emptyMap(),
  val globalConfig: ConfigDto? = null,
  val sync: SyncState = SyncState(),
  val activeDirectory: String? = null,
  val activeSessionId: String? = null,
  val selectedModel: ModelRef? = null,
  val selectedAgent: String? = null,
  val selectedVariant: String? = null,
  val terminalPtyId: String? = null,
  val terminalOutput: String = "",
  val terminalConnected: Boolean = false,
  val agentByDirectory: Map<String, List<AgentDto>> = emptyMap(),
  val configByDirectory: Map<String, ConfigDto> = emptyMap(),
  val loading: Boolean = false,
  val error: String? = null,
)

class OpenCodeRepository(
  private val serverStore: ServerStore,
  private val apiFactory: OpenCodeApiFactory,
  private val sseClient: OpenCodeSseClient,
  private val ptySocket: OpenCodePtySocket,
  private val json: Json,
  private val scope: CoroutineScope,
) {
  private val reducer = EventReducer(json)
  private val internal = MutableStateFlow(RepositoryState())
  val state: StateFlow<RepositoryState> = internal.stateIn(scope, SharingStarted.Eagerly, internal.value)

  private val events = MutableSharedFlow<GlobalEventDto>(extraBufferCapacity = 128)
  val globalEvents = events.asSharedFlow()

  private var streamJob: Job? = null
  private var serverJob: Job? = null
  private var terminalSocket: WebSocket? = null

  fun start() {
    if (serverJob != null) return
    serverJob = scope.launch(Dispatchers.IO) {
      serverStore.state.collectLatest { servers ->
        internal.update { cur ->
          val active = servers.active ?: cur.activeServer
          cur.copy(
            servers = servers,
            activeServer = active,
          )
        }
      }
    }
  }

  suspend fun connect(url: String? = null) {
    val next = url ?: internal.value.servers.active ?: internal.value.servers.defaultUrl
    if (next.isNullOrBlank()) {
      internal.update { it.copy(connected = false, activeServer = null, error = "No server selected") }
      streamJob?.cancel()
      streamJob = null
      return
    }

    if (internal.value.servers.active != next) {
      serverStore.setActive(next)
    }

    internal.update { it.copy(loading = true, error = null, activeServer = next) }

    val base = apiFactory.base(next)
    runCatching {
      val health = base.globalHealth()
      if (!health.healthy) throw OpenCodeApiException("Server is unhealthy")

      val path = scope.async(Dispatchers.IO) { base.pathGet() }
      val projects = scope.async(Dispatchers.IO) { base.projectList() }
      val providers = scope.async(Dispatchers.IO) { base.providerList() }
      val auth = scope.async(Dispatchers.IO) { base.providerAuth() }
      val config = scope.async(Dispatchers.IO) { base.globalConfig() }

      val providerList = providers.await()

      internal.update { cur ->
        cur.copy(
          health = health,
          connected = true,
          path = path.await(),
          providerList = providerList,
          providerAuth = auth.await(),
          globalConfig = config.await(),
          sync = cur.sync.copy(projects = projects.await()),
          selectedModel = cur.selectedModel,
          loading = false,
          error = null,
        )
      }

      ensureStream(next)
    }.onFailure { err ->
      internal.update {
        it.copy(
          loading = false,
          connected = false,
          error = err.message ?: "Failed to connect",
        )
      }
    }
  }

  suspend fun refreshGlobal() {
    connect(internal.value.activeServer)
  }

  suspend fun upsertServer(url: String, name: String? = null, connectNow: Boolean = true) {
    serverStore.upsert(url = url, name = name, activate = connectNow)
    if (connectNow) connect(url)
  }

  suspend fun removeServer(url: String) {
    serverStore.remove(url)
    val current = internal.value.activeServer
    if (current == url) {
      connect()
    }
  }

  suspend fun setDefaultServer(url: String?) {
    serverStore.setDefault(url)
  }

  suspend fun selectServer(url: String) {
    serverStore.setActive(url)
    connect(url)
  }

  suspend fun openDirectory(directory: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)

    internal.update { it.copy(activeDirectory = directory, loading = true, error = null) }

    runCatching {
      val agents = scope.async(Dispatchers.IO) { api.appAgents() }
      val config = scope.async(Dispatchers.IO) { api.configGet() }
      val status = scope.async(Dispatchers.IO) { api.sessionStatus() }
      val sessions = scope.async(Dispatchers.IO) { api.sessionList() }
      val permissions = scope.async(Dispatchers.IO) { api.permissionList() }
      val questions = scope.async(Dispatchers.IO) { api.questionList() }
      val vcs = scope.async(Dispatchers.IO) { runCatching { api.vcsGet() }.getOrNull() }

      internal.update { cur ->
        val sync = cur.sync.copy(
          sessionsByDirectory = cur.sync.sessionsByDirectory + (directory to sessions.await()),
          sessionStatusByDirectory = cur.sync.sessionStatusByDirectory + (directory to status.await()),
          permissionBySession = cur.sync.permissionBySession + permissions.await().groupBy { it.sessionID },
          questionBySession = cur.sync.questionBySession + questions.await().groupBy { it.sessionID },
          vcsByDirectory = vcs.await()?.let { cur.sync.vcsByDirectory + (directory to it) } ?: (cur.sync.vcsByDirectory - directory),
        )

        val agentList = agents.await().filter { it.mode != "subagent" && !it.hidden }
        val selectedAgent = cur.selectedAgent?.takeIf { name ->
          agentList.any { it.name == name }
        }
        cur.copy(
          sync = sync,
          agentByDirectory = cur.agentByDirectory + (directory to agentList),
          configByDirectory = cur.configByDirectory + (directory to config.await()),
          selectedAgent = selectedAgent,
          loading = false,
        )
      }
    }.onFailure { err ->
      internal.update { it.copy(loading = false, error = err.message ?: "Failed to open project") }
    }
  }

  suspend fun preloadDirectory(directory: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)

    runCatching {
      val status = scope.async(Dispatchers.IO) { api.sessionStatus() }
      val sessions = scope.async(Dispatchers.IO) { api.sessionList() }
      val permissions = scope.async(Dispatchers.IO) { api.permissionList() }
      val questions = scope.async(Dispatchers.IO) { api.questionList() }

      internal.update { cur ->
        cur.copy(
          sync = cur.sync.copy(
            sessionsByDirectory = cur.sync.sessionsByDirectory + (directory to sessions.await()),
            sessionStatusByDirectory = cur.sync.sessionStatusByDirectory + (directory to status.await()),
            permissionBySession = cur.sync.permissionBySession + permissions.await().groupBy { it.sessionID },
            questionBySession = cur.sync.questionBySession + questions.await().groupBy { it.sessionID },
          ),
        )
      }
    }
  }

  suspend fun openSession(directory: String, sessionId: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)

    internal.update { it.copy(activeDirectory = directory, activeSessionId = sessionId, loading = true, error = null) }

    runCatching {
      val messages = scope.async(Dispatchers.IO) { api.sessionMessages(sessionId) }
      val todos = scope.async(Dispatchers.IO) { api.sessionTodo(sessionId) }
      val diff = scope.async(Dispatchers.IO) { api.sessionDiff(sessionId) }

      val bundles = messages.await()
      val grouped = groupSessionMessages(bundles)

      internal.update { cur ->
        cur.copy(
          sync = cur.sync.copy(
            messagesBySession = cur.sync.messagesBySession + (sessionId to grouped.first),
            partsByMessage = cur.sync.partsByMessage + grouped.second,
            todoBySession = cur.sync.todoBySession + (sessionId to todos.await()),
            sessionDiffBySession = cur.sync.sessionDiffBySession + (sessionId to diff.await()),
          ),
          activeDirectory = directory,
          activeSessionId = sessionId,
          loading = false,
          error = null,
        )
      }
    }.onFailure { err ->
      internal.update { it.copy(loading = false, error = err.message ?: "Failed to load session") }
    }
  }

  suspend fun createSession(directory: String): String? {
    val url = internal.value.activeServer ?: return null
    val api = apiFactory.scoped(url, directory)
    val next = runCatching { api.sessionCreate() }.getOrNull() ?: return null
    val session = runCatching { api.sessionGet(next.id) }.getOrNull()

    if (session != null) {
      internal.update { cur ->
        val current = cur.sync.sessionsByDirectory[directory].orEmpty()
        cur.copy(sync = cur.sync.copy(sessionsByDirectory = cur.sync.sessionsByDirectory + (directory to upsertSession(current, session))))
      }
    }

    return next.id
  }

  suspend fun sendPrompt(
    directory: String,
    sessionId: String?,
    text: String,
    attachments: List<PromptAttachment> = emptyList(),
  ): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() && attachments.isEmpty()) return sessionId

    val model = internal.value.selectedModel
    val agent = internal.value.selectedAgent
    val variant = internal.value.selectedVariant
    val url = internal.value.activeServer ?: return null
    val api = apiFactory.scoped(url, directory)

    val activeSession = sessionId ?: createSession(directory) ?: return null

    val req = api.buildPromptRequest(
      text = trimmed,
      agent = agent,
      model = model,
      variant = variant,
      attachments = attachments,
    )

    val first = runCatching {
      api.sessionPromptAsync(activeSession, req)
    }
    if (first.isSuccess) {
      internal.update { it.copy(error = null) }
      return activeSession
    }

    val failed = first.exceptionOrNull()
    if (model != null && modelRejected(failed?.message)) {
      val fallback = api.buildPromptRequest(text = trimmed, attachments = attachments)
      runCatching {
        api.sessionPromptAsync(activeSession, fallback)
      }.onSuccess {
        internal.update { it.copy(selectedModel = null, selectedVariant = null, error = null) }
        return activeSession
      }.onFailure { err ->
        internal.update { it.copy(error = err.message ?: "Failed to send prompt") }
        return activeSession
      }
    }

    internal.update { it.copy(error = failed?.message ?: "Failed to send prompt") }

    return activeSession
  }

  suspend fun terminalOpen(directory: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)

    val ptyId = internal.value.terminalPtyId ?: runCatching {
      api.ptyCreate(title = "Terminal").id
    }.getOrNull()

    if (ptyId.isNullOrBlank()) {
      internal.update { it.copy(error = "Failed to create terminal") }
      return
    }

    terminalSocket?.cancel()
    val socket = runCatching {
      ptySocket.connect(
        baseUrl = url,
        directory = directory,
        ptyId = ptyId,
        onOpen = {
          internal.update { it.copy(terminalConnected = true, error = null) }
        },
        onText = { text ->
          internal.update { cur ->
            cur.copy(terminalOutput = (cur.terminalOutput + text).takeLast(200_000))
          }
        },
        onError = { err ->
          terminalSocket = null
          internal.update {
            it.copy(
              terminalConnected = false,
              error = err.message ?: "Terminal disconnected",
            )
          }
        },
        onClosed = {
          terminalSocket = null
          internal.update { it.copy(terminalConnected = false) }
        },
      )
    }.getOrElse { err ->
      terminalSocket = null
      internal.update {
        it.copy(
          terminalConnected = false,
          error = err.message ?: "Failed to open terminal",
        )
      }
      return
    }

    terminalSocket = socket

    internal.update {
      it.copy(
        terminalPtyId = ptyId,
        terminalConnected = false,
        error = null,
      )
    }
  }

  suspend fun terminalSend(text: String) {
    if (text.isBlank()) return
    val socket = terminalSocket ?: return
    socket.send(text)
  }

  suspend fun terminalRunCommand(command: String) {
    val trimmed = command.trim()
    if (trimmed.isEmpty()) return
    terminalSend(trimmed + "\n")
  }

  suspend fun terminalClose(directory: String) {
    val ptyId = internal.value.terminalPtyId
    terminalSocket?.close(1000, "closed")
    terminalSocket = null

    val url = internal.value.activeServer
    if (url != null && !ptyId.isNullOrBlank()) {
      val api = apiFactory.scoped(url, directory)
      runCatching { api.ptyRemove(ptyId) }
    }

    internal.update {
      it.copy(
        terminalPtyId = null,
        terminalConnected = false,
        terminalOutput = "",
      )
    }
  }

  suspend fun abortSession(directory: String, sessionId: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)
    runCatching { api.sessionAbort(sessionId) }
  }

  suspend fun replyPermission(directory: String, requestId: String, reply: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)
    runCatching { api.permissionReply(requestId = requestId, reply = reply) }
      .onFailure { err -> internal.update { it.copy(error = err.message ?: "Failed to reply permission") } }
  }

  suspend fun replyQuestion(directory: String, requestId: String, answers: List<List<String>>) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)
    runCatching { api.questionReply(requestId = requestId, answers = answers) }
      .onFailure { err -> internal.update { it.copy(error = err.message ?: "Failed to reply question") } }
  }

  suspend fun rejectQuestion(directory: String, requestId: String) {
    val url = internal.value.activeServer ?: return
    val api = apiFactory.scoped(url, directory)
    runCatching { api.questionReject(requestId = requestId) }
      .onFailure { err -> internal.update { it.copy(error = err.message ?: "Failed to reject question") } }
  }

  suspend fun listFiles(directory: String, path: String): List<ai.opencode.android.core.model.FileNodeDto> {
    val url = internal.value.activeServer ?: return emptyList()
    val api = apiFactory.scoped(url, directory)
    return runCatching { api.fileList(path) }.getOrElse { emptyList() }
  }

  suspend fun readFile(directory: String, path: String): ai.opencode.android.core.model.FileContentDto? {
    val url = internal.value.activeServer ?: return null
    val api = apiFactory.scoped(url, directory)
    return runCatching { api.fileRead(path) }.getOrNull()
  }

  suspend fun fileStatus(directory: String): List<FileStatusDto> {
    val url = internal.value.activeServer ?: return emptyList()
    val api = apiFactory.scoped(url, directory)
    return runCatching { api.fileStatus() }.getOrElse { emptyList() }
  }

  suspend fun sessionDiff(directory: String, sessionId: String, messageId: String? = null): List<FileDiffDto> {
    val url = internal.value.activeServer ?: return emptyList()
    val api = apiFactory.scoped(url, directory)
    return runCatching { api.sessionDiff(sessionId = sessionId, messageId = messageId) }.getOrElse { emptyList() }
  }

  suspend fun deleteSession(directory: String, sessionId: String): Boolean {
    val url = internal.value.activeServer ?: return false
    val api = apiFactory.scoped(url, directory)
    return runCatching {
      api.sessionDelete(sessionId)
    }.map {
      internal.update { cur ->
        val sessions = cur.sync.sessionsByDirectory[directory].orEmpty().filterNot { it.id == sessionId }
        val sync = removeSessionState(
          sync = cur.sync.copy(sessionsByDirectory = cur.sync.sessionsByDirectory + (directory to sessions)),
          sessionId = sessionId,
        )
        val active = if (cur.activeSessionId == sessionId) null else cur.activeSessionId
        cur.copy(sync = sync, activeSessionId = active, error = null)
      }
      true
    }.getOrElse { err ->
      internal.update { it.copy(error = err.message ?: "Failed to delete session") }
      false
    }
  }

  suspend fun archiveSession(directory: String, sessionId: String): Boolean {
    val url = internal.value.activeServer ?: return false
    val api = apiFactory.scoped(url, directory)
    return runCatching {
      api.sessionUpdate(sessionId = sessionId, archived = System.currentTimeMillis())
    }.map {
      internal.update { cur ->
        val sessions = cur.sync.sessionsByDirectory[directory].orEmpty().filterNot { item -> item.id == sessionId }
        val sync = removeSessionState(
          sync = cur.sync.copy(sessionsByDirectory = cur.sync.sessionsByDirectory + (directory to sessions)),
          sessionId = sessionId,
        )
        val active = if (cur.activeSessionId == sessionId) null else cur.activeSessionId
        cur.copy(sync = sync, activeSessionId = active, error = null)
      }
      true
    }.getOrElse { err ->
      internal.update { it.copy(error = err.message ?: "Failed to archive session") }
      false
    }
  }

  suspend fun probeServer(url: String): Boolean {
    val api = apiFactory.base(url)
    return runCatching { api.globalHealth().healthy }.getOrElse { false }
  }

  suspend fun findProjectDirectories(query: String, limit: Int = 30): List<String> {
    val url = internal.value.activeServer ?: return emptyList()
    val api = apiFactory.base(url)
    return runCatching {
      api.findFiles(
        queryValue = query,
        dirs = true,
        type = "directory",
        limit = limit,
      )
    }.getOrElse { emptyList() }
  }

  fun selectModel(model: ModelRef?) {
    internal.update { it.copy(selectedModel = model, selectedVariant = null) }
  }

  fun selectAgent(name: String?) {
    internal.update { it.copy(selectedAgent = name) }
  }

  fun selectVariant(value: String?) {
    internal.update { it.copy(selectedVariant = value) }
  }

  fun closeSession() {
    internal.update { it.copy(activeSessionId = null) }
  }

  private fun ensureStream(url: String) {
    if (streamJob?.isActive == true) return
    streamJob = scope.launch(Dispatchers.IO) {
      while (isActive) {
        runCatching {
          sseClient.stream(url).collectLatest { event ->
            events.emit(event)
            internal.update { cur ->
              val dir = event.directory ?: "global"
              cur.copy(sync = reducer.reduce(cur.sync, dir, event.payload))
            }
          }
        }
        delay(250)
      }
    }
  }

  private fun modelRejected(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lowered = message.lowercase()
    if ("model" !in lowered) return false
    return "not supported" in lowered || "unsupported" in lowered || "invalid" in lowered
  }

  private fun upsertSession(list: List<SessionDto>, session: SessionDto): List<SessionDto> {
    val idx = list.indexOfFirst { it.id == session.id }
    if (idx == -1) return (list + session).sortedBy { it.id }
    return list.toMutableList().also { it[idx] = session }.sortedBy { it.id }
  }

  private fun removeSessionState(sync: SyncState, sessionId: String): SyncState {
    val messages = sync.messagesBySession[sessionId].orEmpty()
    val part = sync.partsByMessage.toMutableMap().also { map ->
      messages.forEach { map.remove(it.id) }
    }
    return sync.copy(
      messagesBySession = sync.messagesBySession - sessionId,
      partsByMessage = part,
      sessionDiffBySession = sync.sessionDiffBySession - sessionId,
      todoBySession = sync.todoBySession - sessionId,
      permissionBySession = sync.permissionBySession - sessionId,
      questionBySession = sync.questionBySession - sessionId,
    )
  }

  private fun groupSessionMessages(
    bundles: List<SessionMessageBundleDto>,
  ): Pair<List<ai.opencode.android.core.model.MessageDto>, Map<String, List<PartDto>>> {
    val message = bundles.map { it.info }.sortedBy { it.id }
    val part = bundles.associate { it.info.id to it.parts.sortedBy(PartDto::id) }
    return message to part
  }
}
