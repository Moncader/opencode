package ai.opencode.android.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.serverDataStore: DataStore<Preferences> by preferencesDataStore(name = "opencode_servers")

@Serializable
data class ServerConfig(
  val url: String,
  val name: String? = null,
)

@Serializable
data class ServerState(
  val list: List<ServerConfig> = emptyList(),
  val active: String? = null,
  val defaultUrl: String? = null,
)

class ServerStore(private val context: Context) {
  private val key = stringPreferencesKey("state")
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
  }

  val state: Flow<ServerState> = context.serverDataStore.data
    .catch { err ->
      if (err is IOException) {
        emit(emptyPreferences())
        return@catch
      }
      throw err
    }
    .map { pref ->
      val raw = pref[key] ?: return@map ServerState()
      runCatching { json.decodeFromString<ServerState>(raw) }.getOrElse { ServerState() }
    }

  suspend fun set(state: ServerState) {
    context.serverDataStore.edit { pref ->
      pref[key] = json.encodeToString(state)
    }
  }

  suspend fun upsert(url: String, name: String? = null, activate: Boolean = true) {
    val current = stateSnapshot()
    val normalized = normalize(url) ?: return
    val list = current.list.filterNot { normalize(it.url) == normalized } + ServerConfig(url = normalized, name = name)
    val next = current.copy(
      list = list,
      active = if (activate) normalized else current.active,
      defaultUrl = current.defaultUrl ?: normalized,
    )
    set(next)
  }

  suspend fun remove(url: String) {
    val current = stateSnapshot()
    val normalized = normalize(url) ?: return
    val list = current.list.filterNot { normalize(it.url) == normalized }
    val active = current.active?.let(::normalize)
    val defaultUrl = current.defaultUrl?.let(::normalize)
    val next = current.copy(
      list = list,
      active = when {
        active == normalized -> list.firstOrNull()?.url
        else -> current.active
      },
      defaultUrl = when {
        defaultUrl == normalized -> list.firstOrNull()?.url
        else -> current.defaultUrl
      },
    )
    set(next)
  }

  suspend fun setActive(url: String?) {
    val current = stateSnapshot()
    val normalized = url?.let(::normalize)
    val next = current.copy(active = normalized)
    set(next)
  }

  suspend fun setDefault(url: String?) {
    val current = stateSnapshot()
    val normalized = url?.let(::normalize)
    val next = current.copy(defaultUrl = normalized)
    set(next)
  }

  suspend fun stateSnapshot(): ServerState {
    return state.first()
  }

  companion object {
    fun normalize(raw: String): String? {
      val trimmed = raw.trim()
      if (trimmed.isEmpty()) return null
      val prefixed = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
      return prefixed.trimEnd('/')
    }
  }
}
