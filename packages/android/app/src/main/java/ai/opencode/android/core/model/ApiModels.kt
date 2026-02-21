package ai.opencode.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class HealthDto(
  val healthy: Boolean,
  val version: String? = null,
)

@Serializable
data class PathDto(
  val state: String = "",
  val config: String = "",
  val worktree: String = "",
  val directory: String = "",
  val home: String = "",
)

@Serializable
data class VcsInfoDto(
  val branch: String,
)

@Serializable
data class ProjectDto(
  val id: String,
  val worktree: String,
  val vcs: String? = null,
  val name: String? = null,
  val icon: ProjectIconDto? = null,
  val time: ProjectTimeDto,
  val sandboxes: List<String> = emptyList(),
)

@Serializable
data class ProjectIconDto(
  val url: String? = null,
  val override: String? = null,
  val color: String? = null,
)

@Serializable
data class ProjectTimeDto(
  val created: Long,
  val updated: Long,
  val initialized: Long? = null,
)

@Serializable
data class ProviderListDto(
  val all: List<ProviderDto> = emptyList(),
  val connected: List<String> = emptyList(),
  val default: Map<String, String> = emptyMap(),
)

@Serializable
data class ProviderDto(
  val id: String,
  val name: String,
  val models: Map<String, ModelDto> = emptyMap(),
)

@Serializable
data class ModelDto(
  val id: String,
  val name: String,
  val family: String? = null,
  @SerialName("release_date")
  val releaseDate: String = "",
  val limit: ModelLimitDto? = null,
  val variants: Map<String, JsonObject>? = null,
)

@Serializable
data class ModelLimitDto(
  val context: Int? = null,
  val input: Int? = null,
  val output: Int? = null,
)

@Serializable
data class AgentDto(
  val name: String,
  val mode: String? = null,
  val hidden: Boolean = false,
  val model: ModelRef? = null,
  val variant: String? = null,
)

@Serializable
data class ModelRef(
  @SerialName("providerID")
  val providerId: String,
  @SerialName("modelID")
  val modelId: String,
)

@Serializable
data class SessionDto(
  val id: String,
  val slug: String,
  val projectID: String,
  val directory: String,
  val parentID: String? = null,
  val summary: SessionSummaryDto? = null,
  val title: String = "",
  val time: SessionTimeDto,
)

@Serializable
data class SessionSummaryDto(
  val additions: Int = 0,
  val deletions: Int = 0,
  val files: Int = 0,
)

@Serializable
data class SessionTimeDto(
  val created: Long,
  val updated: Long,
  val compacting: Long? = null,
  val archived: Long? = null,
)

@Serializable
data class SessionStatusDto(
  val type: String,
  val attempt: Int? = null,
  val message: String? = null,
  val next: Long? = null,
)

@Serializable
data class MessageDto(
  val id: String,
  val sessionID: String,
  val role: String,
  val parentID: String? = null,
  val agent: String? = null,
  val model: ModelRef? = null,
  val providerID: String? = null,
  val modelID: String? = null,
  val time: MessageTimeDto,
  val cost: Double? = null,
  val tokens: MessageTokensDto? = null,
)

@Serializable
data class MessageTokensDto(
  val total: Int? = null,
  val input: Int = 0,
  val output: Int = 0,
  val reasoning: Int = 0,
  val cache: MessageCacheTokensDto = MessageCacheTokensDto(),
)

@Serializable
data class MessageCacheTokensDto(
  val read: Int = 0,
  val write: Int = 0,
)

@Serializable
data class MessageTimeDto(
  val created: Long,
  val completed: Long? = null,
)

@Serializable
data class SessionMessageBundleDto(
  val info: MessageDto,
  val parts: List<PartDto> = emptyList(),
)

@Serializable
data class ToolStateDto(
  val status: String,
  val title: String? = null,
  val output: String? = null,
  val error: String? = null,
  val input: JsonObject? = null,
  val metadata: JsonObject? = null,
)

