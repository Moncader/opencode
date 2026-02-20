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

private val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "opencode_ui")

@Serializable
data class UiPrefsState(
  val theme: String = "opencode",
  val mode: String = "system",
  val hiddenProjects: Map<String, List<String>> = emptyMap(),
)

class UiPrefsStore(private val context: Context) {
  private val key = stringPreferencesKey("state")
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
  }

  val state: Flow<UiPrefsState> = context.uiDataStore.data
    .catch { err ->
      if (err is IOException) {
        emit(emptyPreferences())
        return@catch
      }
      throw err
    }
    .map { pref ->
      val raw = pref[key] ?: return@map UiPrefsState()
      runCatching { json.decodeFromString<UiPrefsState>(raw) }.getOrElse { UiPrefsState() }
    }

  suspend fun setTheme(name: String) {
    val current = stateSnapshot()
    set(current.copy(theme = name))
  }

  suspend fun setMode(value: String) {
    val next = when (value) {
      "system", "dark", "light" -> value
      else -> "system"
    }
    val current = stateSnapshot()
    set(current.copy(mode = next))
  }

  suspend fun hideProject(server: String, directory: String) {
    val key = ServerStore.normalize(server) ?: return
    val current = stateSnapshot()
    val list = current.hiddenProjects[key].orEmpty().toMutableSet().also { it.add(directory) }.sorted()
    set(current.copy(hiddenProjects = current.hiddenProjects + (key to list)))
  }

  suspend fun showProject(server: String, directory: String) {
    val key = ServerStore.normalize(server) ?: return
    val current = stateSnapshot()
    val list = current.hiddenProjects[key].orEmpty().filterNot { it == directory }
    val hidden = if (list.isEmpty()) current.hiddenProjects - key else current.hiddenProjects + (key to list)
    set(current.copy(hiddenProjects = hidden))
  }

  suspend fun set(state: UiPrefsState) {
    context.uiDataStore.edit { pref ->
      pref[key] = json.encodeToString(state)
    }
  }

  suspend fun stateSnapshot(): UiPrefsState {
    return state.first()
  }
}
