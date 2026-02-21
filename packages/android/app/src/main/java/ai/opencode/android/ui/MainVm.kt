package ai.opencode.android.ui

import ai.opencode.android.core.model.FileContentDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.FileNodeDto
import ai.opencode.android.core.model.FileStatusDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.network.PromptAttachment
import ai.opencode.android.core.repo.OpenCodeRepository
import ai.opencode.android.core.repo.RepositoryState
import ai.opencode.android.core.storage.ServerStore
import ai.opencode.android.core.storage.UiPrefsStore
import ai.opencode.android.ui.theme.ThemeMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppStage {
  Servers,
  Projects,
  Session,
}

enum class SessionTab {
  Chat,
  Terminal,
  Review,
  Files,
}

enum class FilesMode {
  Changes,
  All,
}

enum class ReviewScope {
  Session,
  LastTurn,
}

enum class ServerProbe {
  Checking,
  Online,
  Offline,
}

data class MainState(
  val repo: RepositoryState = RepositoryState(),
  val stage: AppStage = AppStage.Servers,
  val serverInput: String = "",
  val selectedProject: String? = null,
  val sessionTab: SessionTab = SessionTab.Chat,
  val filesMode: FilesMode = FilesMode.All,
  val reviewScope: ReviewScope = ReviewScope.Session,
  val reviewLastTurnDiff: List<FileDiffDto> = emptyList(),
  val reviewLoading: Boolean = false,
  val fileTree: Map<String, List<FileNodeDto>> = emptyMap(),
  val expandedDirs: Set<String> = setOf("/"),
  val filePreviewPath: String? = null,
  val fileContent: FileContentDto? = null,
  val fileLoading: Boolean = false,
  val changedFiles: List<FileStatusDto> = emptyList(),
  val serverStatus: Map<String, ServerProbe> = emptyMap(),
  val hiddenProjects: Set<String> = emptySet(),
  val manualProjects: Set<String> = emptySet(),
  val projectQuery: String = "",
  val projectSuggestions: List<String> = emptyList(),
  val themeName: String = "opencode",
  val themeMode: ThemeMode = ThemeMode.System,
  val themeNames: List<String> = emptyList(),
)

private data class UiState(
  val stage: AppStage = AppStage.Servers,
  val serverInput: String = "",
  val selectedProject: String? = null,
  val sessionTab: SessionTab = SessionTab.Chat,
  val filesMode: FilesMode = FilesMode.All,
  val reviewScope: ReviewScope = ReviewScope.Session,
  val reviewLastTurnDiff: List<FileDiffDto> = emptyList(),
  val reviewLoading: Boolean = false,
  val fileTree: Map<String, List<FileNodeDto>> = emptyMap(),
  val expandedDirs: Set<String> = setOf("/"),
  val filePreviewPath: String? = null,
  val fileContent: FileContentDto? = null,
  val fileLoading: Boolean = false,
  val changedFiles: List<FileStatusDto> = emptyList(),
  val manualProjects: Set<String> = emptySet(),
  val projectQuery: String = "",
  val projectSuggestions: List<String> = emptyList(),
)

