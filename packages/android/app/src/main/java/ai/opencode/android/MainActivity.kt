package ai.opencode.android

import ai.opencode.android.sync.OpenCodeSyncService
import ai.opencode.android.ui.MainScreen
import ai.opencode.android.ui.MainVm
import ai.opencode.android.ui.MainVmFactory
import ai.opencode.android.ui.theme.OpenCodeTheme
import ai.opencode.android.ui.theme.ThemeMode
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
  private val graph by lazy { (application as OpenCodeApp).graph }

  private val vm by lazy {
    ViewModelProvider(
      this,
      MainVmFactory(
        repo = graph.repo,
        uiStore = graph.uiStore,
        themeNames = graph.themeCatalog.names,
      ),
    )[MainVm::class.java]
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
      val state = vm.state.collectAsStateWithLifecycle()
      val dark = when (state.value.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
      }
      OpenCodeTheme(
        theme = graph.themeCatalog.get(state.value.themeName),
        dark = dark,
      ) {
        MainScreen(
          state = state.value,
          onConnect = vm::connect,
          onSelectServer = vm::selectServer,
          onShowServerScreen = vm::showServerScreen,
          onShowProjectScreen = vm::showProjectScreen,
          onRemoveServer = vm::removeServer,
          onRefreshServerStatus = vm::refreshServerStatus,
          onUpdateProjectQuery = vm::updateProjectQuery,
          onOpenProject = vm::openProject,
          onHideProject = vm::hideProject,
          onUnhideProject = vm::unhideProject,
          onCreateSession = vm::createSession,
          onOpenSession = vm::openSession,
          onDeleteSession = vm::deleteSession,
          onArchiveSession = vm::archiveSession,
          onSendPrompt = vm::sendPrompt,
          onAbort = vm::abort,
          onReplyPermission = vm::replyPermission,
          onReplyQuestion = vm::replyQuestion,
          onRejectQuestion = vm::rejectQuestion,
          onSelectModel = vm::selectModel,
          onSelectAgent = vm::selectAgent,
          onSelectVariant = vm::selectVariant,
          onSelectSessionTab = vm::selectSessionTab,
          onOpenTerminal = vm::openTerminal,
          onRunTerminalCommand = vm::runTerminalCommand,
          onSelectReviewScope = vm::selectReviewScope,
          onSelectFilesMode = vm::selectFilesMode,
          onToggleDirectory = vm::toggleDirectory,
          onOpenFile = vm::openFile,
          onCloseFilePreview = vm::closeFilePreview,
          onCloseSession = vm::closeSession,
          onSetTheme = vm::setTheme,
          onSetThemeMode = vm::setThemeMode,
          onMarkSessionSeen = vm::markSessionSeen,
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
