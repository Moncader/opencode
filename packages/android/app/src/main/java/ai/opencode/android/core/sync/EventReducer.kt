package ai.opencode.android.core.sync

import ai.opencode.android.core.model.EventPayloadDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.MessageDto
import ai.opencode.android.core.model.PartDto
import ai.opencode.android.core.model.PermissionRequestDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.QuestionRequestDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.SessionStatusDto
import ai.opencode.android.core.model.TodoDto
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class EventReducer(private val json: Json) {
  fun reduce(state: SyncState, directory: String, payload: EventPayloadDto): SyncState {
    val props = payload.properties

    if (directory == "global") {
      if (payload.type == "project.updated") {
        val project = decode<ProjectDto>(props) ?: return state
        val next = upsertBy(state.projects, project) { it.id }
        return state.copy(projects = next)
      }
      return state
    }

    return when (payload.type) {
      "session.created" -> {
        val info = decodeSessionInfo(props) ?: return state
        val current = state.sessionsByDirectory[directory].orEmpty()
        val next = upsertBy(current, info) { it.id }
        state.copy(sessionsByDirectory = state.sessionsByDirectory + (directory to next))
      }

      "session.updated" -> {
        val info = decodeSessionInfo(props) ?: return state
        val current = state.sessionsByDirectory[directory].orEmpty()
        val filtered = current.filterNot { it.id == info.id }
        val next = if (info.time.archived != null) filtered else upsertBy(filtered, info) { it.id }
        val sessions = state.sessionsByDirectory + (directory to next)
        if (info.time.archived == null) {
          return state.copy(sessionsByDirectory = sessions)
        }
        val cleaned = state.copy(sessionsByDirectory = sessions)
        return cleanupSession(cleaned, info.id)
      }

      "session.deleted" -> {
        val info = decodeSessionInfo(props) ?: return state
        val current = state.sessionsByDirectory[directory].orEmpty()
        val next = current.filterNot { it.id == info.id }
        val updated = state.copy(sessionsByDirectory = state.sessionsByDirectory + (directory to next))
        cleanupSession(updated, info.id)
      }

      "session.status" -> {
        val sessionId = props.string("sessionID") ?: return state
        val status = props.decode("status", SessionStatusDto.serializer(), json) ?: return state
        val current = state.sessionStatusByDirectory[directory].orEmpty()
        val next = current + (sessionId to status)
        state.copy(sessionStatusByDirectory = state.sessionStatusByDirectory + (directory to next))
      }

      "session.diff" -> {
        val sessionId = props.string("sessionID") ?: return state
        val diff = props.decode("diff", kotlinx.serialization.builtins.ListSerializer(FileDiffDto.serializer()), json)
          ?: return state
        state.copy(sessionDiffBySession = state.sessionDiffBySession + (sessionId to diff))
      }

      "todo.updated" -> {
        val sessionId = props.string("sessionID") ?: return state
        val todos = props.decode("todos", kotlinx.serialization.builtins.ListSerializer(TodoDto.serializer()), json)
          ?: return state
        state.copy(todoBySession = state.todoBySession + (sessionId to todos))
      }

      "message.updated" -> {
        val msg = decode<MessageDto>(props.jsonObject["info"] ?: return state) ?: return state
        val current = state.messagesBySession[msg.sessionID].orEmpty()
        val next = upsertBy(current, msg) { it.id }
        state.copy(messagesBySession = state.messagesBySession + (msg.sessionID to next))
      }

      "message.removed" -> {
        val sessionId = props.string("sessionID") ?: return state
        val messageId = props.string("messageID") ?: return state
        val current = state.messagesBySession[sessionId].orEmpty().filterNot { it.id == messageId }
        val parts = state.partsByMessage - messageId
        state.copy(
          messagesBySession = state.messagesBySession + (sessionId to current),
          partsByMessage = parts,
        )
      }

      "message.part.updated" -> {
        val part = props.decode("part", PartDto.serializer(), json) ?: return state
        val current = state.partsByMessage[part.messageID].orEmpty()
        val next = upsertBy(current, part) { it.id }
        state.copy(partsByMessage = state.partsByMessage + (part.messageID to next))
      }

      "message.part.removed" -> {
        val messageId = props.string("messageID") ?: return state
        val partId = props.string("partID") ?: return state
        val current = state.partsByMessage[messageId].orEmpty().filterNot { it.id == partId }
        val next = if (current.isEmpty()) state.partsByMessage - messageId else state.partsByMessage + (messageId to current)
        state.copy(partsByMessage = next)
      }

      "message.part.delta" -> {
        val messageId = props.string("messageID") ?: return state
        val partId = props.string("partID") ?: return state
        val field = props.string("field") ?: return state
        val delta = props.string("delta") ?: return state
        val current = state.partsByMessage[messageId].orEmpty()
        val next = current.map { part ->
          if (part.id != partId) return@map part
          appendDelta(part, field, delta)
        }
        state.copy(partsByMessage = state.partsByMessage + (messageId to next))
      }

      "permission.asked" -> {
        val permission = decode<PermissionRequestDto>(props) ?: return state
        val current = state.permissionBySession[permission.sessionID].orEmpty()
        val next = upsertBy(current, permission) { it.id }
        state.copy(permissionBySession = state.permissionBySession + (permission.sessionID to next))
      }

      "permission.replied" -> {
        val sessionId = props.string("sessionID") ?: return state
        val requestId = props.string("requestID") ?: return state
        val current = state.permissionBySession[sessionId].orEmpty().filterNot { it.id == requestId }
        state.copy(permissionBySession = state.permissionBySession + (sessionId to current))
      }

      "question.asked" -> {
        val question = decode<QuestionRequestDto>(props) ?: return state
        val current = state.questionBySession[question.sessionID].orEmpty()
        val next = upsertBy(current, question) { it.id }
        state.copy(questionBySession = state.questionBySession + (question.sessionID to next))
      }

      "question.replied", "question.rejected" -> {
        val sessionId = props.string("sessionID") ?: return state
        val requestId = props.string("requestID") ?: return state
        val current = state.questionBySession[sessionId].orEmpty().filterNot { it.id == requestId }
        state.copy(questionBySession = state.questionBySession + (sessionId to current))
      }

      else -> state
    }
  }

  private fun appendDelta(part: PartDto, field: String, delta: String): PartDto {
    if (field == "text") {
      return part.copy(text = (part.text ?: "") + delta)
    }
    return part
  }

  private fun cleanupSession(state: SyncState, sessionId: String): SyncState {
    val messages = state.messagesBySession[sessionId].orEmpty()
    val partMap = state.partsByMessage.toMutableMap()
    for (message in messages) {
      partMap.remove(message.id)
    }
    return state.copy(
      messagesBySession = state.messagesBySession - sessionId,
      sessionDiffBySession = state.sessionDiffBySession - sessionId,
      todoBySession = state.todoBySession - sessionId,
      permissionBySession = state.permissionBySession - sessionId,
      questionBySession = state.questionBySession - sessionId,
      partsByMessage = partMap,
    )
  }

  private inline fun <reified T> decode(value: kotlinx.serialization.json.JsonElement): T? {
    return runCatching { json.decodeFromJsonElement<T>(value) }.getOrNull()
  }

  private fun decodeSessionInfo(props: JsonObject): SessionDto? {
    val info = props["info"] ?: return null
    return runCatching { json.decodeFromJsonElement(SessionDto.serializer(), info) }.getOrNull()
  }

  private fun <T> upsertBy(list: List<T>, item: T, key: (T) -> String): List<T> {
    val index = list.indexOfFirst { key(it) == key(item) }
    if (index == -1) return (list + item).sortedBy(key)
    return list.toMutableList().also { it[index] = item }.sortedBy(key)
  }
}

private fun JsonObject.string(name: String): String? {
  val value = this[name] ?: return null
  val primitive = value as? JsonPrimitive ?: return null
  if (!primitive.isString) return primitive.contentOrNull
  return primitive.content
}

private fun <T> JsonObject.decode(
  name: String,
  serializer: kotlinx.serialization.DeserializationStrategy<T>,
  json: Json,
): T? {
  val value = this[name] ?: return null
  return runCatching { json.decodeFromJsonElement(serializer, value) }.getOrNull()
}
