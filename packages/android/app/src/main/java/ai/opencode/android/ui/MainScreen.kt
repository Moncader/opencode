package ai.opencode.android.ui

import ai.opencode.android.core.model.AgentDto
import ai.opencode.android.core.model.FileContentDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.FileNodeDto
import ai.opencode.android.core.model.FileStatusDto
import ai.opencode.android.core.model.MessageDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.model.PartDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.ProviderListDto
import ai.opencode.android.core.model.QuestionRequestDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.TodoDto
import ai.opencode.android.core.network.PromptAttachment
import ai.opencode.android.ui.theme.LocalOpenCodeColors
import ai.opencode.android.ui.theme.ThemeMode
import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  state: MainState,
  onConnect: (String) -> Unit,
  onSelectServer: (String) -> Unit,
  onShowServerScreen: () -> Unit,
  onShowProjectScreen: () -> Unit,
  onRemoveServer: (String) -> Unit,
  onRefreshServerStatus: () -> Unit,
  onUpdateProjectQuery: (String) -> Unit,
  onOpenProject: (String) -> Unit,
  onHideProject: (String) -> Unit,
  onUnhideProject: (String) -> Unit,
  onCreateSession: () -> Unit,
  onOpenSession: (String) -> Unit,
  onDeleteSession: (String) -> Unit,
  onArchiveSession: (String) -> Unit,
  onSendPrompt: (String, List<PromptAttachment>) -> Unit,
  onAbort: () -> Unit,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
  onSelectSessionTab: (SessionTab) -> Unit,
  onOpenTerminal: () -> Unit,
  onRunTerminalCommand: (String) -> Unit,
  onSelectReviewScope: (ReviewScope) -> Unit,
  onSelectFilesMode: (FilesMode) -> Unit,
  onToggleDirectory: (String) -> Unit,
  onOpenFile: (String) -> Unit,
  onCloseFilePreview: () -> Unit,
  onCloseSession: () -> Unit,
  onRefresh: () -> Unit,
  onSetTheme: (String) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
) {
  var serverInput by remember(state.serverInput) { mutableStateOf(state.serverInput) }
  var settingsOpen by rememberSaveable { mutableStateOf(false) }
  var showContextDetails by rememberSaveable { mutableStateOf(false) }
  var prompt by rememberSaveable { mutableStateOf("") }
  var sessionsProject by rememberSaveable { mutableStateOf<String?>(null) }
  var projectTerminalOpen by rememberSaveable { mutableStateOf(false) }
  var promptScrollTick by rememberSaveable { mutableStateOf(0) }

  val ui = LocalOpenCodeColors.current
  val context = LocalContext.current
  val keyboard = LocalSoftwareKeyboardController.current
  val attachments = remember { mutableStateListOf<PromptAttachment>() }
  val picker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
    val next = uri?.let { loadAttachment(context, it) } ?: return@rememberLauncherForActivityResult
    attachments.add(next)
  }

  val directory = state.selectedProject
  val sessionId = state.repo.activeSessionId
  val messages = remember(state.repo.sync.messagesBySession, sessionId) {
    sessionId?.let { state.repo.sync.messagesBySession[it].orEmpty().sortedBy { msg -> msg.time.created } }.orEmpty()
  }
  val providers = state.repo.providerList
  val metrics = remember(messages, providers) { sessionMetrics(messages, providers) }
  val canAbort = remember(directory, sessionId, state.repo.sync.sessionStatusByDirectory) {
    if (directory == null || sessionId == null) return@remember false
    val type = state.repo.sync.sessionStatusByDirectory[directory]?.get(sessionId)?.type?.lowercase(Locale.getDefault())
    type == "busy" || type == "running" || type == "retry"
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (state.stage == AppStage.Session) {
            IconButton(onClick = onCloseSession) {
              Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to projects")
            }
          } else if (state.stage == AppStage.Projects) {
            IconButton(onClick = onShowServerScreen) {
              Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to servers")
            }
          }
        },
        title = {
          when (state.stage) {
            AppStage.Servers -> {
              Column {
                Text("OpenCode", fontWeight = FontWeight.SemiBold)
                Text("Server Selection", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
              }
            }

            AppStage.Projects, AppStage.Session -> {
              Column {
                Text(
                  if (state.stage == AppStage.Projects) "Project Management" else "Session",
                  fontWeight = FontWeight.SemiBold,
                )
                ServerSubtitle(
                  server = state.repo.activeServer,
                  status = state.repo.activeServer?.let { state.serverStatus[it] },
                )
              }
            }
          }
        },
        actions = {
          IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
          }
          if (state.stage == AppStage.Servers) {
            IconButton(onClick = onRefreshServerStatus) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = "Refresh status")
            }
          }
          IconButton(onClick = { settingsOpen = true }) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
          }
        },
      )
    },
    bottomBar = {
      if (state.stage == AppStage.Session && state.sessionTab == SessionTab.Chat && directory != null) {
        PromptDock(
          value = prompt,
          onValue = { prompt = it },
          attachments = attachments,
          onAttach = { picker.launch(ACCEPTED_ATTACHMENT_TYPES) },
          onRemoveAttachment = { index ->
            if (index in attachments.indices) attachments.removeAt(index)
          },
          providers = state.repo.providerList,
          agents = state.repo.agentByDirectory[directory].orEmpty(),
          selectedModel = state.repo.selectedModel,
          selectedAgent = state.repo.selectedAgent,
          selectedVariant = state.repo.selectedVariant,
          onSelectModel = onSelectModel,
          onSelectAgent = onSelectAgent,
          onSelectVariant = onSelectVariant,
          canAbort = canAbort,
          onSend = {
            promptScrollTick += 1
            onSendPrompt(prompt, attachments.toList())
            prompt = ""
            attachments.clear()
            keyboard?.hide()
          },
          onAbort = onAbort,
        )
      }
    },
  ) { pad ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(pad)
        .background(MaterialTheme.colorScheme.background),
    ) {
      when (state.stage) {
        AppStage.Servers -> {
          ServerScreen(
            input = serverInput,
            loading = state.repo.loading,
            servers = state.repo.servers.list.map { it.url },
            active = state.repo.activeServer,
            status = state.serverStatus,
            connected = state.repo.connected,
            onInput = {
              serverInput = it
            },
            onConnect = {
              onConnect(serverInput)
            },
            onUse = onSelectServer,
            onRemove = onRemoveServer,
            onContinue = onShowProjectScreen,
          )
        }

        AppStage.Projects -> {
          ProjectScreen(
            projects = state.repo.sync.projects,
            hiddenProjects = state.hiddenProjects,
            manualProjects = state.manualProjects,
            selected = state.selectedProject,
            sessionsByDirectory = state.repo.sync.sessionsByDirectory,
            statusByDirectory = state.repo.sync.sessionStatusByDirectory,
            permissionBySession = state.repo.sync.permissionBySession,
            questionBySession = state.repo.sync.questionBySession,
            activeSession = state.repo.activeSessionId,
            query = state.projectQuery,
            suggestions = state.projectSuggestions,
            onQuery = onUpdateProjectQuery,
            onOpenProject = onOpenProject,
            onHideProject = onHideProject,
            onCreateSession = onCreateSession,
            onOpenSession = onOpenSession,
            onOpenTerminal = {
              onOpenTerminal()
              projectTerminalOpen = true
            },
            onShowAllSessions = { sessionsProject = it },
          )
        }

        AppStage.Session -> {
          if (directory == null || sessionId == null) {
            EmptyState(
              title = "Session unavailable",
              body = "Select a project and session to continue.",
              action = {
                onShowProjectScreen()
              },
              actionLabel = "Back to projects",
            )
          } else {
            SessionScreen(
              directory = directory,
              sessionId = sessionId,
              promptScrollTick = promptScrollTick,
              messages = messages,
              parts = state.repo.sync.partsByMessage,
              permissions = state.repo.sync.permissionBySession[sessionId].orEmpty(),
              questions = state.repo.sync.questionBySession[sessionId].orEmpty(),
              todos = state.repo.sync.todoBySession[sessionId].orEmpty(),
              sessionDiff = state.repo.sync.sessionDiffBySession[sessionId].orEmpty(),
              reviewDiff = if (state.reviewScope == ReviewScope.Session) {
                state.repo.sync.sessionDiffBySession[sessionId].orEmpty()
              } else {
                state.reviewLastTurnDiff
              },
              reviewScope = state.reviewScope,
              reviewLoading = state.reviewLoading,
              sessionTab = state.sessionTab,
              filesMode = state.filesMode,
              fileTree = state.fileTree,
              expandedDirs = state.expandedDirs,
              changedFiles = state.changedFiles,
              onSelectTab = onSelectSessionTab,
              terminalConnected = state.repo.terminalConnected,
              terminalOutput = state.repo.terminalOutput,
              onOpenTerminal = onOpenTerminal,
              onRunTerminalCommand = onRunTerminalCommand,
              onSelectReviewScope = onSelectReviewScope,
              onSelectFilesMode = onSelectFilesMode,
              onToggleDirectory = onToggleDirectory,
              onOpenFile = onOpenFile,
              onReplyPermission = onReplyPermission,
              onReplyQuestion = onReplyQuestion,
              onRejectQuestion = onRejectQuestion,
              onOpenContext = {
                showContextDetails = true
              },
              contextUsage = metrics.usage,
            )
          }
        }
      }

      val err = state.repo.error
      if (err != null) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
          Text(
            text = err,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    }
  }

  if (settingsOpen) {
    SettingsSheet(
      theme = state.themeName,
      themeMode = state.themeMode,
      themes = state.themeNames,
      hiddenProjects = state.hiddenProjects,
      onSetTheme = onSetTheme,
      onSetThemeMode = onSetThemeMode,
      onUnhideProject = onUnhideProject,
      onDismiss = { settingsOpen = false },
    )
  }

  if (showContextDetails) {
    ContextDialog(
      metrics = metrics,
      onDismiss = { showContextDetails = false },
    )
  }

  val previewPath = state.filePreviewPath
  if (previewPath != null) {
    FilePreviewDialog(
      path = previewPath,
      file = state.fileContent,
      loading = state.fileLoading,
      onClose = onCloseFilePreview,
    )
  }

  val allSessionsFor = sessionsProject
  if (allSessionsFor != null) {
    val sessions = state.repo.sync.sessionsByDirectory[allSessionsFor].orEmpty().sortedByDescending { it.time.updated }
    AllSessionsSheet(
      project = allSessionsFor,
      sessions = sessions,
      active = state.repo.activeSessionId,
      onOpen = {
        onOpenSession(it)
        sessionsProject = null
      },
      onDelete = onDeleteSession,
      onArchive = onArchiveSession,
      onDismiss = { sessionsProject = null },
    )
  }

  LaunchedEffect(state.stage) {
    if (state.stage != AppStage.Projects) {
      projectTerminalOpen = false
    }
  }

  if (projectTerminalOpen && state.stage == AppStage.Projects && directory != null) {
    ProjectTerminalSheet(
      project = directory,
      connected = state.repo.terminalConnected,
      output = state.repo.terminalOutput,
      onReconnect = onOpenTerminal,
      onRunCommand = onRunTerminalCommand,
      onDismiss = { projectTerminalOpen = false },
    )
  }
}

