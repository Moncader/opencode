package ai.opencode.android.ui

import ai.opencode.android.core.model.AgentDto
import ai.opencode.android.core.model.FileContentDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.FileNodeDto
import ai.opencode.android.core.model.MessageDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.model.PartDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.ProviderListDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.TodoDto
import ai.opencode.android.core.network.PromptAttachment
import android.content.Context
import android.provider.OpenableColumns
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  state: MainState,
  onConnect: (String) -> Unit,
  onSelectServer: (String) -> Unit,
  onRemoveServer: (String) -> Unit,
  onOpenProject: (String) -> Unit,
  onOpenSession: (String) -> Unit,
  onSendPrompt: (String, List<PromptAttachment>) -> Unit,
  onRunShell: (String) -> Unit,
  onTerminalConnect: () -> Unit,
  onTerminalClose: () -> Unit,
  onTerminalSendRaw: (String) -> Unit,
  onAbort: () -> Unit,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
  onSelectSessionTab: (SessionTab) -> Unit,
  onOpenPath: (String, Boolean) -> Unit,
  onParentPath: () -> Unit,
  onCloseSession: () -> Unit,
  onRefresh: () -> Unit,
) {
  var input by remember(state.serverInput) { mutableStateOf(state.serverInput) }
  var prompt by remember { mutableStateOf("") }
  val attachments = remember { mutableStateListOf<PromptAttachment>() }
  val context = LocalContext.current
  val picker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
    val next = uri?.let { loadAttachment(context, it) } ?: return@rememberLauncherForActivityResult
    attachments.add(next)
  }
  val dir = state.selectedProject
  val sid = state.repo.activeSessionId
  val sessionOpen = state.repo.connected && dir != null && sid != null

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("OpenCode", fontWeight = FontWeight.Bold)
            Text(
              text = state.repo.activeServer ?: "No server",
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          if (sessionOpen) {
            TextButton(onClick = onCloseSession) {
              Text("Sessions")
            }
          }
          IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "refresh")
          }
        },
      )
    },
    bottomBar = {
      if (sessionOpen && state.sessionTab == SessionTab.Chat) {
        PromptDock(
          value = prompt,
          onChange = { prompt = it },
          onSend = {
            onSendPrompt(prompt, attachments.toList())
            prompt = ""
            attachments.clear()
          },
          attachments = attachments,
          onAttach = { picker.launch(ACCEPTED_ATTACHMENT_TYPES) },
          onRemoveAttachment = { index ->
            if (index in attachments.indices) attachments.removeAt(index)
          },
          onAbort = onAbort,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    },
  ) { pad ->
    if (sessionOpen) {
      SessionBody(
        modifier = Modifier
          .fillMaxSize()
          .padding(pad)
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 14.dp, vertical = 10.dp),
        directory = dir,
        messages = state.repo.sync.messagesBySession[sid].orEmpty(),
        parts = state.repo.sync.partsByMessage,
        permissions = state.repo.sync.permissionBySession[sid].orEmpty(),
        questions = state.repo.sync.questionBySession[sid].orEmpty(),
        providers = state.repo.providerList,
        agents = state.repo.agentByDirectory[dir].orEmpty(),
        selectedModel = state.repo.selectedModel,
        selectedAgent = state.repo.selectedAgent,
        selectedVariant = state.repo.selectedVariant,
        sessionTab = state.sessionTab,
        todos = state.repo.sync.todoBySession[sid].orEmpty(),
        diffs = state.repo.sync.sessionDiffBySession[sid].orEmpty(),
        filePath = state.filePath,
        files = state.fileNodes,
        fileContent = state.fileContent,
        fileLoading = state.fileLoading,
        terminalConnected = state.repo.terminalConnected,
        terminalOutput = state.repo.terminalOutput,
        onReplyPermission = onReplyPermission,
        onReplyQuestion = onReplyQuestion,
        onRejectQuestion = onRejectQuestion,
        onSelectModel = onSelectModel,
        onSelectAgent = onSelectAgent,
        onSelectVariant = onSelectVariant,
        onSelectSessionTab = onSelectSessionTab,
        onOpenPath = onOpenPath,
        onParentPath = onParentPath,
        onRunShell = onRunShell,
        onTerminalConnect = onTerminalConnect,
        onTerminalClose = onTerminalClose,
        onTerminalSendRaw = onTerminalSendRaw,
      )
      return@Scaffold
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(pad)
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        ServerCard(
          input = input,
          loading = state.repo.loading,
          servers = state.repo.servers.list.map { it.url },
          active = state.repo.activeServer,
          onChange = { input = it },
          onConnect = { onConnect(input) },
          onSelectServer = onSelectServer,
          onRemoveServer = onRemoveServer,
        )
      }

      if (state.repo.connected) {
        item {
          ProjectCard(
            projects = state.repo.sync.projects,
            selected = state.selectedProject,
            onOpen = onOpenProject,
          )
        }

        val dir = state.selectedProject
        if (dir != null) {
          item {
            SessionCard(
              sessions = state.repo.sync.sessionsByDirectory[dir].orEmpty(),
              active = state.repo.activeSessionId,
              onOpen = onOpenSession,
            )
          }
        }
      }

      val err = state.repo.error
      if (err != null) {
        item {
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
              text = err,
              modifier = Modifier.padding(12.dp),
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServerCard(
  input: String,
  loading: Boolean,
  servers: List<String>,
  active: String?,
  onChange: (String) -> Unit,
  onConnect: () -> Unit,
  onSelectServer: (String) -> Unit,
  onRemoveServer: (String) -> Unit,
) {
  Card {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Server", style = MaterialTheme.typography.titleMedium)
      OutlinedTextField(
        value = input,
        onValueChange = onChange,
        singleLine = true,
        label = { Text("URL") },
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onConnect, enabled = !loading && input.isNotBlank()) {
          Text("Connect")
        }
        if (loading) {
          CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
        }
      }
      if (servers.isNotEmpty()) {
        HorizontalDivider()
        servers.forEach { url ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = if (url == active) "$url (active)" else url,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row {
              TextButton(onClick = { onSelectServer(url) }) { Text("Use") }
              TextButton(onClick = { onRemoveServer(url) }) { Text("Remove") }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProjectCard(
  projects: List<ProjectDto>,
  selected: String?,
  onOpen: (String) -> Unit,
) {
  Card {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Projects", style = MaterialTheme.typography.titleMedium)
      if (projects.isEmpty()) {
        Text("No projects from server")
      }
      projects.forEach { item ->
        val label = item.name?.ifBlank { null } ?: item.worktree
        OutlinedButton(
          onClick = { onOpen(item.worktree) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = if (item.worktree == selected) "$label (open)" else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun SessionCard(
  sessions: List<SessionDto>,
  active: String?,
  onOpen: (String) -> Unit,
) {
  Card {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Sessions", style = MaterialTheme.typography.titleMedium)
      if (sessions.isEmpty()) {
        Text("No sessions")
      }
      sessions.sortedByDescending { it.time.updated }.forEach { item ->
        val title = item.title.ifBlank { item.slug }
        OutlinedButton(
          onClick = { onOpen(item.id) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = if (item.id == active) "$title (open)" else title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun SessionBody(
  modifier: Modifier = Modifier,
  directory: String,
  messages: List<MessageDto>,
  parts: Map<String, List<PartDto>>,
  todos: List<TodoDto>,
  diffs: List<FileDiffDto>,
  sessionTab: SessionTab,
  filePath: String,
  files: List<FileNodeDto>,
  fileContent: FileContentDto?,
  fileLoading: Boolean,
  terminalConnected: Boolean,
  terminalOutput: String,
  permissions: List<ai.opencode.android.core.model.PermissionRequestDto>,
  questions: List<ai.opencode.android.core.model.QuestionRequestDto>,
  providers: ProviderListDto?,
  agents: List<AgentDto>,
  selectedModel: ModelRef?,
  selectedAgent: String?,
  selectedVariant: String?,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
  onSelectSessionTab: (SessionTab) -> Unit,
  onOpenPath: (String, Boolean) -> Unit,
  onParentPath: () -> Unit,
  onRunShell: (String) -> Unit,
  onTerminalConnect: () -> Unit,
  onTerminalClose: () -> Unit,
  onTerminalSendRaw: (String) -> Unit,
) {
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
  val thinking = modelOptions.firstOrNull { it.ref == selectedModel }?.variants.orEmpty()

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = directory,
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    SessionSelectors(
      models = modelOptions,
      agents = agents,
      thinking = thinking,
      selectedModel = selectedModel,
      selectedAgent = selectedAgent,
      selectedVariant = selectedVariant,
      onSelectModel = onSelectModel,
      onSelectAgent = onSelectAgent,
      onSelectVariant = onSelectVariant,
    )

    SessionTabs(
      tab = sessionTab,
      onSelect = onSelectSessionTab,
    )

    if (sessionTab == SessionTab.Chat) {
      ContextUsageCard(messages = messages)

      AttentionCard(
        permissions = permissions,
        questions = questions,
        onReplyPermission = onReplyPermission,
        onReplyQuestion = onReplyQuestion,
        onRejectQuestion = onRejectQuestion,
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f, fill = true),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(
          items = messages.sortedByDescending { it.time.created },
          key = { it.id },
        ) { item ->
          MessageItem(
            message = item,
            parts = parts[item.id].orEmpty(),
          )
        }
      }
      return@Column
    }

    if (sessionTab == SessionTab.Review) {
      ReviewPanel(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f, fill = true),
        todos = todos,
        diffs = diffs,
      )
      return@Column
    }

    if (sessionTab == SessionTab.Terminal) {
      TerminalPanel(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f, fill = true),
        connected = terminalConnected,
        output = terminalOutput,
        onRun = onRunShell,
        onConnect = onTerminalConnect,
        onClose = onTerminalClose,
        onSendRaw = onTerminalSendRaw,
      )
      return@Column
    }

    FilesPanel(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f, fill = true),
      path = filePath,
      files = files,
      fileContent = fileContent,
      loading = fileLoading,
      onOpenPath = onOpenPath,
      onParentPath = onParentPath,
    )
  }
}

private data class ModelItem(
  val ref: ModelRef,
  val label: String,
  val variants: List<String>,
)

@Composable
private fun SessionSelectors(
  models: List<ModelItem>,
  agents: List<AgentDto>,
  thinking: List<String>,
  selectedModel: ModelRef?,
  selectedAgent: String?,
  selectedVariant: String?,
  onSelectModel: (ModelRef?) -> Unit,
  onSelectAgent: (String?) -> Unit,
  onSelectVariant: (String?) -> Unit,
) {
  var modelOpen by remember { mutableStateOf(false) }
  var agentOpen by remember { mutableStateOf(false) }
  var thinkingOpen by remember { mutableStateOf(false) }

  val modelLabel = models.firstOrNull { it.ref == selectedModel }?.label ?: "Server default model"
  val agentLabel = selectedAgent ?: "Server default agent"
  val thinkingLabel = selectedVariant ?: "Auto"

  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Column {
      OutlinedButton(onClick = { modelOpen = true }) {
        Text("Model: $modelLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
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

    Column {
      OutlinedButton(onClick = { agentOpen = true }) {
        Text("Agent: $agentLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
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
  }

  Column {
    OutlinedButton(onClick = { thinkingOpen = true }) {
      Text("Thinking: $thinkingLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    DropdownMenu(expanded = thinkingOpen, onDismissRequest = { thinkingOpen = false }) {
      DropdownMenuItem(
        text = { Text("Auto") },
        onClick = {
          onSelectVariant(null)
          thinkingOpen = false
        },
      )
      thinking.forEach { item ->
        DropdownMenuItem(
          text = { Text(item) },
          onClick = {
            onSelectVariant(item)
            thinkingOpen = false
          },
        )
      }
    }
  }
}

@Composable
private fun SessionTabs(
  tab: SessionTab,
  onSelect: (SessionTab) -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (tab == SessionTab.Chat) {
      Button(onClick = { onSelect(SessionTab.Chat) }) { Text("Chat") }
    } else {
      OutlinedButton(onClick = { onSelect(SessionTab.Chat) }) { Text("Chat") }
    }
    if (tab == SessionTab.Review) {
      Button(onClick = { onSelect(SessionTab.Review) }) { Text("Review") }
    } else {
      OutlinedButton(onClick = { onSelect(SessionTab.Review) }) { Text("Review") }
    }
    if (tab == SessionTab.Files) {
      Button(onClick = { onSelect(SessionTab.Files) }) { Text("Files") }
    } else {
      OutlinedButton(onClick = { onSelect(SessionTab.Files) }) { Text("Files") }
    }
    if (tab == SessionTab.Terminal) {
      Button(onClick = { onSelect(SessionTab.Terminal) }) { Text("Terminal") }
    } else {
      OutlinedButton(onClick = { onSelect(SessionTab.Terminal) }) { Text("Terminal") }
    }
  }
}

@Composable
private fun ContextUsageCard(messages: List<MessageDto>) {
  val assistant = messages.filter { it.role == "assistant" }
  val input = assistant.sumOf { it.tokens?.input ?: 0 }
  val output = assistant.sumOf { it.tokens?.output ?: 0 }
  val reasoning = assistant.sumOf { it.tokens?.reasoning ?: 0 }
  val total = assistant.sumOf { it.tokens?.total ?: 0 }
  val cost = assistant.sumOf { it.cost ?: 0.0 }

  Card {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("Context", style = MaterialTheme.typography.titleMedium)
      Text("input $input • output $output • reasoning $reasoning • total $total")
      Text("cost ${"%.4f".format(cost)}")
    }
  }
}

@Composable
private fun TerminalPanel(
  modifier: Modifier = Modifier,
  connected: Boolean,
  output: String,
  onRun: (String) -> Unit,
  onConnect: () -> Unit,
  onClose: () -> Unit,
  onSendRaw: (String) -> Unit,
) {
  var command by remember { mutableStateOf("") }
  var raw by remember { mutableStateOf("") }

  Card(modifier = modifier) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Terminal", style = MaterialTheme.typography.titleMedium)
      Text(if (connected) "Connected" else "Disconnected")
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (connected) {
          OutlinedButton(onClick = onClose) { Text("Disconnect") }
        } else {
          Button(onClick = onConnect) { Text("Connect") }
        }
      }
      OutlinedTextField(
        value = output,
        onValueChange = {},
        label = { Text("Output") },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = true),
        readOnly = true,
      )
      OutlinedTextField(
        value = command,
        onValueChange = { command = it },
        label = { Text("Command") },
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {
          onRun(command)
          command = ""
        },
        enabled = connected && command.isNotBlank(),
      ) {
        Text("Run")
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = raw,
          onValueChange = { raw = it },
          label = { Text("Raw input") },
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = {
            onSendRaw(raw)
            raw = ""
          },
          enabled = connected && raw.isNotBlank(),
        ) {
          Text("Send")
        }
      }
    }
  }
}

@Composable
private fun ReviewPanel(
  modifier: Modifier = Modifier,
  todos: List<TodoDto>,
  diffs: List<FileDiffDto>,
) {
  LazyColumn(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Todo", style = MaterialTheme.typography.titleMedium)
          if (todos.isEmpty()) {
            Text("No todo items")
          }
          todos.forEach { item ->
            Text("- [${item.status}] ${item.content}")
          }
        }
      }
    }

    item {
      Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Changes", style = MaterialTheme.typography.titleMedium)
          if (diffs.isEmpty()) {
            Text("No file changes")
          }
          diffs.forEach { item ->
            val status = item.status ?: "modified"
            Text("$status ${item.file} (+${item.additions} -${item.deletions})")
          }
        }
      }
    }
  }
}

@Composable
private fun FilesPanel(
  modifier: Modifier = Modifier,
  path: String,
  files: List<FileNodeDto>,
  fileContent: FileContentDto?,
  loading: Boolean,
  onOpenPath: (String, Boolean) -> Unit,
  onParentPath: () -> Unit,
) {
  LazyColumn(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onParentPath, enabled = path != "/") { Text("Up") }
            Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          if (loading) {
            CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
          }
          if (files.isEmpty() && !loading) {
            Text("No files")
          }
          files.sortedBy { it.path }.forEach { item ->
            val directory = isDirectory(item)
            val icon = if (directory) "[dir]" else "[file]"
            OutlinedButton(
              onClick = { onOpenPath(item.path, directory) },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("$icon ${item.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    if (fileContent != null) {
      item {
        Card {
          Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            Text(fileContent.content.take(5000))
          }
        }
      }
    }
  }
}

private fun isDirectory(item: FileNodeDto): Boolean {
  val type = item.type.lowercase()
  return type == "dir" || type == "directory" || type == "folder"
}

@Composable
private fun PromptDock(
  value: String,
  onChange: (String) -> Unit,
  onSend: () -> Unit,
  attachments: List<PromptAttachment>,
  onAttach: () -> Unit,
  onRemoveAttachment: (Int) -> Unit,
  onAbort: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (attachments.isNotEmpty()) {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(attachments.withIndex().toList(), key = { it.index }) { item ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = item.value.filename,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              TextButton(onClick = { onRemoveAttachment(item.index) }) {
                Text("Remove")
              }
            }
          }
        }
      }
      OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Ask OpenCode") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onAttach) {
          Text("Attach")
        }
        Button(onClick = onSend, enabled = value.isNotBlank() || attachments.isNotEmpty()) {
          Text("Send")
        }
        OutlinedButton(onClick = onAbort) {
          Text("Abort")
        }
      }
    }
  }
}

