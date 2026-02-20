package ai.opencode.android.sync

import ai.opencode.android.MainActivity
import ai.opencode.android.OpenCodeApp
import ai.opencode.android.R
import ai.opencode.android.core.model.GlobalEventDto
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class OpenCodeSyncService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var running = false

  override fun onCreate() {
    super.onCreate()
    ensureChannels()
    val foreground = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(1, foreground(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
      } else {
        startForeground(1, foreground())
      }
    }
    if (foreground.isFailure) {
      Log.w("OpenCodeSyncService", "foreground start rejected", foreground.exceptionOrNull())
      stopSelf()
      return
    }
    startSync()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!running) startSync()
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?) = null

  override fun onDestroy() {
    running = false
    scope.cancel()
    super.onDestroy()
  }

  private fun startSync() {
    if (running) return
    running = true
    val repo = (application as OpenCodeApp).graph.repo
    scope.launch {
      repo.connect()
    }
    scope.launch {
      repo.globalEvents.collectLatest { event ->
        notifyFor(event)
      }
    }
  }

  private fun foreground(): Notification {
    return NotificationCompat.Builder(this, "sync")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.notif_sync_title))
      .setContentText(getString(R.string.notif_sync_body))
      .setOngoing(true)
      .setContentIntent(openIntent())
      .build()
  }

  private fun notifyFor(event: GlobalEventDto) {
    if (event.payload.type == "permission.asked") {
      post("attention", getString(R.string.notif_permission_title), event.directory ?: "Session needs permission")
      return
    }
    if (event.payload.type == "question.asked") {
      post("attention", getString(R.string.notif_question_title), event.directory ?: "Session needs a reply")
      return
    }
    if (event.payload.type != "session.status") return
    val status = event.payload.properties.type("status") ?: return
    if (status == "error") {
      post("agent", getString(R.string.notif_session_error_title), event.directory ?: "Session reported an error")
      return
    }
    if (status == "idle") {
      post("agent", getString(R.string.notif_turn_complete_title), event.directory ?: "Response is ready")
    }
  }

  private fun post(channel: String, title: String, body: String) {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(this, channel)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(openIntent())
      .build()
    manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
  }

  private fun openIntent(): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getActivity(this, 1, intent, flags)
  }

  private fun ensureChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
      NotificationChannel("sync", getString(R.string.notif_channel_sync), NotificationManager.IMPORTANCE_LOW),
    )
    manager.createNotificationChannel(
      NotificationChannel("agent", getString(R.string.notif_channel_agent), NotificationManager.IMPORTANCE_DEFAULT),
    )
    manager.createNotificationChannel(
      NotificationChannel("attention", getString(R.string.notif_channel_attention), NotificationManager.IMPORTANCE_HIGH),
    )
  }
}

private fun JsonObject.type(name: String): String? {
  val value = this[name] ?: return null
  if (value is JsonPrimitive) return value.contentOrNull
  val nested = value as? JsonObject ?: return null
  return nested["type"]?.let { item ->
    (item as? JsonPrimitive)?.contentOrNull
  }
}