@Composable
private fun ServerSubtitle(server: String?, status: ServerProbe?) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    StatusDot(status = status)
    Text(
      text = server ?: "No server selected",
      style = MaterialTheme.typography.labelSmall,
      color = LocalOpenCodeColors.current.textMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun StatusDot(status: ServerProbe?) {
  val color = when (status) {
    ServerProbe.Online -> Color(0xFF38D884)
    ServerProbe.Offline -> MaterialTheme.colorScheme.error
    ServerProbe.Checking -> MaterialTheme.colorScheme.tertiary
    null -> LocalOpenCodeColors.current.textMuted
  }
  Box(
    modifier = Modifier
      .size(10.dp)
      .background(color = color, shape = CircleShape),
  )
}

@Composable
private fun ServerScreen(
  input: String,
  loading: Boolean,
  servers: List<String>,
  active: String?,
  status: Map<String, ServerProbe>,
  connected: Boolean,
  onInput: (String) -> Unit,
  onConnect: () -> Unit,
  onUse: (String) -> Unit,
  onRemove: (String) -> Unit,
  onContinue: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = ui.panel),
      border = BorderStroke(1.dp, ui.borderSubtle),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Connect to server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
          value = input,
          onValueChange = onInput,
          label = { Text("Server URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(onClick = onConnect, enabled = !loading && input.isNotBlank()) {
            Text("Connect")
          }
          if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          }
          if (connected) {
            OutlinedButton(onClick = onContinue) {
              Text("Enter server")
            }
          }
        }
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = ui.panel),
      border = BorderStroke(1.dp, ui.borderSubtle),
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Saved servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (servers.isEmpty()) {
          Text("No servers added yet", color = ui.textMuted)
        }
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(servers, key = { it }) { url ->
            Card(
              colors = CardDefaults.cardColors(containerColor = ui.element),
              border = BorderStroke(1.dp, if (url == active) ui.borderActive else ui.borderSubtle),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  StatusDot(status[url])
                  Text(
                    text = url,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (url == active) {
                    AssistChip(onClick = {}, label = { Text("Active") })
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(onClick = { onUse(url) }) {
                    Text(if (url == active) "Reconnect" else "Use")
                  }
                  OutlinedButton(onClick = { onRemove(url) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProjectScreen(
  projects: List<ProjectDto>,
  hiddenProjects: Set<String>,
  manualProjects: Set<String>,
  selected: String?,
  sessionsByDirectory: Map<String, List<SessionDto>>,
  statusByDirectory: Map<String, Map<String, ai.opencode.android.core.model.SessionStatusDto>>,
  permissionBySession: Map<String, List<ai.opencode.android.core.model.PermissionRequestDto>>,
  questionBySession: Map<String, List<QuestionRequestDto>>,
  activeSession: String?,
  query: String,
  suggestions: List<String>,
  onQuery: (String) -> Unit,
  onOpenProject: (String) -> Unit,
  onHideProject: (String) -> Unit,
  onCreateSession: () -> Unit,
  onOpenSession: (String) -> Unit,
  onOpenTerminal: () -> Unit,
  onShowAllSessions: (String) -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  val known = remember(projects) { projects.associateBy { it.worktree } }
  val entries = remember(known, manualProjects, hiddenProjects) {
    (known.keys + manualProjects)
      .filterNot { it in hiddenProjects }
      .sortedBy { it.lowercase(Locale.getDefault()) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = ui.panel),
      border = BorderStroke(1.dp, ui.borderSubtle),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Open project path", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
          value = query,
          onValueChange = onQuery,
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
          label = { Text("Path on server") },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(onClick = { if (query.isNotBlank()) onOpenProject(query.trim()) }, enabled = query.isNotBlank()) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open")
          }
          if (suggestions.isNotEmpty()) {
            Text("Suggestions", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
          }
        }
        if (suggestions.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            suggestions.take(16).forEach { path ->
              AssistChip(onClick = { onOpenProject(path) }, label = { Text(path, maxLines = 1) })
            }
          }
        }
      }
    }

    if (entries.isEmpty()) {
      EmptyState(
        title = "No projects",
        body = "Open a project path or connect to a server with known projects.",
      )
      return@Column
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(entries, key = { it }) { worktree ->
        val project = known[worktree]
        val sessions = sessionsByDirectory[worktree].orEmpty().sortedByDescending { it.time.updated }
        val status = statusByDirectory[worktree].orEmpty()
        val recent = sessions.firstOrNull()
        val attention = sessions.filter { item ->
          val needsPrompt = permissionBySession[item.id].orEmpty().isNotEmpty() || questionBySession[item.id].orEmpty().isNotEmpty()
          val state = status[item.id]?.type
          needsPrompt || state == "busy" || state == "retry"
        }

        ProjectSection(
          worktree = worktree,
          label = project?.name?.ifBlank { null } ?: project?.worktree ?: worktree,
          selected = selected == worktree,
          sessions = sessions,
          attention = attention,
          recent = recent,
          activeSession = activeSession,
          onOpen = { onOpenProject(worktree) },
          onHide = { onHideProject(worktree) },
          onCreateSession = onCreateSession,
          onOpenSession = onOpenSession,
          onOpenTerminal = onOpenTerminal,
          onShowAll = { onShowAllSessions(worktree) },
        )
      }
    }
  }
}

@Composable
private fun ProjectSection(
  worktree: String,
  label: String,
  selected: Boolean,
  sessions: List<SessionDto>,
  attention: List<SessionDto>,
  recent: SessionDto?,
  activeSession: String?,
  onOpen: () -> Unit,
  onHide: () -> Unit,
  onCreateSession: () -> Unit,
  onOpenSession: (String) -> Unit,
  onOpenTerminal: () -> Unit,
  onShowAll: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  var confirmHide by remember(worktree) { mutableStateOf(false) }

  LaunchedEffect(confirmHide) {
    if (!confirmHide) return@LaunchedEffect
    delay(2600)
    confirmHide = false
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = ui.panel),
    border = BorderStroke(1.dp, if (selected) ui.borderActive else ui.borderSubtle),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
          Text(worktree, style = MaterialTheme.typography.labelSmall, color = ui.textMuted, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          if (selected) {
            AssistChip(onClick = {}, label = { Text("Open") })
          }
          IconButton(onClick = onOpen) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Open project")
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onOpen) {
          Text(if (selected) "Refresh project" else "Enter")
        }
        OutlinedButton(onClick = onCreateSession, enabled = selected) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("New session")
        }
        OutlinedButton(onClick = onOpenTerminal, enabled = selected) {
          Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Terminal")
        }
        OutlinedButton(onClick = {
          if (confirmHide) {
            onHide()
            confirmHide = false
          } else {
            confirmHide = true
          }
        }) {
          Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (confirmHide) "Confirm hide" else "Hide")
        }
      }

      if (selected) {
        if (recent != null) {
          SessionShortcut(
            label = "Most recent",
            session = recent,
            active = recent.id == activeSession,
            onOpen = { onOpenSession(recent.id) },
          )
        }
        if (attention.isNotEmpty()) {
          Text("Needs attention", style = MaterialTheme.typography.labelMedium, color = ui.warning)
          attention.take(4).forEach { item ->
            SessionShortcut(
              label = "Attention",
              session = item,
              active = item.id == activeSession,
              warn = true,
              onOpen = { onOpenSession(item.id) },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onShowAll, enabled = sessions.isNotEmpty()) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("All sessions")
          }
          Text(
            text = "${sessions.size} session${if (sessions.size == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = ui.textMuted,
            modifier = Modifier.align(Alignment.CenterVertically),
          )
        }
      }
    }
  }
}