class MainVm(
  private val repo: OpenCodeRepository,
  private val uiStore: UiPrefsStore,
  private val themeNames: List<String>,
) : ViewModel() {
  private val ui = MutableStateFlow(UiState())
  private val serverProbe = MutableStateFlow<Map<String, ServerProbe>>(emptyMap())
  private var probeJob: Job? = null
  private var queryJob: Job? = null

  val state: StateFlow<MainState> = combine(repo.state, ui, uiStore.state, serverProbe) { rs, us, pref, probe ->
    val active = rs.activeServer?.let(ServerStore::normalize)
    val hidden = active?.let { pref.hiddenProjects[it].orEmpty().toSet() } ?: emptySet()
    val stage = when {
      !rs.connected -> AppStage.Servers
      us.stage == AppStage.Session && (rs.activeSessionId == null || us.selectedProject == null) -> AppStage.Projects
      else -> us.stage
    }
    MainState(
      repo = rs,
      stage = stage,
      serverInput = us.serverInput.ifBlank { rs.activeServer.orEmpty() },
      selectedProject = us.selectedProject,
      sessionTab = us.sessionTab,
      filesMode = us.filesMode,
      reviewScope = us.reviewScope,
      reviewLastTurnDiff = us.reviewLastTurnDiff,
      reviewLoading = us.reviewLoading,
      fileTree = us.fileTree,
      expandedDirs = us.expandedDirs,
      filePreviewPath = us.filePreviewPath,
      fileContent = us.fileContent,
      fileLoading = us.fileLoading,
      changedFiles = us.changedFiles,
      serverStatus = probe,
      hiddenProjects = hidden,
      manualProjects = us.manualProjects,
      projectQuery = us.projectQuery,
      projectSuggestions = us.projectSuggestions,
      themeName = pref.theme,
      themeMode = ThemeMode.from(pref.mode),
      themeNames = themeNames,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, MainState())

  init {
    viewModelScope.launch {
      repo.connect()
    }
    viewModelScope.launch {
      repo.state
        .map { state -> state.servers.list.map { item -> item.url }.sorted() }
        .distinctUntilChanged()
        .collect { urls ->
          refreshServerProbe(urls)
        }
    }
  }

  fun connect(url: String) {
    viewModelScope.launch {
      repo.upsertServer(url = url, connectNow = true)
      ui.update {
        resetWorkspace(
          it.copy(
            serverInput = url,
            stage = if (repo.state.value.connected) AppStage.Projects else AppStage.Servers,
          ),
        )
      }
      refreshServerProbe(repo.state.value.servers.list.map { it.url })
    }
  }

  fun selectServer(url: String) {
    viewModelScope.launch {
      val prev = ui.value.selectedProject
      if (prev != null) repo.terminalClose(prev)
      repo.selectServer(url)
      ui.update {
        resetWorkspace(
          it.copy(
            serverInput = url,
            stage = if (repo.state.value.connected) AppStage.Projects else AppStage.Servers,
          ),
        )
      }
      refreshServerProbe(repo.state.value.servers.list.map { it.url })
    }
  }

  fun showServerScreen() {
    val dir = ui.value.selectedProject
    if (dir != null) {
      viewModelScope.launch {
        repo.terminalClose(dir)
      }
    }
    ui.update { it.copy(stage = AppStage.Servers) }
  }

  fun showProjectScreen() {
    if (!repo.state.value.connected) return
    ui.update { it.copy(stage = AppStage.Projects) }
  }

  fun removeServer(url: String) {
    viewModelScope.launch {
      repo.removeServer(url)
      refreshServerProbe(repo.state.value.servers.list.map { it.url })
      if (!repo.state.value.connected) {
        ui.update { resetWorkspace(it.copy(stage = AppStage.Servers)) }
      }
    }
  }

  fun refreshServerStatus() {
    refreshServerProbe(repo.state.value.servers.list.map { it.url })
  }

  fun updateProjectQuery(query: String) {
    ui.update { it.copy(projectQuery = query) }
    queryJob?.cancel()
    val trimmed = query.trim()
    if (trimmed.length < 2) {
      ui.update { it.copy(projectSuggestions = emptyList()) }
      return
    }
    queryJob = viewModelScope.launch {
      delay(220)
      val result = repo.findProjectDirectories(trimmed)
      ui.update { cur ->
        if (cur.projectQuery.trim() != trimmed) return@update cur
        cur.copy(projectSuggestions = result)
      }
    }
  }

  fun openProject(directory: String) {
    if (directory.isBlank()) return
    viewModelScope.launch {
      val prev = ui.value.selectedProject
      if (prev != null && prev != directory) repo.terminalClose(prev)
      repo.openDirectory(directory)
      ui.update {
        resetSessionPanels(
          it.copy(
            stage = AppStage.Projects,
            selectedProject = directory,
            manualProjects = it.manualProjects + directory,
            projectSuggestions = emptyList(),
          ),
        )
      }
      loadRootFiles(directory)
    }
  }

  fun hideProject(directory: String) {
    val server = repo.state.value.activeServer ?: return
    viewModelScope.launch {
      uiStore.hideProject(server, directory)
      if (ui.value.selectedProject == directory) {
        repo.terminalClose(directory)
        repo.closeSession()
        ui.update { resetSessionPanels(it.copy(selectedProject = null, stage = AppStage.Projects)) }
      }
    }
  }

  fun unhideProject(directory: String) {
    val server = repo.state.value.activeServer ?: return
    viewModelScope.launch {
      uiStore.showProject(server, directory)
    }
  }

  fun createSession() {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      val next = repo.createSession(dir) ?: return@launch
      repo.openSession(dir, next)
      ui.update { it.copy(stage = AppStage.Session, sessionTab = SessionTab.Chat) }
    }
  }

  fun openSession(sessionId: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      repo.openSession(dir, sessionId)
      ui.update { it.copy(stage = AppStage.Session, sessionTab = SessionTab.Chat) }
    }
  }

  fun deleteSession(sessionId: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      val ok = repo.deleteSession(directory = dir, sessionId = sessionId)
      if (!ok) return@launch
      if (repo.state.value.activeSessionId == null) {
        ui.update { it.copy(stage = AppStage.Projects) }
      }
    }
  }

  fun archiveSession(sessionId: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      val ok = repo.archiveSession(directory = dir, sessionId = sessionId)
      if (!ok) return@launch
      if (repo.state.value.activeSessionId == null) {
        ui.update { it.copy(stage = AppStage.Projects) }
      }
    }
  }

  fun sendPrompt(text: String, attachments: List<PromptAttachment>) {
    val dir = ui.value.selectedProject ?: return
    val id = state.value.repo.activeSessionId
    viewModelScope.launch {
      val next = repo.sendPrompt(directory = dir, sessionId = id, text = text, attachments = attachments)
      if (next == null) return@launch
      if (next != id) {
        repo.openSession(dir, next)
      }
      ui.update { it.copy(stage = AppStage.Session, sessionTab = SessionTab.Chat) }
    }
  }

  fun abort() {
    val dir = ui.value.selectedProject ?: return
    val id = state.value.repo.activeSessionId ?: return
    viewModelScope.launch {
      repo.abortSession(dir, id)
    }
  }

  fun replyPermission(requestId: String, reply: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      repo.replyPermission(directory = dir, requestId = requestId, reply = reply)
    }
  }

  fun replyQuestion(requestId: String, answers: List<List<String>>) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      repo.replyQuestion(directory = dir, requestId = requestId, answers = answers)
    }
  }

  fun rejectQuestion(requestId: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      repo.rejectQuestion(directory = dir, requestId = requestId)
    }
  }

  fun selectSessionTab(value: SessionTab) {
    ui.update { it.copy(sessionTab = value) }
    val dir = ui.value.selectedProject ?: return
    if (value == SessionTab.Terminal) {
      viewModelScope.launch {
        repo.terminalOpen(dir)
      }
      return
    }
    if (value == SessionTab.Files) {
      viewModelScope.launch {
        if (ui.value.filesMode == FilesMode.Changes) {
          loadChangedFiles(dir)
          return@launch
        }
        ensureRootFiles(dir)
      }
      return
    }
    if (value == SessionTab.Review && ui.value.reviewScope == ReviewScope.LastTurn) {
      viewModelScope.launch {
        loadLastTurnDiff()
      }
    }
  }

  fun selectReviewScope(value: ReviewScope) {
    ui.update { it.copy(reviewScope = value) }
    if (value != ReviewScope.LastTurn) return
    viewModelScope.launch {
      loadLastTurnDiff()
    }
  }

  fun selectFilesMode(value: FilesMode) {
    ui.update { it.copy(filesMode = value) }
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      if (value == FilesMode.Changes) {
        loadChangedFiles(dir)
        return@launch
      }
      ensureRootFiles(dir)
    }
  }

  fun toggleDirectory(path: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      val open = ui.value.expandedDirs
      if (path in open) {
        ui.update { it.copy(expandedDirs = it.expandedDirs - path) }
        return@launch
      }
      ui.update { it.copy(expandedDirs = it.expandedDirs + path) }
      if (ui.value.fileTree[path] != null) return@launch
      loadFiles(path = path, directory = dir)
    }
  }

  fun openFile(path: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      ui.update { it.copy(fileLoading = true, filePreviewPath = path) }
      val content = repo.readFile(dir, path)
      ui.update { it.copy(fileLoading = false, fileContent = content, filePreviewPath = path) }
    }
  }

  fun closeFilePreview() {
    ui.update { it.copy(filePreviewPath = null, fileContent = null, fileLoading = false) }
  }

  fun refresh() {
    viewModelScope.launch {
      repo.refreshGlobal()
      val dir = ui.value.selectedProject
      if (dir != null) {
        repo.openDirectory(dir)
      }
      val id = repo.state.value.activeSessionId
      if (dir != null && id != null) {
        repo.openSession(dir, id)
      }
      if (dir != null && ui.value.filesMode == FilesMode.Changes) {
        loadChangedFiles(dir)
      }
      if (dir != null && ui.value.filesMode == FilesMode.All && ui.value.fileTree.isNotEmpty()) {
        ensureRootFiles(dir)
      }
      if (ui.value.reviewScope == ReviewScope.LastTurn) {
        loadLastTurnDiff()
      }
      refreshServerProbe(repo.state.value.servers.list.map { it.url })
    }
  }

  fun closeSession() {
    val dir = ui.value.selectedProject
    viewModelScope.launch {
      if (dir != null) repo.terminalClose(dir)
      repo.closeSession()
      ui.update { it.copy(stage = AppStage.Projects, sessionTab = SessionTab.Chat) }
    }
  }

  fun openTerminal() {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      repo.terminalOpen(dir)
    }
  }

  fun runTerminalCommand(command: String) {
    val dir = ui.value.selectedProject ?: return
    viewModelScope.launch {
      if (repo.state.value.terminalPtyId == null) {
        repo.terminalOpen(dir)
      }
      repo.terminalRunCommand(command)
    }
  }

  fun selectModel(model: ModelRef?) {
    repo.selectModel(model)
  }

  fun selectAgent(agent: String?) {
    repo.selectAgent(agent)
  }

  fun selectVariant(variant: String?) {
    repo.selectVariant(variant)
  }

  fun setTheme(name: String) {
    viewModelScope.launch {
      uiStore.setTheme(name)
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      uiStore.setMode(mode.value)
    }
  }

  private suspend fun loadLastTurnDiff() {
    val dir = ui.value.selectedProject ?: return
    val sessionId = repo.state.value.activeSessionId ?: return
    val messages = repo.state.value.sync.messagesBySession[sessionId].orEmpty()
    val lastUser = messages.filter { it.role == "user" }.maxByOrNull { it.time.created } ?: run {
      ui.update { it.copy(reviewLastTurnDiff = emptyList(), reviewLoading = false) }
      return
    }
    ui.update { it.copy(reviewLoading = true) }
    val diff = repo.sessionDiff(directory = dir, sessionId = sessionId, messageId = lastUser.id)
    ui.update { it.copy(reviewLastTurnDiff = diff, reviewLoading = false) }
  }

  private suspend fun ensureRootFiles(directory: String) {
    if (ui.value.fileTree["/"] != null) return
    loadRootFiles(directory)
  }

  private suspend fun loadRootFiles(directory: String) {
    ui.update {
      it.copy(
        fileTree = emptyMap(),
        expandedDirs = setOf("/"),
      )
    }
    loadFiles(path = "/", directory = directory)
  }

  private suspend fun loadFiles(path: String, directory: String) {
    val list = repo.listFiles(directory, path)
      .sortedWith(compareBy<FileNodeDto> { !isDirectory(it) }.thenBy { it.name.lowercase() })
    ui.update { cur ->
      cur.copy(fileTree = cur.fileTree + (path to list))
    }
  }

  private suspend fun loadChangedFiles(directory: String) {
    val files = repo.fileStatus(directory)
      .sortedWith(compareBy<FileStatusDto> { it.path.lowercase() })
    ui.update { it.copy(changedFiles = files) }
  }

  private fun refreshServerProbe(urls: List<String>) {
    probeJob?.cancel()
    if (urls.isEmpty()) {
      serverProbe.value = emptyMap()
      return
    }
    probeJob = viewModelScope.launch {
      serverProbe.value = urls.associateWith { ServerProbe.Checking }
      val result = urls.map { url ->
        async(Dispatchers.IO) {
          url to if (repo.probeServer(url)) ServerProbe.Online else ServerProbe.Offline
        }
      }.awaitAll().toMap()
      serverProbe.value = result
    }
  }

  private fun resetWorkspace(state: UiState): UiState {
    return state.copy(
      selectedProject = null,
      sessionTab = SessionTab.Chat,
      filesMode = FilesMode.All,
      reviewScope = ReviewScope.Session,
      reviewLastTurnDiff = emptyList(),
      reviewLoading = false,
      fileTree = emptyMap(),
      expandedDirs = setOf("/"),
      filePreviewPath = null,
      fileContent = null,
      fileLoading = false,
      changedFiles = emptyList(),
      projectSuggestions = emptyList(),
    )
  }

  private fun resetSessionPanels(state: UiState): UiState {
    return state.copy(
      sessionTab = SessionTab.Chat,
      filesMode = FilesMode.All,
      reviewScope = ReviewScope.Session,
      reviewLastTurnDiff = emptyList(),
      reviewLoading = false,
      fileTree = emptyMap(),
      expandedDirs = setOf("/"),
      filePreviewPath = null,
      fileContent = null,
      fileLoading = false,
      changedFiles = emptyList(),
    )
  }

  private fun isDirectory(item: FileNodeDto): Boolean {
    val type = item.type.lowercase()
    return type == "dir" || type == "directory" || type == "folder"
  }
}

class MainVmFactory(
  private val repo: OpenCodeRepository,
  private val uiStore: UiPrefsStore,
  private val themeNames: List<String>,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainVm(
      repo = repo,
      uiStore = uiStore,
      themeNames = themeNames,
    ) as T
  }
}
