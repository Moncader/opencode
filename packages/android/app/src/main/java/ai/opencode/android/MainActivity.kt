package ai.opencode.android

import ai.opencode.android.sync.OpenCodeSyncService
import ai.opencode.android.ui.MainScreen
import ai.opencode.android.ui.MainVm
import ai.opencode.android.ui.MainVmFactory
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
  private val vm by lazy {
    ViewModelProvider(this, MainVmFactory((application as OpenCodeApp).graph.repo))[MainVm::class.java]
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    maybeAskNotificationPermission()
    runCatching {
      ContextCompat.startForegroundService(this, Intent(this, OpenCodeSyncService::class.java))
    }.onFailure {
      Log.w("MainActivity", "sync service start rejected", it)
    }
    setContent {
      MaterialTheme {
        val state = vm.state.collectAsStateWithLifecycle()
        MainScreen(
          state = state.value,
          onConnect = vm::connect,
          onSelectServer = vm::selectServer,
          onRemoveServer = vm::removeServer,
          onOpenProject = vm::openProject,
          onOpenSession = vm::openSession,
          onSendPrompt = vm::sendPrompt,
          onRunShell = vm::runShell,
          onTerminalConnect = vm::terminalConnect,
          onTerminalClose = vm::terminalClose,
          onTerminalSendRaw = vm::terminalSendRaw,
          onAbort = vm::abort,
          onReplyPermission = vm::replyPermission,
          onReplyQuestion = vm::replyQuestion,
          onRejectQuestion = vm::rejectQuestion,
          onSelectModel = vm::selectModel,
          onSelectAgent = vm::selectAgent,
          onSelectVariant = vm::selectVariant,
          onSelectSessionTab = vm::selectSessionTab,
          onOpenPath = vm::openPath,
          onParentPath = vm::parentPath,
          onCloseSession = vm::closeSession,
          onRefresh = vm::refresh,
        )
      }
    }
  }

  private fun maybeAskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
      return
    }
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
  }
}