@Composable
private fun SessionShortcut(
  label: String,
  session: SessionDto,
  active: Boolean,
  warn: Boolean = false,
  onOpen: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  val title = session.title.ifBlank { session.slug }
  val color = if (warn) ui.warning else ui.info
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen),
    colors = CardDefaults.cardColors(containerColor = if (active) ui.element else Color.Transparent),
    border = BorderStroke(1.dp, if (active) ui.borderActive else ui.borderSubtle),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = if (warn) Icons.Rounded.WarningAmber else Icons.Rounded.Description,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
      }
      if (active) {
        AssistChip(onClick = {}, label = { Text("Active") })
      }
    }
  }
}

@Composable
private fun SessionScreen(
  directory: String,
  sessionId: String,
  promptScrollTick: Int,
  messages: List<MessageDto>,
  parts: Map<String, List<PartDto>>,
  permissions: List<ai.opencode.android.core.model.PermissionRequestDto>,
  questions: List<QuestionRequestDto>,
  todos: List<TodoDto>,
  sessionDiff: List<FileDiffDto>,
  reviewDiff: List<FileDiffDto>,
  reviewScope: ReviewScope,
  reviewLoading: Boolean,
  sessionTab: SessionTab,
  filesMode: FilesMode,
  fileTree: Map<String, List<FileNodeDto>>,
  expandedDirs: Set<String>,
  changedFiles: List<FileStatusDto>,
  onSelectTab: (SessionTab) -> Unit,
  terminalConnected: Boolean,
  terminalOutput: String,
  onOpenTerminal: () -> Unit,
  onRunTerminalCommand: (String) -> Unit,
  onSelectReviewScope: (ReviewScope) -> Unit,
  onSelectFilesMode: (FilesMode) -> Unit,
  onToggleDirectory: (String) -> Unit,
  onOpenFile: (String) -> Unit,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
  onOpenContext: () -> Unit,
  contextUsage: Int,
) {
  val ui = LocalOpenCodeColors.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = ui.panel),
      border = BorderStroke(1.dp, ui.borderSubtle),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(directory, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("Session $sessionId", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
        }
        ContextCircle(usage = contextUsage, onOpen = onOpenContext)
      }
    }

    SessionTabs(tab = sessionTab, onSelect = onSelectTab)

    when (sessionTab) {
      SessionTab.Chat -> {
        AttentionPanel(
          permissions = permissions,
          questions = questions,
          onReplyPermission = onReplyPermission,
          onReplyQuestion = onReplyQuestion,
          onRejectQuestion = onRejectQuestion,
        )
        MessageTimeline(
          sessionId = sessionId,
          promptScrollTick = promptScrollTick,
          messages = messages,
          parts = parts,
        )
      }

      SessionTab.Terminal -> {
        TerminalPanel(
          title = "Session terminal",
          subtitle = directory,
          connected = terminalConnected,
          output = terminalOutput,
          onReconnect = onOpenTerminal,
          onRunCommand = onRunTerminalCommand,
        )
      }

      SessionTab.Review -> {
        ReviewPanel(
          todos = todos,
          fullDiff = sessionDiff,
          visibleDiff = reviewDiff,
          scope = reviewScope,
          loading = reviewLoading,
          onScope = onSelectReviewScope,
        )
      }

      SessionTab.Files -> {
        FilesPanel(
          mode = filesMode,
          onMode = onSelectFilesMode,
          fileTree = fileTree,
          expanded = expandedDirs,
          changed = changedFiles,
          onToggleDirectory = onToggleDirectory,
          onOpenFile = onOpenFile,
        )
      }
    }
  }
}

@Composable
private fun ContextCircle(usage: Int, onOpen: () -> Unit) {
  val bounded = usage.coerceIn(0, 100)
  Box(
    modifier = Modifier
      .size(36.dp)
      .clickable(onClick = onOpen),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
      progress = { bounded / 100f },
      strokeWidth = 3.dp,
      modifier = Modifier.fillMaxSize(),
    )
    Text("$bounded%", style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun SessionTabs(tab: SessionTab, onSelect: (SessionTab) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SessionTabButton(active = tab == SessionTab.Chat, label = "Chat") { onSelect(SessionTab.Chat) }
      SessionTabButton(active = tab == SessionTab.Terminal, label = "Terminal") { onSelect(SessionTab.Terminal) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SessionTabButton(active = tab == SessionTab.Review, label = "Review") { onSelect(SessionTab.Review) }
      SessionTabButton(active = tab == SessionTab.Files, label = "Files") { onSelect(SessionTab.Files) }
    }
  }
}

@Composable
private fun SessionTabButton(active: Boolean, label: String, onClick: () -> Unit) {
  if (active) {
    Button(onClick = onClick) { Text(label) }
    return
  }
  OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun AttentionPanel(
  permissions: List<ai.opencode.android.core.model.PermissionRequestDto>,
  questions: List<QuestionRequestDto>,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
) {
  if (permissions.isEmpty() && questions.isEmpty()) return
  val ui = LocalOpenCodeColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.element),
    border = BorderStroke(1.dp, ui.warning.copy(alpha = 0.32f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ui.warning)
        Text("Needs attention", style = MaterialTheme.typography.titleSmall)
      }

      permissions.forEach { item ->
        Card(border = BorderStroke(1.dp, ui.borderSubtle), colors = CardDefaults.cardColors(containerColor = ui.panel)) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(item.permission, fontWeight = FontWeight.Medium)
            if (item.patterns.isNotEmpty()) {
              Text(item.patterns.joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = ui.textMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(onClick = { onReplyPermission(item.id, "reject") }) { Text("Deny") }
              OutlinedButton(onClick = { onReplyPermission(item.id, "once") }) { Text("Allow once") }
              Button(onClick = { onReplyPermission(item.id, "always") }) { Text("Always allow") }
            }
          }
        }
      }

      questions.forEach { req ->
        QuestionCard(
          request = req,
          onReply = onReplyQuestion,
          onReject = onRejectQuestion,
        )
      }
    }
  }
}

@Composable
private fun QuestionCard(
  request: QuestionRequestDto,
  onReply: (String, List<List<String>>) -> Unit,
  onReject: (String) -> Unit,
) {
  val questions = request.questions
  if (questions.isEmpty()) return
  var answers by remember(request.id) { mutableStateOf(List(questions.size) { emptyList<String>() }) }

  fun toggle(index: Int, label: String, multiple: Boolean) {
    answers = answers.mapIndexed { i, value ->
      if (i != index) return@mapIndexed value
      if (!multiple) return@mapIndexed listOf(label)
      if (label in value) return@mapIndexed value - label
      value + label
    }
  }

  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("Question", style = MaterialTheme.typography.titleSmall)
      questions.forEachIndexed { index, q ->
        Text(q.question, fontWeight = FontWeight.Medium)
        if (q.multiple) {
          Text("Select one or more options", style = MaterialTheme.typography.labelSmall)
        }
        q.options.forEach { option ->
          val picked = option.label in answers[index]
          FilterChip(
            selected = picked,
            onClick = { toggle(index, option.label, q.multiple) },
            label = { Text(option.label) },
          )
        }
        if (q.custom) {
          var custom by remember(request.id, index) { mutableStateOf("") }
          OutlinedTextField(
            value = custom,
            onValueChange = { custom = it },
            label = { Text("Type your own answer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                if (custom.isBlank()) return@OutlinedButton
                toggle(index, custom.trim(), q.multiple)
                custom = ""
              },
              enabled = custom.isNotBlank(),
            ) {
              Text("Add")
            }
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onReject(request.id) }) {
          Text("Reject")
        }
        Button(onClick = { onReply(request.id, answers) }) {
          Text("Submit")
        }
      }
    }
  }
}