@Composable
private fun AttentionCard(
  permissions: List<ai.opencode.android.core.model.PermissionRequestDto>,
  questions: List<ai.opencode.android.core.model.QuestionRequestDto>,
  onReplyPermission: (String, String) -> Unit,
  onReplyQuestion: (String, List<List<String>>) -> Unit,
  onRejectQuestion: (String) -> Unit,
) {
  if (permissions.isEmpty() && questions.isEmpty()) return
  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Needs attention", style = MaterialTheme.typography.titleMedium)
      permissions.forEach { item ->
        Text("Permission: ${item.permission}")
        if (item.patterns.isNotEmpty()) {
          Text(item.patterns.joinToString(", "))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { onReplyPermission(item.id, "allow") }) { Text("Allow") }
          OutlinedButton(onClick = { onReplyPermission(item.id, "deny") }) { Text("Deny") }
        }
      }
      questions.forEach { item ->
        Text("Question")
        item.questions.forEachIndexed { index, q ->
          Text(q.question)
          if (q.multiple) {
            Text("Multiple selection supported via repeated replies")
          }
          q.options.forEach { option ->
            OutlinedButton(
              onClick = {
                onReplyQuestion(item.id, oneAnswer(item.questions.size, index, option.label))
              },
            ) {
              Text(option.label)
            }
          }
          if (q.custom) {
            var custom by remember(item.id, index) { mutableStateOf("") }
            OutlinedTextField(
              value = custom,
              onValueChange = { custom = it },
              label = { Text("Custom answer") },
              modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  onReplyQuestion(item.id, oneAnswer(item.questions.size, index, custom.trim()))
                },
                enabled = custom.isNotBlank(),
              ) {
                Text("Submit")
              }
              OutlinedButton(onClick = { onRejectQuestion(item.id) }) {
                Text("Reject")
              }
            }
          }
        }
      }
    }
  }
}

