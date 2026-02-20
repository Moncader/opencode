package ai.opencode.android.ui

import ai.opencode.android.core.model.FileContentDto
import ai.opencode.android.core.model.FileNodeDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.network.PromptAttachment
import ai.opencode.android.core.repo.OpenCodeRepository
import ai.opencode.android.core.repo.RepositoryState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainState(
  val repo: RepositoryState = RepositoryState(),
  val serverInput: String = "",
  val selectedProject: String? = null,
  val sessionTab: SessionTab = SessionTab.Chat,
  val filePath: String = "/",
  val fileNodes: List<FileNodeDto> = emptyList(),
  val fileContent: FileContentDto? = null,
  val fileLoading: Boolean = false,
)

enum class SessionTab {
  Chat,
  Review,
  Files,
  Terminal,
}

class MainVm(
  private val repo: OpenCodeRepository,
) : ViewModel() {
  private val project = MutableStateFlow<String?>(null)
  private val server = MutableStateFlow("")
  private val tab = MutableStateFlow(SessionTab.Chat)
  private val filePath = MutableStateFlow("/")
  private val fileNodes = MutableStateFlow<List<FileNodeDto>>(emptyList())
  private val fileContent = MutableStateFlow<FileContentDto?>(null)
  private val fileLoading = MutableStateFlow(false)

  val state: StateFlow<MainState> = combine(repo.state, server, project, tab, filePath, fileNodes, fileContent, fileLoading) { values ->
    val rs = values[0] as RepositoryState
    val url = values[1] as String
    val dir = values[2] as String?
    val sessionTab = values[3] as SessionTab
    val path = values[4] as String
    val nodes = values[5] as List<FileNodeDto>
    val content = values[6] as FileContentDto?
    val loading = values[7] as Boolean
    MainState(
      repo = rs,
      serverInput = url.ifBlank { rs.activeServer.orEmpty() },
      selectedProject = dir,
      sessionTab = sessionTab,
      filePath = path,
      fileNodes = nodes,
      fileContent = content,
      fileLoading = loading,
    )
  }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, MainState())

  init {
    viewModelScope.launch {
      repo.connect()
    }
  }

  fun connect(url: String) {
    viewModelScope.launch {
      repo.upsertServer(url = url, connectNow = true)
      server.value = url
    }
  }

  fun selectServer(url: String) {
    viewModelScope.launch {
      val prev = project.value
      if (prev != null) repo.terminalClose(prev)
      repo.selectServer(url)
      server.value = url
      project.value = null
      tab.value = SessionTab.Chat
      filePath.value = "/"
      fileNodes.value = emptyList()
      fileContent.value = null
    }
  }

  fun removeServer(url: String) {
    viewModelScope.launch {
      repo.removeServer(url)
    }
  }

  fun openProject(directory: String) {
    viewModelScope.launch {
      val prev = project.value
      if (prev != null && prev != directory) repo.terminalClose(prev)
      project.value = directory
      tab.value = SessionTab.Chat
      filePath.value = "/"
      fileNodes.value = emptyList()
      fileContent.value = null
      repo.openDirectory(directory)
    }
  }

  fun openSession(sessionId: String) {
    val dir = project.value ?: return
    viewModelScope.launch {
      tab.value = SessionTab.Chat
      repo.openSession(dir, sessionId)
    }
  }

  fun sendPrompt(text: String, attachments: List<PromptAttachment>) {
    val dir = project.value ?: return
    val id = state.value.repo.activeSessionId
    viewModelScope.launch {
      val next = repo.sendPrompt(directory = dir, sessionId = id, text = text, attachments = attachments)
      if (next != null && next != id) {
        repo.openSession(dir, next)
      }
    }
  }

  fun runShell(command: String) {
    val dir = project.value ?: return
    viewModelScope.launch {
      if (!state.value.repo.terminalConnected) {
        repo.terminalOpen(dir)
      }
      repo.terminalRunCommand(command)
    }
  }

  fun terminalConnect() {
    val dir = project.value ?: return
    viewModelScope.launch {
      repo.terminalOpen(dir)
    }
  }

  fun terminalClose() {
    val dir = project.value ?: return
    viewModelScope.launch {
      repo.terminalClose(dir)
    }
  }

  fun terminalSendRaw(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      repo.terminalSend(text)
    }
  }

  fun abort() {
    val dir = project.value ?: return
    val id = state.value.repo.activeSessionId ?: return
    viewModelScope.launch {
      repo.abortSession(dir, id)
    }
  }

  fun replyPermission(requestId: String, reply: String) {
    val dir = project.value ?: return
    viewModelScope.launch {
      repo.replyPermission(directory = dir, requestId = requestId, reply = reply)
    }
  }

  fun replyQuestion(requestId: String, answers: List<List<String>>) {
    val dir = project.value ?: return
    viewModelScope.launch {
      repo.replyQuestion(directory = dir, requestId = requestId, answers = answers)
    }
  }

  fun rejectQuestion(requestId: String) {
    val dir = project.value ?: return
    viewModelScope.launch {
      repo.rejectQuestion(directory = dir, requestId = requestId)
    }
  }

  fun refresh() {
    viewModelScope.launch {
      repo.refreshGlobal()
      val dir = project.value
      if (dir != null) repo.openDirectory(dir)
      val id = state.value.repo.activeSessionId
      if (dir != null && id != null) repo.openSession(dir, id)
    }
  }

  fun closeSession() {
    val dir = project.value
    viewModelScope.launch {
      if (dir != null) repo.terminalClose(dir)
      repo.closeSession()
      tab.value = SessionTab.Chat
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

  fun selectSessionTab(value: SessionTab) {
    tab.value = value
    val dir = project.value ?: return
    if (value == SessionTab.Files && fileNodes.value.isNotEmpty()) return
    viewModelScope.launch {
      if (value == SessionTab.Files) {
        loadFiles(dir, "/")
        return@launch
      }
      if (value == SessionTab.Terminal) {
        repo.terminalOpen(dir)
      }
    }
  }

  fun openPath(path: String, directory: Boolean) {
    val dir = project.value ?: return
    viewModelScope.launch {
      if (directory) {
        loadFiles(dir, path)
        return@launch
      }
      fileLoading.value = true
      fileContent.value = repo.readFile(dir, path)
      fileLoading.value = false
    }
  }

  fun parentPath() {
    val dir = project.value ?: return
    val current = filePath.value
    if (current == "/") return
    val trimmed = current.trimEnd('/')
    val parent = trimmed.substringBeforeLast('/', missingDelimiterValue = "")
    val next = if (parent.isBlank()) "/" else parent
    viewModelScope.launch {
      loadFiles(dir, next)
    }
  }

  private suspend fun loadFiles(directory: String, path: String) {
    fileLoading.value = true
    val nodes = repo.listFiles(directory, path)
    filePath.value = path
    fileNodes.value = nodes
    fileContent.value = null
    fileLoading.value = false
  }
}

class MainVmFactory(
  private val repo: OpenCodeRepository,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainVm(repo) as T
  }
}