@Composable
private fun MessageTimeline(
  sessionId: String,
  promptScrollTick: Int,
  messages: List<MessageDto>,
  parts: Map<String, List<PartDto>>,
) {
  val listState = rememberLazyListState()
  var jumped by remember(sessionId) { mutableStateOf(false) }
  var lastUser by remember(sessionId) { mutableStateOf<String?>(null) }
  val canStick by remember {
    derivedStateOf {
      val total = listState.layoutInfo.totalItemsCount
      if (total == 0) return@derivedStateOf true
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      lastVisible >= total - 2
    }
  }

  val tail = remember(messages, parts) {
    val last = messages.lastOrNull() ?: return@remember "none"
    val payload = parts[last.id].orEmpty().joinToString("|") { part ->
      "${part.id}:${part.type}:${part.text?.length ?: 0}:${part.state?.status ?: ""}"
    }
    "${messages.size}:$payload"
  }

  LaunchedEffect(sessionId, messages.size) {
    if (jumped) return@LaunchedEffect
    if (messages.isEmpty()) return@LaunchedEffect
    listState.scrollToItem(messages.lastIndex)
    jumped = true
  }

  LaunchedEffect(promptScrollTick) {
    if (messages.isEmpty()) return@LaunchedEffect
    listState.animateScrollToItem(messages.lastIndex)
  }

  val userTail = remember(messages) { messages.lastOrNull { it.role == "user" }?.id }
  LaunchedEffect(userTail, messages.size) {
    val next = userTail ?: return@LaunchedEffect
    if (lastUser == null) {
      lastUser = next
      return@LaunchedEffect
    }
    if (lastUser == next) return@LaunchedEffect
    lastUser = next
    listState.animateScrollToItem(messages.lastIndex)
  }

  LaunchedEffect(tail) {
    if (!canStick) return@LaunchedEffect
    if (messages.isEmpty()) return@LaunchedEffect
    listState.animateScrollToItem(messages.lastIndex)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items(messages, key = { it.id }) { message ->
      MessageCard(
        message = message,
        parts = parts[message.id].orEmpty().sortedBy { it.id },
      )
    }
  }
}

@Composable
private fun MessageCard(message: MessageDto, parts: List<PartDto>) {
  val ui = LocalOpenCodeColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.panel),
    border = BorderStroke(1.dp, ui.borderSubtle),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val role = when (message.role) {
          "assistant" -> assistantTitle(parts)
          "user" -> "You"
          else -> message.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        Text(role, fontWeight = FontWeight.SemiBold)
        Text(timeStamp(message.time.created), style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
      }

      if (message.role == "user") {
        val text = parts.filter { it.type == "text" }.joinToString("\n\n") { it.text.orEmpty() }.trim()
        if (text.isNotBlank()) {
          MarkdownText(text = text)
        }
        val files = parts.filter { it.type == "file" }
        if (files.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            files.forEach { item ->
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = ui.textMuted, modifier = Modifier.size(16.dp))
                Text(item.filename ?: "attachment", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      if (message.role == "assistant") {
        AssistantParts(parts)
      }
    }
  }
}

@Composable
private fun AssistantParts(parts: List<PartDto>) {
  parts.forEach { part ->
    if (isImplicitStep(part.type)) return@forEach
    when (part.type) {
      "text" -> {
        val text = part.text?.trim().orEmpty()
        if (text.isNotBlank()) {
          MarkdownText(text = text)
        }
      }

      "reasoning" -> {
        CollapsiblePart(
          title = "Thinking",
          icon = Icons.Rounded.ExpandMore,
        ) {
          MarkdownText(part.text.orEmpty())
        }
      }

      "tool" -> {
        ToolPartCard(part)
      }

      else -> {
        val value = part.text ?: part.description ?: part.prompt ?: part.name ?: part.type
        if (value.isNotBlank()) {
          CollapsiblePart(
            title = part.type,
            icon = Icons.Rounded.ChevronRight,
          ) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
          }
        }
      }
    }
  }
}

@Composable
private fun ToolPartCard(part: PartDto) {
  val ui = LocalOpenCodeColors.current
  val state = part.state
  val status = state?.status ?: "pending"
  val color = when (status) {
    "completed" -> ui.success
    "error" -> MaterialTheme.colorScheme.error
    "running", "pending" -> ui.warning
    else -> ui.info
  }
  val title = buildString {
    append(toolTitle(part.tool ?: "tool"))
    if (!state?.title.isNullOrBlank()) {
      append(" - ")
      append(state?.title)
    }
  }
  val output = state?.output?.trim().orEmpty()
  val error = state?.error?.trim().orEmpty()
  val input = prettyJson(state?.input)

  CollapsiblePart(
    title = title,
    icon = toolIcon(part.tool),
    badge = status,
    tint = color,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (input.isNotBlank()) {
        Text("Input", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
        CodeBlock(input)
      }
      if (output.isNotBlank()) {
        Text("Output", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
        CodeBlock(output)
      }
      if (error.isNotBlank()) {
        Text("Error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        CodeBlock(error)
      }
      if (output.isBlank() && error.isBlank()) {
        Text("No output", style = MaterialTheme.typography.bodySmall, color = ui.textMuted)
      }
    }
  }
}

@Composable
private fun CollapsiblePart(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  badge: String? = null,
  tint: Color = LocalOpenCodeColors.current.info,
  content: @Composable () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  var open by rememberSaveable(title) { mutableStateOf(false) }
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.element),
    border = BorderStroke(1.dp, ui.borderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { open = !open }
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
          title,
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (badge != null) {
          Text(
            badge,
            style = MaterialTheme.typography.labelSmall,
            color = ui.textMuted,
          )
        }
        Icon(
          if (open) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
          contentDescription = null,
          tint = ui.textMuted,
        )
      }
      AnimatedVisibility(visible = open) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 10.dp),
        ) {
          content()
        }
      }
    }
  }
}

@Composable
private fun MarkdownText(text: String) {
  val ui = LocalOpenCodeColors.current
  val blocks = remember(text) { parseMarkdown(text) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Code -> CodeBlock(block.value)
        is MarkdownBlock.Heading -> Text(
          block.value,
          style = MaterialTheme.typography.titleSmall,
          color = ui.markdownLink,
          fontWeight = FontWeight.SemiBold,
        )

        is MarkdownBlock.Quote -> Card(
          colors = CardDefaults.cardColors(containerColor = ui.element),
          border = BorderStroke(1.dp, ui.borderSubtle),
        ) {
          Text(
            text = block.value,
            color = ui.markdownText,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
          )
        }

        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("•", color = ui.markdownLink)
          Text(block.value, style = MaterialTheme.typography.bodySmall, color = ui.markdownText)
        }

        is MarkdownBlock.Text -> Text(
          text = block.value,
          style = MaterialTheme.typography.bodySmall,
          color = ui.markdownText,
        )
      }
    }
  }
}

@Composable
private fun CodeBlock(text: String) {
  val ui = LocalOpenCodeColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.diffContextBg),
    border = BorderStroke(1.dp, ui.borderSubtle),
  ) {
    Text(
      text = text,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      fontFamily = FontFamily.Monospace,
      style = MaterialTheme.typography.bodySmall,
      color = ui.markdownCode,
    )
  }
}

@Composable
private fun ReviewPanel(
  todos: List<TodoDto>,
  fullDiff: List<FileDiffDto>,
  visibleDiff: List<FileDiffDto>,
  scope: ReviewScope,
  loading: Boolean,
  onScope: (ReviewScope) -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SessionTabButton(active = scope == ReviewScope.Session, label = "Whole session") { onScope(ReviewScope.Session) }
      SessionTabButton(active = scope == ReviewScope.LastTurn, label = "Last turn") { onScope(ReviewScope.LastTurn) }
    }

    if (todos.isNotEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = ui.panel),
        border = BorderStroke(1.dp, ui.borderSubtle),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text("Todo", style = MaterialTheme.typography.titleSmall)
          todos.forEach { item ->
            val status = item.status.lowercase()
            val icon = if (status == "completed") Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (status == "completed") ui.success else ui.warning)
              Text(item.content, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    if (loading) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text("Loading last turn changes…", color = ui.textMuted)
      }
    }

    val list = if (scope == ReviewScope.Session) fullDiff else visibleDiff
    if (list.isEmpty() && !loading) {
      EmptyState(title = "No changes", body = "No files were changed in this scope.")
      return@Column
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(list, key = { "${it.file}:${it.after.hashCode()}:${it.before.hashCode()}" }) { item ->
        DiffCard(item)
      }
    }
  }
}

@Composable
private fun DiffCard(diff: FileDiffDto) {
  val ui = LocalOpenCodeColors.current
  var open by rememberSaveable(diff.file) { mutableStateOf(false) }
  val ext = diff.file.substringAfterLast('.', missingDelimiterValue = "")
  val icon = fileIcon(ext, isDirectory = false)
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.panel),
    border = BorderStroke(1.dp, ui.borderSubtle),
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { open = !open }
          .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = ui.info)
        Column(modifier = Modifier.weight(1f)) {
          Text(diff.file, maxLines = 1, overflow = TextOverflow.Ellipsis)
          val status = diff.status ?: "modified"
          Text(
            "$status (+${diff.additions} -${diff.deletions})",
            style = MaterialTheme.typography.labelSmall,
            color = ui.textMuted,
          )
        }
        Icon(if (open) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight, contentDescription = null)
      }
      AnimatedVisibility(visible = open) {
        DiffView(before = diff.before, after = diff.after)
      }
    }
  }
}