private fun oneAnswer(total: Int, index: Int, value: String): List<List<String>> {
  return List(total) { current ->
    if (current != index || value.isBlank()) return@List emptyList()
    listOf(value)
  }
}

@Composable
private fun MessageItem(
  message: MessageDto,
  parts: List<PartDto>,
) {
  Card {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      val label = buildString {
        append(message.role)
        if (!message.agent.isNullOrBlank()) append(" • ${message.agent}")
      }
      Text(label, style = MaterialTheme.typography.labelMedium)
      val metrics = usageLine(message)
      if (metrics != null) {
        Text(metrics, style = MaterialTheme.typography.labelSmall)
      }
      Text(renderText(parts))
    }
  }
}

private fun usageLine(message: MessageDto): String? {
  val tokens = message.tokens
  val cost = message.cost
  if (tokens == null && cost == null) return null
  val usage = buildString {
    if (tokens != null) {
      append("in ${tokens.input} • out ${tokens.output} • reason ${tokens.reasoning}")
      if (tokens.total != null) append(" • total ${tokens.total}")
    }
    if (cost != null) {
      if (isNotEmpty()) append(" • ")
      append("cost ${"%.4f".format(cost)}")
    }
  }
  if (usage.isBlank()) return null
  return usage
}

private fun renderText(list: List<PartDto>): String {
  val body = list
    .filter { it.type == "text" || it.type == "tool" }
    .mapNotNull {
      if (it.type == "tool") return@mapNotNull it.state?.title ?: it.name ?: it.tool
      it.text
    }
    .joinToString("\n")
    .trim()
  if (body.isNotBlank()) return body
  return "(no text parts)"
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
