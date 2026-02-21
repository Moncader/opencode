package ai.opencode.android.core.network

import ai.opencode.android.core.model.AgentDto
import ai.opencode.android.core.model.ConfigDto
import ai.opencode.android.core.model.CreateSessionRequestDto
import ai.opencode.android.core.model.CreateSessionResponseDto
import ai.opencode.android.core.model.FileContentDto
import ai.opencode.android.core.model.FileDiffDto
import ai.opencode.android.core.model.FileNodeDto
import ai.opencode.android.core.model.FileStatusDto
import ai.opencode.android.core.model.HealthDto
import ai.opencode.android.core.model.MessageDto
import ai.opencode.android.core.model.ModelRef
import ai.opencode.android.core.model.PathDto
import ai.opencode.android.core.model.PermissionRequestDto
import ai.opencode.android.core.model.PtyCreateRequestDto
import ai.opencode.android.core.model.PtyDto
import ai.opencode.android.core.model.ProjectDto
import ai.opencode.android.core.model.PromptPartInputDto
import ai.opencode.android.core.model.PromptRequestDto
import ai.opencode.android.core.model.QuestionRequestDto
import ai.opencode.android.core.model.ReplyPermissionRequestDto
import ai.opencode.android.core.model.ReplyQuestionRequestDto
import ai.opencode.android.core.model.SessionShellRequestDto
import ai.opencode.android.core.model.SessionDto
import ai.opencode.android.core.model.SessionMessageBundleDto
import ai.opencode.android.core.model.SessionStatusDto
import ai.opencode.android.core.model.TodoDto
import ai.opencode.android.core.model.UpdateSessionRequestDto
import ai.opencode.android.core.model.UpdateSessionTimeDto
import ai.opencode.android.core.model.VcsInfoDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class OpenCodeApi(
  private val client: OkHttpClient,
  private val json: Json,
  private val baseUrl: String,
  private val directory: String? = null,
) {
  private val jsonMedia = "application/json; charset=utf-8".toMediaType()

  private data class AuthUrl(
    val url: HttpUrl,
    val auth: String?,
  )

  private val parsedBase by lazy {
    parseBase(baseUrl)
  }

  suspend fun globalHealth(): HealthDto {
    return get("/global/health", HealthDto.serializer())
  }

  suspend fun pathGet(): PathDto {
    return get("/path", PathDto.serializer())
  }

  suspend fun projectList(): List<ProjectDto> {
    return get("/project", ListSerializer(ProjectDto.serializer()))
  }

  suspend fun providerList(): ai.opencode.android.core.model.ProviderListDto {
    return get("/provider", ai.opencode.android.core.model.ProviderListDto.serializer())
  }

  suspend fun providerAuth(): Map<String, JsonElement> {
    return get(
      "/provider/auth",
      MapSerializer(String.serializer(), JsonElement.serializer()),
    )
  }

  suspend fun globalConfig(): ConfigDto {
    return get("/global/config", ConfigDto.serializer())
  }

  suspend fun appAgents(): List<AgentDto> {
    return get("/agent", ListSerializer(AgentDto.serializer()))
  }

  suspend fun configGet(): ConfigDto {
    return get("/config", ConfigDto.serializer())
  }

  suspend fun vcsGet(): VcsInfoDto {
    return get("/vcs", VcsInfoDto.serializer())
  }

  suspend fun sessionList(limit: Int = 100, roots: Boolean = true): List<SessionDto> {
    return get(
      "/session",
      ListSerializer(SessionDto.serializer()),
      query = mapOf(
        "limit" to limit.toString(),
        "roots" to roots.toString(),
      ),
    )
  }

  suspend fun sessionGet(sessionId: String): SessionDto {
    return get("/session/$sessionId", SessionDto.serializer())
  }

  suspend fun sessionDelete(sessionId: String) {
    val request = request(method = "DELETE", path = "/session/$sessionId")
    executeUnit(request)
  }

  suspend fun sessionUpdate(sessionId: String, title: String? = null, archived: Long? = null): SessionDto {
    return post(
      "/session/$sessionId",
      UpdateSessionRequestDto(
        title = title,
        time = if (archived == null) null else UpdateSessionTimeDto(archived = archived),
      ),
      UpdateSessionRequestDto.serializer(),
      SessionDto.serializer(),
    )
  }

  suspend fun sessionStatus(): Map<String, SessionStatusDto> {
    return get(
      "/session/status",
      MapSerializer(String.serializer(), SessionStatusDto.serializer()),
    )
  }

  suspend fun sessionMessages(sessionId: String, limit: Int = 400): List<SessionMessageBundleDto> {
    return get(
      "/session/$sessionId/message",
      ListSerializer(SessionMessageBundleDto.serializer()),
      query = mapOf("limit" to limit.toString()),
    )
  }

  suspend fun sessionCreate(parentId: String? = null, title: String? = null): CreateSessionResponseDto {
    return post(
      "/session",
      CreateSessionRequestDto(parentId = parentId, title = title),
      CreateSessionRequestDto.serializer(),
      CreateSessionResponseDto.serializer(),
    )
  }

  suspend fun sessionPromptAsync(sessionId: String, request: PromptRequestDto) {
    postUnit("/session/$sessionId/prompt_async", request, PromptRequestDto.serializer())
  }

  suspend fun sessionShell(sessionId: String, agent: String, model: ModelRef?, command: String): MessageDto {
    return post(
      "/session/$sessionId/shell",
      SessionShellRequestDto(agent = agent, model = model, command = command),
      SessionShellRequestDto.serializer(),
      MessageDto.serializer(),
    )
  }

  suspend fun sessionAbort(sessionId: String) {
    postUnit("/session/$sessionId/abort")
  }

  suspend fun sessionTodo(sessionId: String): List<TodoDto> {
    return get(
      "/session/$sessionId/todo",
      ListSerializer(TodoDto.serializer()),
    )
  }

  suspend fun sessionDiff(sessionId: String, messageId: String? = null): List<FileDiffDto> {
    return get(
      "/session/$sessionId/diff",
      ListSerializer(FileDiffDto.serializer()),
      query = mapOf("messageID" to messageId),
    )
  }

  suspend fun permissionList(): List<PermissionRequestDto> {
    return get(
      "/permission",
      ListSerializer(PermissionRequestDto.serializer()),
    )
  }

  suspend fun permissionReply(requestId: String, reply: String, message: String? = null) {
    postUnit(
      "/permission/$requestId/reply",
      ReplyPermissionRequestDto(reply = reply, message = message),
      ReplyPermissionRequestDto.serializer(),
    )
  }

  suspend fun questionList(): List<QuestionRequestDto> {
    return get(
      "/question",
      ListSerializer(QuestionRequestDto.serializer()),
    )
  }

  suspend fun questionReply(requestId: String, answers: List<List<String>>) {
    postUnit(
      "/question/$requestId/reply",
      ReplyQuestionRequestDto(answers = answers),
      ReplyQuestionRequestDto.serializer(),
    )
  }

  suspend fun questionReject(requestId: String) {
    postUnit("/question/$requestId/reject")
  }

  suspend fun fileList(path: String): List<FileNodeDto> {
    return get(
      "/file",
      ListSerializer(FileNodeDto.serializer()),
      query = mapOf("path" to path),
    )
  }

  suspend fun fileRead(path: String): FileContentDto {
    return get(
      "/file/content",
      FileContentDto.serializer(),
      query = mapOf("path" to path),
    )
  }

  suspend fun fileStatus(): List<FileStatusDto> {
    return get(
      "/file/status",
      ListSerializer(FileStatusDto.serializer()),
    )
  }

  suspend fun findFiles(
    queryValue: String,
    dirs: Boolean = true,
    type: String = "file",
    limit: Int = 100,
  ): List<String> {
    return get(
      "/find/file",
      ListSerializer(String.serializer()),
      query = mapOf(
        "query" to queryValue,
        "dirs" to dirs.toString(),
        "type" to type,
        "limit" to limit.toString(),
      ),
    )
  }

  suspend fun ptyList(): List<PtyDto> {
    return get("/pty", ListSerializer(PtyDto.serializer()))
  }

  suspend fun ptyCreate(title: String? = null): PtyDto {
    return post(
      "/pty",
      PtyCreateRequestDto(title = title),
      PtyCreateRequestDto.serializer(),
      PtyDto.serializer(),
    )
  }

  suspend fun ptyRemove(ptyId: String) {
    val request = request(method = "DELETE", path = "/pty/$ptyId")
    executeUnit(request)
  }

  suspend fun buildPromptRequest(
    text: String,
    agent: String? = null,
    model: ModelRef? = null,
    variant: String? = null,
    attachments: List<PromptAttachment> = emptyList(),
  ): PromptRequestDto {
    return PromptRequestDto(
      agent = agent,
      model = model,
      variant = variant,
      parts = listOf(
        PromptPartInputDto(type = "text", text = text),
      ) + attachments.map {
        PromptPartInputDto(
          type = "file",
          mime = it.mime,
          filename = it.filename,
          url = it.dataUrl,
        )
      },
    )
  }

  private suspend fun <T> get(
    path: String,
    serializer: kotlinx.serialization.DeserializationStrategy<T>,
    query: Map<String, String?> = emptyMap(),
  ): T {
    val request = request(method = "GET", path = path, query = query)
    return execute(request, serializer)
  }

  private suspend fun <T, B> post(
    path: String,
    body: B,
    bodySerializer: kotlinx.serialization.SerializationStrategy<B>,
    serializer: kotlinx.serialization.DeserializationStrategy<T>,
    query: Map<String, String?> = emptyMap(),
  ): T {
    val payload = json.encodeToString(bodySerializer, body)
    val request = request(method = "POST", path = path, query = query, body = payload)
    return execute(request, serializer)
  }

  private suspend fun <B> postUnit(
    path: String,
    body: B,
    serializer: kotlinx.serialization.SerializationStrategy<B>,
    query: Map<String, String?> = emptyMap(),
  ) {
    val payload = json.encodeToString(serializer, body)
    val request = request(method = "POST", path = path, query = query, body = payload)
    executeUnit(request)
  }

  private suspend fun postUnit(
    path: String,
    query: Map<String, String?> = emptyMap(),
  ) {
    val request = request(method = "POST", path = path, query = query, body = "{}")
    executeUnit(request)
  }

  private fun request(
    method: String,
    path: String,
    query: Map<String, String?> = emptyMap(),
    body: String? = null,
  ): Request {
    val parsed = parsedBase
    val builder = parsed.url.newBuilder()

    val cleanPath = path.trim().trimStart('/').split('/').filter { it.isNotEmpty() }
    for (segment in cleanPath) {
      builder.addPathSegment(segment)
    }

    for ((key, value) in query) {
      if (value == null) continue
      builder.addQueryParameter(key, value)
    }

    val req = Request.Builder().url(builder.build())
    req.header("Accept", "application/json")

    parsed.auth?.let { req.header("Authorization", it) }

    val scoped = directoryHeader(directory)
    if (scoped != null) {
      req.header("x-opencode-directory", scoped)
    }

    if (method == "GET") {
      req.get()
    }

    if (method != "GET") {
      val payload = body ?: "{}"
      req.method(method, payload.toRequestBody(jsonMedia))
      req.header("Content-Type", "application/json")
    }

    return req.build()
  }

  private suspend fun <T> execute(
    request: Request,
    serializer: kotlinx.serialization.DeserializationStrategy<T>,
  ): T {
    val response = call(request)
    val body = response.body?.string().orEmpty()
    if (!response.isSuccessful) {
      throw OpenCodeApiException("${response.code}: ${body.ifBlank { response.message }}")
    }
    if (body.isBlank()) {
      throw OpenCodeApiException("Empty response body for ${request.url.encodedPath}")
    }
    return json.decodeFromString(serializer, body)
  }

  private suspend fun executeUnit(request: Request) {
    val response = call(request)
    if (response.isSuccessful) return
    val body = response.body?.string().orEmpty()
    throw OpenCodeApiException("${response.code}: ${body.ifBlank { response.message }}")
  }

  private suspend fun call(request: Request): Response {
    return withContext(Dispatchers.IO) {
      client.newCall(request).execute()
    }
  }

  private fun parseBase(raw: String): AuthUrl {
    val parsed = raw.toHttpUrlOrNull() ?: throw OpenCodeApiException("Invalid server URL: $raw")
    val auth = when {
      parsed.username.isNotEmpty() -> {
        val user = parsed.username
        val pass = parsed.password
        okhttp3.Credentials.basic(user, pass)
      }
      else -> null
    }
    val clean = parsed.newBuilder().username("").password("").build()
    return AuthUrl(url = clean, auth = auth)
  }

  private fun directoryHeader(value: String?): String? {
    if (value.isNullOrBlank()) return null
    if (value.all { it.code in 0..127 }) return value
    return URLEncoder.encode(value, StandardCharsets.UTF_8)
  }
}

class OpenCodeApiFactory(
  private val client: OkHttpClient,
  private val json: Json,
) {
  fun base(url: String): OpenCodeApi {
    return OpenCodeApi(client = client, json = json, baseUrl = url)
  }

  fun scoped(url: String, directory: String): OpenCodeApi {
    return OpenCodeApi(client = client, json = json, baseUrl = url, directory = directory)
  }
}

class OpenCodeApiException(message: String) : RuntimeException(message)

data class PromptAttachment(
  val mime: String,
  val filename: String,
  val dataUrl: String,
)