@Composable
private fun DiffView(before: String, after: String) {
  val ui = LocalOpenCodeColors.current
  val lines = remember(before, after) { diffRows(before, after) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(ui.diffContextBg)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp),
  ) {
    lines.take(700).forEach { line ->
      val bg = when (line.kind) {
        DiffKind.Added -> ui.diffAddedBg
        DiffKind.Removed -> ui.diffRemovedBg
        DiffKind.Context -> Color.Transparent
      }
      val fg = when (line.kind) {
        DiffKind.Added -> ui.diffAdded
        DiffKind.Removed -> ui.diffRemoved
        DiffKind.Context -> ui.markdownText
      }
      Text(
        text = "${line.prefix} ${line.value}",
        modifier = Modifier
          .fillMaxWidth()
          .background(bg)
          .padding(horizontal = 4.dp, vertical = 1.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = fg,
      )
    }
    if (lines.size > 700) {
      Text("Diff truncated for mobile performance", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
    }
  }
}

@Composable
private fun FilesPanel(
  mode: FilesMode,
  onMode: (FilesMode) -> Unit,
  fileTree: Map<String, List<FileNodeDto>>,
  expanded: Set<String>,
  changed: List<FileStatusDto>,
  onToggleDirectory: (String) -> Unit,
  onOpenFile: (String) -> Unit,
) {
  val root = fileTree["/"].orEmpty()
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SessionTabButton(active = mode == FilesMode.Changes, label = "Changes") { onMode(FilesMode.Changes) }
      SessionTabButton(active = mode == FilesMode.All, label = "All files") { onMode(FilesMode.All) }
    }

    if (mode == FilesMode.Changes) {
      if (changed.isEmpty()) {
        EmptyState(title = "No changed files", body = "Git status is clean.")
        return@Column
      }
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(changed, key = { it.path }) { item ->
          val icon = fileIcon(item.path.substringAfterLast('.', missingDelimiterValue = ""), isDirectory = false)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenFile(item.path) },
            border = BorderStroke(1.dp, LocalOpenCodeColors.current.borderSubtle),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = LocalOpenCodeColors.current.info)
              Column(modifier = Modifier.weight(1f)) {
                Text(item.path, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.status} (+${item.added} -${item.removed})", style = MaterialTheme.typography.labelSmall)
              }
              Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
          }
        }
      }
      return@Column
    }

    if (root.isEmpty()) {
      EmptyState(title = "File tree empty", body = "Open a directory to load files.")
      return@Column
    }

    val rows = remember(fileTree, expanded) { flattenTree(fileTree, expanded) }
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(rows, key = { it.path + ":" + it.depth }) { row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (row.directory) onToggleDirectory(row.path) else onOpenFile(row.path)
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Spacer(Modifier.width((row.depth * 14).dp))
          if (row.directory) {
            Icon(
              if (row.expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = LocalOpenCodeColors.current.textMuted,
            )
          } else {
            Spacer(Modifier.width(16.dp))
          }
          Icon(
            imageVector = fileIcon(row.ext, row.directory, row.expanded),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (row.directory) LocalOpenCodeColors.current.info else LocalOpenCodeColors.current.textMuted,
          )
          Text(
            row.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProjectTerminalSheet(
  project: String,
  connected: Boolean,
  output: String,
  onReconnect: () -> Unit,
  onRunCommand: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      TerminalPanel(
        title = "Project terminal",
        subtitle = project,
        connected = connected,
        output = output,
        onReconnect = onReconnect,
        onRunCommand = onRunCommand,
      )
    }
  }
}

@Composable
private fun TerminalPanel(
  title: String,
  subtitle: String,
  connected: Boolean,
  output: String,
  onReconnect: () -> Unit,
  onRunCommand: (String) -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  var command by rememberSaveable(subtitle) { mutableStateOf("") }
  val scroll = rememberScrollState()

  LaunchedEffect(output.length) {
    scroll.scrollTo(scroll.maxValue)
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = ui.panel),
    border = BorderStroke(1.dp, ui.borderSubtle),
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ui.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusDot(status = if (connected) ServerProbe.Online else ServerProbe.Offline)
          Text(if (connected) "Connected" else "Disconnected", style = MaterialTheme.typography.labelSmall, color = ui.textMuted)
          OutlinedButton(onClick = onReconnect) {
            Text(if (connected) "Reconnect" else "Connect")
          }
        }
      }

      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = ui.diffContextBg),
        border = BorderStroke(1.dp, ui.borderSubtle),
      ) {
        val rendered = remember(output) { terminalOutput(output) }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(8.dp),
        ) {
          if (output.isBlank()) {
            Text(
              text = "Terminal output will appear here.",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = ui.textMuted,
            )
          } else {
            Text(
              text = rendered,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = ui.markdownCode,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = command,
          onValueChange = { command = it },
          singleLine = true,
          label = { Text("Run command") },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = {
            val next = command.trim()
            if (next.isEmpty()) return@Button
            onRunCommand(next)
            command = ""
          },
          enabled = command.isNotBlank(),
        ) {
          Text("Run")
        }
      }
    }
  }
}