@Serializable
data class PartDto(
  val id: String,
  val sessionID: String,
  val messageID: String,
  val type: String,
  val text: String? = null,
  val name: String? = null,
  val prompt: String? = null,
  val description: String? = null,
  val mime: String? = null,
  val filename: String? = null,
  val url: String? = null,
  val tool: String? = null,
  val state: ToolStateDto? = null,
)

@Serializable
data class PermissionRequestDto(
  val id: String,
  val sessionID: String,
  val permission: String,
  val patterns: List<String> = emptyList(),
  val metadata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class QuestionInfoDto(
  val question: String,
  val header: String,
  val options: List<QuestionOptionDto> = emptyList(),
  val multiple: Boolean = false,
  val custom: Boolean = true,
)

@Serializable
data class QuestionOptionDto(
  val label: String,
  val description: String,
)

@Serializable
data class QuestionRequestDto(
  val id: String,
  val sessionID: String,
  val questions: List<QuestionInfoDto> = emptyList(),
)

@Serializable
data class TodoDto(
  val id: String = "",
  val content: String = "",
  val status: String = "pending",
  val priority: String = "medium",
)

@Serializable
data class FileDiffDto(
  val file: String,
  val before: String,
  val after: String,
  val additions: Int,
  val deletions: Int,
  val status: String? = null,
)

@Serializable
data class FileNodeDto(
  val name: String,
  val path: String,
  val absolute: String,
  val type: String,
  val ignored: Boolean = false,
)

@Serializable
data class FileContentDto(
  val type: String,
  val content: String,
  val diff: String? = null,
  val patch: FilePatchDto? = null,
  val encoding: String? = null,
  val mimeType: String? = null,
)

@Serializable
data class FilePatchDto(
  val oldFileName: String,
  val newFileName: String,
  val oldHeader: String? = null,
  val newHeader: String? = null,
  val hunks: List<FilePatchHunkDto> = emptyList(),
  val index: String? = null,
)

@Serializable
data class FilePatchHunkDto(
  val oldStart: Int,
  val oldLines: Int,
  val newStart: Int,
  val newLines: Int,
  val lines: List<String> = emptyList(),
)

@Serializable
data class FileStatusDto(
  val path: String,
  val added: Int,
  val removed: Int,
  val status: String,
)

@Serializable
data class PtyDto(
  val id: String,
  val title: String,
  val command: String,
  val args: List<String> = emptyList(),
  val cwd: String,
  val status: String,
  val pid: Int,
)

@Serializable
data class PtyCreateRequestDto(
  val title: String? = null,
)

@Serializable
data class GlobalEventDto(
  val directory: String? = null,
  val payload: EventPayloadDto,
)

@Serializable
data class EventPayloadDto(
  val type: String,
  val properties: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class CreateSessionResponseDto(
  val id: String,
)

@Serializable
data class CreateSessionRequestDto(
  @SerialName("parentID")
  val parentId: String? = null,
  val title: String? = null,
)

@Serializable
data class UpdateSessionRequestDto(
  val title: String? = null,
  val time: UpdateSessionTimeDto? = null,
)

@Serializable
data class UpdateSessionTimeDto(
  val archived: Long? = null,
)

@Serializable
data class PromptPartInputDto(
  val type: String,
  val text: String? = null,
  val mime: String? = null,
  val filename: String? = null,
  val url: String? = null,
)

@Serializable
data class PromptRequestDto(
  val agent: String? = null,
  val model: ModelRef? = null,
  val variant: String? = null,
  val messageID: String? = null,
  val parts: List<PromptPartInputDto>,
)

@Serializable
data class SessionShellRequestDto(
  val agent: String,
  val model: ModelRef? = null,
  val command: String,
)

@Serializable
data class ReplyPermissionRequestDto(
  val reply: String,
  val message: String? = null,
)

@Serializable
data class ReplyQuestionRequestDto(
  val answers: List<List<String>>,
)

@Serializable
data class ConfigDto(
  val permission: JsonElement? = null,
)
