package ai.opencode.android.core.sync

import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.MessageDto
import ai.opencode.android.core.model.PartDto
import ai.opencode.android.core.model.PermissionRequestDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.QuestionRequestDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.SessionStatusDto
import ai.opencode.android.core.model.TodoDto
import ai.opencode.android.core.model.VcsInfoDto

data class SyncState(
  val projects: List<ProjectDto> = emptyList(),
  val sessionsByDirectory: Map<String, List<SessionDto>> = emptyMap(),
  val messagesBySession: Map<String, List<MessageDto>> = emptyMap(),
  val partsByMessage: Map<String, List<PartDto>> = emptyMap(),
  val sessionStatusByDirectory: Map<String, Map<String, SessionStatusDto>> = emptyMap(),
  val sessionDiffBySession: Map<String, List<FileDiffDto>> = emptyMap(),
  val todoBySession: Map<String, List<TodoDto>> = emptyMap(),
  val permissionBySession: Map<String, List<PermissionRequestDto>> = emptyMap(),
  val questionBySession: Map<String, List<QuestionRequestDto>> = emptyMap(),
  val vcsByDirectory: Map<String, VcsInfoDto> = emptyMap(),
)