@Composable
private fun PromptDock(
  value: String,
  onValue: (String) -> Unit,
  attachments: List<PromptAttachment>,
  onAttach: () -> Unit,
  onRemoveAttachment: (Int) -> Unit,
  providers: ProviderListDto?,
  agents: List<AgentDto>,
  selectedModel: ModelRef?,
  selectedAgent: String?,
  selectedVariant: String?,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
  canAbort: Boolean,
  onSend: () -> Unit,
  onAbort: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  val modelOptions = remember(providers) {
    if (providers == null) return@remember emptyList()
    providers.connected.flatMap { providerId ->
      val provider = providers.all.firstOrNull { it.id == providerId } ?: return@flatMap emptyList()
      provider.models.values.sortedBy { it.id }.map { model ->
        ModelItem(
          ref = ModelRef(providerId = provider.id, modelId = model.id),
          label = "${provider.name} / ${model.name}",
          variants = model.variants?.keys?.sorted().orEmpty(),
        )
      }
    }
  }
  val variants = modelOptions.firstOrNull { it.ref == selectedModel }?.variants.orEmpty()

  Surface(
    color = ui.panel,
    tonalElevation = 1.dp,
    shadowElevation = 4.dp,
    border = BorderStroke(1.dp, ui.borderSubtle),
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (attachments.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          attachments.forEachIndexed { index, item ->
            AttachmentPreviewCard(
              attachment = item,
              onRemove = { onRemoveAttachment(index) },
            )
          }
        }
      }

      SelectorRow(
        models = modelOptions,
        agents = agents,
        variants = variants,
        selectedModel = selectedModel,
        selectedAgent = selectedAgent,
        selectedVariant = selectedVariant,
        onSelectModel = onSelectModel,
        onSelectAgent = onSelectAgent,
        onSelectVariant = onSelectVariant,
        canAbort = canAbort,
        onAbort = onAbort,
      )

      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
          value = value,
          onValueChange = onValue,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
          ),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp, max = 320.dp),
          label = { Text("Ask OpenCode") },
          minLines = 1,
          maxLines = 14,
        )
        Row(
          modifier = Modifier.padding(bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
          IconButton(
            onClick = onAttach,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(Icons.Rounded.AttachFile, contentDescription = "Attach file", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() || attachments.isNotEmpty(),
            modifier = Modifier.size(36.dp),
          ) {
            Icon(Icons.Rounded.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentPreviewCard(
  attachment: PromptAttachment,
  onRemove: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  val bitmap = remember(attachment.dataUrl) {
    if (!attachment.mime.startsWith("image/")) return@remember null
    val encoded = attachment.dataUrl.substringAfter(",", missingDelimiterValue = "")
    if (encoded.isBlank()) return@remember null
    runCatching {
      val bytes = Base64.decode(encoded, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
  }
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.element),
    border = BorderStroke(1.dp, ui.borderSubtle),
    modifier = Modifier.size(width = 98.dp, height = 74.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = attachment.filename,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = if (attachment.mime == "application/pdf") Icons.Rounded.Description else Icons.Rounded.AttachFile,
            contentDescription = null,
            tint = ui.info,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.height(4.dp))
          Text(
            attachment.filename,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
      IconButton(
        onClick = onRemove,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(22.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Cancel,
          contentDescription = "Remove attachment",
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

@Composable
private fun SelectorRow(
  models: List<ModelItem>,
  agents: List<AgentDto>,
  variants: List<String>,
  selectedModel: ModelRef?,
  selectedAgent: String?,
  selectedVariant: String?,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
  canAbort: Boolean,
  onAbort: () -> Unit,
) {
  var modelOpen by remember { mutableStateOf(false) }
  var agentOpen by remember { mutableStateOf(false) }
  var variantOpen by remember { mutableStateOf(false) }

  val modelLabel = models.firstOrNull { it.ref == selectedModel }?.label ?: "Server model"
  val agentLabel = selectedAgent ?: "Server agent"
  val variantLabel = selectedVariant ?: "Auto"

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box {
      AssistChip(onClick = { modelOpen = true }, label = { Text(shorten(modelLabel)) })
      DropdownMenu(expanded = modelOpen, onDismissRequest = { modelOpen = false }) {
        DropdownMenuItem(
          text = { Text("Server default") },
          onClick = {
            onSelectModel(null)
            onSelectVariant(null)
            modelOpen = false
          },
        )
        models.forEach { item ->
          DropdownMenuItem(
            text = { Text(item.label) },
            onClick = {
              onSelectModel(item.ref)
              modelOpen = false
            },
          )
        }
      }
    }

    Box {
      AssistChip(onClick = { agentOpen = true }, label = { Text(shorten(agentLabel)) })
      DropdownMenu(expanded = agentOpen, onDismissRequest = { agentOpen = false }) {
        DropdownMenuItem(
          text = { Text("Server default") },
          onClick = {
            onSelectAgent(null)
            agentOpen = false
          },
        )
        agents.forEach { item ->
          DropdownMenuItem(
            text = { Text(item.name) },
            onClick = {
              onSelectAgent(item.name)
              agentOpen = false
            },
          )
        }
      }
    }

    Box {
      AssistChip(onClick = { variantOpen = true }, label = { Text("Think: ${shorten(variantLabel)}") })
      DropdownMenu(expanded = variantOpen, onDismissRequest = { variantOpen = false }) {
        DropdownMenuItem(
          text = { Text("Auto") },
          onClick = {
            onSelectVariant(null)
            variantOpen = false
          },
        )
        variants.forEach { item ->
          DropdownMenuItem(
            text = { Text(item) },
            onClick = {
              onSelectVariant(item)
              variantOpen = false
            },
          )
        }
      }
    }

    Spacer(Modifier.weight(1f))
    FilledTonalIconButton(
      onClick = onAbort,
      enabled = canAbort,
      colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = LocalOpenCodeColors.current.textMuted,
      ),
    ) {
      Icon(Icons.Rounded.Cancel, contentDescription = "Abort running task")
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsSheet(
  theme: String,
  themeMode: ThemeMode,
  themes: List<String>,
  hiddenProjects: Set<String>,
  onSetTheme: (String) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onUnhideProject: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

      Card {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Theme mode", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionTabButton(active = themeMode == ThemeMode.System, label = "System") { onSetThemeMode(ThemeMode.System) }
            SessionTabButton(active = themeMode == ThemeMode.Dark, label = "Dark") { onSetThemeMode(ThemeMode.Dark) }
            SessionTabButton(active = themeMode == ThemeMode.Light, label = "Light") { onSetThemeMode(ThemeMode.Light) }
          }
        }
      }

      Card(modifier = Modifier.fillMaxHeight(0.46f)) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Theme", style = MaterialTheme.typography.titleSmall)
          LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(themes, key = { it }) { item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSetTheme(item) }
                  .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(item)
                if (item == theme) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      }

      if (hiddenProjects.isNotEmpty()) {
        Card {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text("Hidden projects", style = MaterialTheme.typography.titleSmall)
            hiddenProjects.sorted().forEach { path ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(path, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { onUnhideProject(path) }) {
                  Text("Restore")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContextDialog(metrics: SessionMetrics, onDismiss: () -> Unit) {
  val item = metrics.context
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Context details") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (item == null) {
          Text("No context usage yet")
        } else {
          Text("Provider: ${item.provider}")
          Text("Model: ${item.model}")
          Text("Total tokens: ${item.total}")
          Text("Input tokens: ${item.input}")
          Text("Output tokens: ${item.output}")
          Text("Reasoning tokens: ${item.reasoning}")
          Text("Cache read/write: ${item.cacheRead} / ${item.cacheWrite}")
          Text("Context limit: ${item.limit ?: "unknown"}")
          Text("Usage: ${item.usage}%")
        }
        Text("Total cost: ${"%.4f".format(metrics.totalCost)} USD")
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
private fun FilePreviewDialog(
  path: String,
  file: FileContentDto?,
  loading: Boolean,
  onClose: () -> Unit,
) {
  val ui = LocalOpenCodeColors.current
  AlertDialog(
    onDismissRequest = onClose,
    confirmButton = {
      TextButton(onClick = onClose) {
        Text("Close")
      }
    },
    title = {
      Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis)
    },
    text = {
      Box(modifier = Modifier.fillMaxWidth()) {
        if (loading) {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
          return@Box
        }
        if (file == null) {
          Text("Unable to load file", color = ui.textMuted)
          return@Box
        }
        if (file.type == "binary" && file.mimeType?.startsWith("image/") == true && file.encoding == "base64") {
          AsyncImage(
            model = "data:${file.mimeType};base64,${file.content}",
            contentDescription = path,
            modifier = Modifier
              .fillMaxWidth()
              .height(320.dp),
          )
          return@Box
        }
        if (file.patch != null) {
          val patchText = buildString {
            file.patch.hunks.forEach { hunk ->
              append("@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@\n")
              hunk.lines.forEach { append(it).append('\n') }
            }
          }
          CodeBlock(patchText)
          return@Box
        }
        val text = if (file.type == "text") file.content else "[binary file]"
        CodeBlock(text.take(14000))
      }
    },
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AllSessionsSheet(
  project: String,
  sessions: List<SessionDto>,
  active: String?,
  onOpen: (String) -> Unit,
  onDelete: (String) -> Unit,
  onArchive: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("All sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Text(project, style = MaterialTheme.typography.labelSmall, color = LocalOpenCodeColors.current.textMuted)
      if (sessions.isEmpty()) {
        Text("No sessions")
      }
      LazyColumn(
        modifier = Modifier.fillMaxHeight(0.72f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(sessions, key = { it.id }) { item ->
          val title = item.title.ifBlank { item.slug }
          Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, if (active == item.id) LocalOpenCodeColors.current.borderActive else LocalOpenCodeColors.current.borderSubtle),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpen(item.id) }) { Text("Open") }
                OutlinedButton(onClick = { onArchive(item.id) }) { Text("Archive") }
                OutlinedButton(onClick = { onDelete(item.id) }) { Text("Delete") }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyState(
  title: String,
  body: String,
  action: (() -> Unit)? = null,
  actionLabel: String = "",
) {
  val ui = LocalOpenCodeColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = ui.panel),
    border = BorderStroke(1.dp, ui.borderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(body, color = ui.textMuted)
      if (action != null && actionLabel.isNotBlank()) {
        Button(onClick = action) {
          Text(actionLabel)
        }
      }
    }
  }
}

private data class ModelItem(
  val ref: ModelRef,
  val label: String,
  val variants: List<String>,
)

private data class SessionMetrics(
  val totalCost: Double,
  val context: ContextMetrics?,
  val usage: Int,
)

private data class ContextMetrics(
  val provider: String,
  val model: String,
  val limit: Int?,
  val input: Int,
  val output: Int,
  val reasoning: Int,
  val cacheRead: Int,
  val cacheWrite: Int,
  val total: Int,
  val usage: Int,
)

private sealed interface MarkdownBlock {
  data class Heading(val value: String) : MarkdownBlock
  data class Quote(val value: String) : MarkdownBlock
  data class ListItem(val value: String) : MarkdownBlock
  data class Code(val value: String) : MarkdownBlock
  data class Text(val value: String) : MarkdownBlock
}

private enum class DiffKind {
  Added,
  Removed,
  Context,
}

private data class DiffRow(
  val kind: DiffKind,
  val prefix: String,
  val value: String,
)

private data class FileRow(
  val path: String,
  val name: String,
  val ext: String,
  val depth: Int,
  val directory: Boolean,
  val expanded: Boolean,
)

private fun toolTitle(name: String): String {
  return when (name) {
    "bash" -> "Shell"
    "read" -> "Read"
    "write" -> "Write"
    "edit" -> "Edit"
    "grep" -> "Search"
    "glob" -> "Glob"
    "list" -> "List"
    "task" -> "Sub-agent"
    "apply_patch" -> "Patch"
    else -> name
  }
}

private fun assistantTitle(parts: List<PartDto>): String {
  val tool = parts.firstNotNullOfOrNull { part ->
    if (part.type != "tool") return@firstNotNullOfOrNull null
    val title = part.state?.title?.trim().orEmpty()
    if (title.isNotEmpty()) return@firstNotNullOfOrNull shorten(title.replace(Regex("\\s+"), " "), 46)
    val name = part.tool?.trim().orEmpty()
    if (name.isNotEmpty()) return@firstNotNullOfOrNull "${toolTitle(name)} step"
    null
  }
  if (tool != null) return tool

  val text = parts.firstNotNullOfOrNull { part ->
    if (part.type != "text" && part.type != "reasoning") return@firstNotNullOfOrNull null
    part.text
      ?.lineSequence()
      ?.map { it.trim() }
      ?.firstOrNull { it.isNotBlank() }
  }
  if (text != null) return shorten(text.replace(Regex("\\s+"), " "), 46)

  return "Assistant"
}

private fun isImplicitStep(type: String): Boolean {
  return type == "step-start" || type == "step-finish" || type == "step_start" || type == "step_finish"
}

private fun toolIcon(name: String?): androidx.compose.ui.graphics.vector.ImageVector {
  return when (name) {
    "read", "write", "edit", "apply_patch" -> Icons.Rounded.Code
    "bash" -> Icons.Rounded.Description
    "task" -> Icons.Rounded.MoreVert
    else -> Icons.Rounded.ChevronRight
  }
}

private fun fileIcon(
  ext: String,
  isDirectory: Boolean,
  expanded: Boolean = false,
): androidx.compose.ui.graphics.vector.ImageVector {
  if (isDirectory) return if (expanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder
  return when (ext.lowercase(Locale.getDefault())) {
    "kt", "ts", "tsx", "js", "jsx", "py", "go", "rs", "java", "c", "cpp", "swift", "rb", "php", "json", "yaml", "yml", "xml" -> Icons.Rounded.Code
    "png", "jpg", "jpeg", "gif", "webp", "svg" -> Icons.Rounded.Image
    "md", "txt", "log" -> Icons.Rounded.Description
    else -> Icons.Rounded.InsertDriveFile
  }
}

private fun parseMarkdown(input: String): List<MarkdownBlock> {
  val out = mutableListOf<MarkdownBlock>()
  val chunks = input.split("```")
  chunks.forEachIndexed { index, chunk ->
    if (index % 2 == 1) {
      val value = chunk.trim('\n')
      if (value.isNotBlank()) out += MarkdownBlock.Code(value)
      return@forEachIndexed
    }
    chunk.lines().forEach { raw ->
      val line = raw.trimEnd()
      if (line.isBlank()) return@forEach
      if (line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")) {
        out += MarkdownBlock.Heading(line.trimStart('#', ' '))
        return@forEach
      }
      if (line.startsWith("> ")) {
        out += MarkdownBlock.Quote(line.removePrefix("> "))
        return@forEach
      }
      if (line.startsWith("- ") || line.startsWith("* ")) {
        out += MarkdownBlock.ListItem(line.drop(2))
        return@forEach
      }
      out += MarkdownBlock.Text(line)
    }
  }
  return out
}

private fun diffRows(before: String, after: String): List<DiffRow> {
  val a = before.split("\n")
  val b = after.split("\n")
  val out = mutableListOf<DiffRow>()
  var i = 0
  var j = 0
  val window = 8

  while (i < a.size || j < b.size) {
    if (i < a.size && j < b.size && a[i] == b[j]) {
      out += DiffRow(DiffKind.Context, " ", a[i])
      i += 1
      j += 1
      continue
    }

    if (i >= a.size) {
      out += DiffRow(DiffKind.Added, "+", b[j])
      j += 1
      continue
    }
    if (j >= b.size) {
      out += DiffRow(DiffKind.Removed, "-", a[i])
      i += 1
      continue
    }

    val nextA = (1..window).firstOrNull { step -> i + step < a.size && a[i + step] == b[j] }
    val nextB = (1..window).firstOrNull { step -> j + step < b.size && b[j + step] == a[i] }

    if (nextA != null && (nextB == null || nextA <= nextB)) {
      out += DiffRow(DiffKind.Removed, "-", a[i])
      i += 1
      continue
    }
    if (nextB != null) {
      out += DiffRow(DiffKind.Added, "+", b[j])
      j += 1
      continue
    }

    out += DiffRow(DiffKind.Removed, "-", a[i])
    out += DiffRow(DiffKind.Added, "+", b[j])
    i += 1
    j += 1
  }

  return out
}

private data class TermStyle(
  val fg: Color? = null,
  val bg: Color? = null,
  val bold: Boolean = false,
  val dim: Boolean = false,
  val underline: Boolean = false,
)

private data class Glyph(
  val value: Char,
  val style: TermStyle,
)

private fun terminalOutput(raw: String): AnnotatedString {
  if (raw.isEmpty()) return AnnotatedString("")

  val base = TermStyle()
  val lines = mutableListOf<MutableList<Glyph>>(mutableListOf())
  var row = 0
  var col = 0
  var style = base
  var saved: Pair<Int, Int>? = null

  fun ensureRow(next: Int) {
    while (lines.size <= next) {
      lines.add(mutableListOf())
    }
  }

  fun fill(line: MutableList<Glyph>, limit: Int) {
    while (line.size < limit) {
      line += Glyph(value = ' ', style = base)
    }
  }

  fun put(value: Char) {
    ensureRow(row)
    val line = lines[row]
    fill(line, col)
    if (col < line.size) {
      line[col] = Glyph(value = value, style = style)
    } else {
      line += Glyph(value = value, style = style)
    }
    col += 1
  }

  fun eraseLine(mode: Int) {
    ensureRow(row)
    val line = lines[row]
    if (mode == 2) {
      line.clear()
      col = 0
      return
    }
    if (mode == 1) {
      if (line.isEmpty()) return
      val end = minOf(col, line.lastIndex)
      for (index in 0..end) {
        line[index] = Glyph(value = ' ', style = base)
      }
      return
    }
    if (col >= line.size) return
    line.subList(col, line.size).clear()
  }

  fun eraseScreen(mode: Int) {
    if (mode == 2) {
      lines.clear()
      lines.add(mutableListOf())
      row = 0
      col = 0
      return
    }
    ensureRow(row)
    eraseLine(0)
    if (row + 1 >= lines.size) return
    lines.subList(row + 1, lines.size).clear()
  }

  fun parse(param: String): List<Int?> {
    if (param.isEmpty()) return emptyList()
    return param.split(';').map { item ->
      if (item.isBlank()) return@map null
      item.trimStart('?').toIntOrNull()
    }
  }

  fun color(code: Int): Color {
    if (code < 16) {
      val list = listOf(
        "#000000",
        "#800000",
        "#008000",
        "#808000",
        "#000080",
        "#800080",
        "#008080",
        "#c0c0c0",
        "#808080",
        "#ff0000",
        "#00ff00",
        "#ffff00",
        "#0000ff",
        "#ff00ff",
        "#00ffff",
        "#ffffff",
      )
      return hexColor(list.getOrNull(code) ?: "#000000")
    }
    if (code < 232) {
      val index = code - 16
      val b = index % 6
      val g = (index / 6) % 6
      val r = index / 36
      val unit = { x: Int -> if (x == 0) 0 else x * 40 + 55 }
      return Color(unit(r), unit(g), unit(b))
    }
    val gray = ((code - 232) * 10 + 8).coerceIn(0, 255)
    return Color(gray, gray, gray)
  }

  fun applySgr(list: List<Int?>) {
    if (list.isEmpty()) {
      style = base
      return
    }
    var index = 0
    while (index < list.size) {
      val value = list[index] ?: 0
      if (value == 0) {
        style = base
        index += 1
        continue
      }
      if (value == 1) {
        style = style.copy(bold = true)
        index += 1
        continue
      }
      if (value == 2) {
        style = style.copy(dim = true)
        index += 1
        continue
      }
      if (value == 4) {
        style = style.copy(underline = true)
        index += 1
        continue
      }
      if (value == 22) {
        style = style.copy(bold = false, dim = false)
        index += 1
        continue
      }
      if (value == 24) {
        style = style.copy(underline = false)
        index += 1
        continue
      }
      if (value in 30..37) {
        style = style.copy(fg = color(value - 30))
        index += 1
        continue
      }
      if (value == 39) {
        style = style.copy(fg = null)
        index += 1
        continue
      }
      if (value in 90..97) {
        style = style.copy(fg = color(value - 90 + 8))
        index += 1
        continue
      }
      if (value in 40..47) {
        style = style.copy(bg = color(value - 40))
        index += 1
        continue
      }
      if (value == 49) {
        style = style.copy(bg = null)
        index += 1
        continue
      }
      if (value in 100..107) {
        style = style.copy(bg = color(value - 100 + 8))
        index += 1
        continue
      }
      if (value == 38 || value == 48) {
        val targetFg = value == 38
        val mode = list.getOrNull(index + 1)
        if (mode == 5) {
          val next = list.getOrNull(index + 2)
          if (next != null) {
            val mapped = color(next.coerceIn(0, 255))
            style = if (targetFg) style.copy(fg = mapped) else style.copy(bg = mapped)
          }
          index += 3
          continue
        }
        if (mode == 2) {
          val r = list.getOrNull(index + 2) ?: 0
          val g = list.getOrNull(index + 3) ?: 0
          val b = list.getOrNull(index + 4) ?: 0
          val mapped = Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
          style = if (targetFg) style.copy(fg = mapped) else style.copy(bg = mapped)
          index += 5
          continue
        }
      }
      index += 1
    }
  }

  var index = 0
  while (index < raw.length) {
    val value = raw[index]

    if (value == '\u001B') {
      if (index + 1 >= raw.length) break
      val marker = raw[index + 1]

      if (marker == ']') {
        var end = index + 2
        while (end < raw.length) {
          val cur = raw[end]
          if (cur == '\u0007') {
            end += 1
            break
          }
          if (cur == '\u001B' && end + 1 < raw.length && raw[end + 1] == '\\') {
            end += 2
            break
          }
          end += 1
        }
        index = end
        continue
      }

      if (marker == '[') {
        var end = index + 2
        while (end < raw.length && raw[end] !in '@'..'~') {
          end += 1
        }
        if (end >= raw.length) break

        val cmd = raw[end]
        val list = parse(raw.substring(index + 2, end))
        val first = list.firstOrNull() ?: 1

        if (cmd == 'm') applySgr(list)
        if (cmd == 'K') eraseLine((list.firstOrNull() ?: 0))
        if (cmd == 'J') eraseScreen((list.firstOrNull() ?: 0))
        if (cmd == 'A') row = maxOf(0, row - maxOf(first, 1))
        if (cmd == 'B') {
          row += maxOf(first, 1)
          ensureRow(row)
        }
        if (cmd == 'C') col += maxOf(first, 1)
        if (cmd == 'D') col = maxOf(0, col - maxOf(first, 1))
        if (cmd == 'G') col = maxOf(0, first - 1)
        if (cmd == 'H' || cmd == 'f') {
          row = maxOf((list.getOrNull(0) ?: 1) - 1, 0)
          col = maxOf((list.getOrNull(1) ?: 1) - 1, 0)
          ensureRow(row)
        }
        if (cmd == 's') saved = row to col
        if (cmd == 'u') {
          val next = saved
          if (next != null) {
            row = next.first
            col = next.second
            ensureRow(row)
          }
        }

        index = end + 1
        continue
      }

      if (marker == '7') {
        saved = row to col
        index += 2
        continue
      }
      if (marker == '8') {
        val next = saved
        if (next != null) {
          row = next.first
          col = next.second
          ensureRow(row)
        }
        index += 2
        continue
      }

      index += 2
      continue
    }

    if (value == '\n') {
      row += 1
      col = 0
      ensureRow(row)
      index += 1
      continue
    }
    if (value == '\r') {
      col = 0
      index += 1
      continue
    }
    if (value == '\b') {
      col = maxOf(0, col - 1)
      index += 1
      continue
    }
    if (value == '\t') {
      val stop = ((col / 8) + 1) * 8
      while (col < stop) {
        put(' ')
      }
      index += 1
      continue
    }
    if (value.code < 32) {
      index += 1
      continue
    }

    put(value)
    index += 1
  }

  fun span(item: TermStyle): SpanStyle {
    val color = if (item.dim) item.fg?.copy(alpha = 0.72f) else item.fg
    return SpanStyle(
      color = color ?: Color.Unspecified,
      background = item.bg ?: Color.Unspecified,
      fontWeight = if (item.bold) FontWeight.SemiBold else null,
      textDecoration = if (item.underline) TextDecoration.Underline else null,
    )
  }

  val output = buildAnnotatedString {
    var run: TermStyle? = null
    var start = 0

    fun flush(limit: Int) {
      val item = run ?: return
      if (item == base) return
      addStyle(span(item), start, limit)
    }

    lines.forEachIndexed { lineIndex, line ->
      line.forEach { item ->
        if (run == null) {
          run = item.style
          start = length
        }
        if (run != null && run != item.style) {
          flush(length)
          run = item.style
          start = length
        }
        append(item.value)
      }

      if (lineIndex >= lines.lastIndex) return@forEachIndexed
      flush(length)
      run = null
      append('\n')
    }

    flush(length)
  }

  return output
}

private fun hexColor(input: String): Color {
  val value = input.removePrefix("#")
  if (value.length != 6) return Color.Unspecified
  val r = value.substring(0, 2).toIntOrNull(16) ?: return Color.Unspecified
  val g = value.substring(2, 4).toIntOrNull(16) ?: return Color.Unspecified
  val b = value.substring(4, 6).toIntOrNull(16) ?: return Color.Unspecified
  return Color(r, g, b)
}

private fun flattenTree(fileTree: Map<String, List<FileNodeDto>>, expanded: Set<String>): List<FileRow> {
  val rows = mutableListOf<FileRow>()

  fun walk(path: String, depth: Int) {
    val list = fileTree[path].orEmpty().sortedWith(compareBy<FileNodeDto> { !isDirectory(it) }.thenBy { it.name.lowercase() })
    list.forEach { node ->
      val dir = isDirectory(node)
      val open = dir && node.path in expanded
      rows += FileRow(
        path = node.path,
        name = node.name,
        ext = node.name.substringAfterLast('.', missingDelimiterValue = ""),
        depth = depth,
        directory = dir,
        expanded = open,
      )
      if (dir && open) {
        walk(node.path, depth + 1)
      }
    }
  }

  walk("/", 0)
  return rows
}

private fun isDirectory(item: FileNodeDto): Boolean {
  val type = item.type.lowercase(Locale.getDefault())
  return type == "dir" || type == "directory" || type == "folder"
}

private fun shorten(value: String, limit: Int = 22): String {
  if (value.length <= limit) return value
  return value.take(limit - 1) + "…"
}

private fun prettyJson(value: JsonObject?): String {
  if (value == null) return ""
  return runCatching {
    Json {
      prettyPrint = true
      prettyPrintIndent = "  "
      explicitNulls = false
      encodeDefaults = false
    }.encodeToString(JsonObject.serializer(), value)
  }.getOrElse { value.toString() }
}

private fun sessionMetrics(messages: List<MessageDto>, providers: ProviderListDto?): SessionMetrics {
  val totalCost = messages.filter { it.role == "assistant" }.sumOf { it.cost ?: 0.0 }
  val assistant = messages
    .asReversed()
    .firstOrNull {
      if (it.role != "assistant") return@firstOrNull false
      val total = it.tokens?.total ?: it.tokens?.let { t -> t.input + t.output + t.reasoning + t.cache.read + t.cache.write } ?: 0
      total > 0
    }

  if (assistant == null) {
    return SessionMetrics(totalCost = totalCost, context = null, usage = 0)
  }

  val providerId = assistant.providerID ?: assistant.model?.providerId
  val modelId = assistant.modelID ?: assistant.model?.modelId
  val provider = providers?.all?.firstOrNull { it.id == providerId }
  val model = if (modelId == null) null else provider?.models?.get(modelId)
  val limit = model?.limit?.context

  val input = assistant.tokens?.input ?: 0
  val output = assistant.tokens?.output ?: 0
  val reasoning = assistant.tokens?.reasoning ?: 0
  val cacheRead = assistant.tokens?.cache?.read ?: 0
  val cacheWrite = assistant.tokens?.cache?.write ?: 0
  val total = assistant.tokens?.total ?: (input + output + reasoning + cacheRead + cacheWrite)
  val usage = if (limit == null || limit <= 0) 0 else ((total.toFloat() / limit.toFloat()) * 100f).toInt().coerceIn(0, 100)

  return SessionMetrics(
    totalCost = totalCost,
    usage = usage,
    context = ContextMetrics(
      provider = provider?.name ?: providerId.orEmpty(),
      model = model?.name ?: modelId.orEmpty(),
      limit = limit,
      input = input,
      output = output,
      reasoning = reasoning,
      cacheRead = cacheRead,
      cacheWrite = cacheWrite,
      total = total,
      usage = usage,
    ),
  )
}

private fun timeStamp(epoch: Long): String {
  return runCatching {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))
  }.getOrElse { "" }
}

private val ACCEPTED_ATTACHMENT_TYPES = arrayOf(
  "image/png",
  "image/jpeg",
  "image/gif",
  "image/webp",
  "application/pdf",
)

private fun loadAttachment(context: Context, uri: Uri): PromptAttachment? {
  val mime = context.contentResolver.getType(uri) ?: return null
  if (mime !in ACCEPTED_ATTACHMENT_TYPES) return null
  val stream = context.contentResolver.openInputStream(uri) ?: return null
  val bytes = stream.use { it.readBytes() }
  if (bytes.isEmpty()) return null
  val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
  val filename = queryDisplayName(context, uri) ?: "attachment"
  return PromptAttachment(
    mime = mime,
    filename = filename,
    dataUrl = "data:$mime;base64,$encoded",
  )
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
  val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
  cursor.use {
    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (index == -1) return null
    if (!it.moveToFirst()) return null
    return it.getString(index)
  }
}
