package ai.opencode.android.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

enum class ThemeMode(val value: String) {
  System("system"),
  Dark("dark"),
  Light("light"),
  ;

  companion object {
    fun from(value: String): ThemeMode {
      return entries.firstOrNull { it.value == value } ?: System
    }
  }
}

@Immutable
data class OpenCodePalette(
  val primary: Color,
  val secondary: Color,
  val accent: Color,
  val error: Color,
  val warning: Color,
  val success: Color,
  val info: Color,
  val text: Color,
  val textMuted: Color,
  val background: Color,
  val backgroundPanel: Color,
  val backgroundElement: Color,
  val border: Color,
  val borderActive: Color,
  val borderSubtle: Color,
  val diffAdded: Color,
  val diffRemoved: Color,
  val diffAddedBg: Color,
  val diffRemovedBg: Color,
  val diffContextBg: Color,
  val markdownText: Color,
  val markdownCode: Color,
  val markdownLink: Color,
  val syntaxComment: Color,
  val syntaxString: Color,
  val syntaxKeyword: Color,
)

@Immutable
data class OpenCodeThemeColors(
  val dark: OpenCodePalette,
  val light: OpenCodePalette,
)

@Immutable
data class OpenCodeUiColors(
  val textMuted: Color,
  val border: Color,
  val borderSubtle: Color,
  val borderActive: Color,
  val panel: Color,
  val element: Color,
  val success: Color,
  val warning: Color,
  val info: Color,
  val diffAdded: Color,
  val diffRemoved: Color,
  val diffAddedBg: Color,
  val diffRemovedBg: Color,
  val diffContextBg: Color,
  val markdownText: Color,
  val markdownCode: Color,
  val markdownLink: Color,
  val syntaxComment: Color,
  val syntaxString: Color,
  val syntaxKeyword: Color,
)

val LocalOpenCodeColors = staticCompositionLocalOf {
  OpenCodeUiColors(
    textMuted = Color(0xFF888888),
    border = Color(0xFF484848),
    borderSubtle = Color(0xFF3C3C3C),
    borderActive = Color(0xFF606060),
    panel = Color(0xFF141414),
    element = Color(0xFF1E1E1E),
    success = Color(0xFF7FD88F),
    warning = Color(0xFFF5A742),
    info = Color(0xFF56B6C2),
    diffAdded = Color(0xFF4FD6BE),
    diffRemoved = Color(0xFFC53B53),
    diffAddedBg = Color(0xFF20303B),
    diffRemovedBg = Color(0xFF37222C),
    diffContextBg = Color(0xFF141414),
    markdownText = Color(0xFFEEEEEE),
    markdownCode = Color(0xFF7FD88F),
    markdownLink = Color(0xFFFAB283),
    syntaxComment = Color(0xFF808080),
    syntaxString = Color(0xFF7FD88F),
    syntaxKeyword = Color(0xFF9D7CD8),
  )
}

@Composable
fun OpenCodeTheme(
  theme: OpenCodeThemeColors,
  dark: Boolean,
  content: @Composable () -> Unit,
) {
  val palette = if (dark) theme.dark else theme.light
  val scheme = palette.toColorScheme(dark)
  val ui = OpenCodeUiColors(
    textMuted = palette.textMuted,
    border = palette.border,
    borderSubtle = palette.borderSubtle,
    borderActive = palette.borderActive,
    panel = palette.backgroundPanel,
    element = palette.backgroundElement,
    success = palette.success,
    warning = palette.warning,
    info = palette.info,
    diffAdded = palette.diffAdded,
    diffRemoved = palette.diffRemoved,
    diffAddedBg = palette.diffAddedBg,
    diffRemovedBg = palette.diffRemovedBg,
    diffContextBg = palette.diffContextBg,
    markdownText = palette.markdownText,
    markdownCode = palette.markdownCode,
    markdownLink = palette.markdownLink,
    syntaxComment = palette.syntaxComment,
    syntaxString = palette.syntaxString,
    syntaxKeyword = palette.syntaxKeyword,
  )

  CompositionLocalProvider(LocalOpenCodeColors provides ui) {
    MaterialTheme(
      colorScheme = scheme,
      content = content,
    )
  }
}

@Stable
class OpenCodeThemeCatalog(context: Context) {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  private val themes: Map<String, OpenCodeThemeColors> = context.assets.list("themes")
    .orEmpty()
    .filter { it.endsWith(".json") }
    .sorted()
    .associate { file ->
      val key = file.removeSuffix(".json")
      key to parse(context.assets.open("themes/$file").bufferedReader().use { it.readText() })
    }

  val names: List<String> = themes.keys.sortedWith(compareBy<String> { it != "opencode" }.thenBy { it })

  fun get(name: String): OpenCodeThemeColors {
    return themes[name] ?: themes["opencode"] ?: fallbackTheme()
  }

  private fun parse(raw: String): OpenCodeThemeColors {
    val root = json.parseToJsonElement(raw) as? JsonObject ?: return fallbackTheme()
    val defs = root["defs"] as? JsonObject ?: JsonObject(emptyMap())
    val node = root["theme"] as? JsonObject ?: JsonObject(emptyMap())
    return OpenCodeThemeColors(
      dark = palette(node = node, defs = defs, mode = "dark"),
      light = palette(node = node, defs = defs, mode = "light"),
    )
  }

  private fun palette(node: JsonObject, defs: JsonObject, mode: String): OpenCodePalette {
    return OpenCodePalette(
      primary = color(node = node, defs = defs, key = "primary", mode = mode, fallback = Color(0xFFFAB283)),
      secondary = color(node = node, defs = defs, key = "secondary", mode = mode, fallback = Color(0xFF5C9CF5)),
      accent = color(node = node, defs = defs, key = "accent", mode = mode, fallback = Color(0xFF9D7CD8)),
      error = color(node = node, defs = defs, key = "error", mode = mode, fallback = Color(0xFFE06C75)),
      warning = color(node = node, defs = defs, key = "warning", mode = mode, fallback = Color(0xFFF5A742)),
      success = color(node = node, defs = defs, key = "success", mode = mode, fallback = Color(0xFF7FD88F)),
      info = color(node = node, defs = defs, key = "info", mode = mode, fallback = Color(0xFF56B6C2)),
      text = color(node = node, defs = defs, key = "text", mode = mode, fallback = Color(0xFFEEEEEE)),
      textMuted = color(node = node, defs = defs, key = "textMuted", mode = mode, fallback = Color(0xFF808080)),
      background = color(node = node, defs = defs, key = "background", mode = mode, fallback = Color(0xFF0A0A0A)),
      backgroundPanel = color(
        node = node,
        defs = defs,
        key = "backgroundPanel",
        mode = mode,
        fallback = Color(0xFF141414),
      ),
      backgroundElement = color(
        node = node,
        defs = defs,
        key = "backgroundElement",
        mode = mode,
        fallback = Color(0xFF1E1E1E),
      ),
      border = color(node = node, defs = defs, key = "border", mode = mode, fallback = Color(0xFF484848)),
      borderActive = color(node = node, defs = defs, key = "borderActive", mode = mode, fallback = Color(0xFF606060)),
      borderSubtle = color(node = node, defs = defs, key = "borderSubtle", mode = mode, fallback = Color(0xFF3C3C3C)),
      diffAdded = color(node = node, defs = defs, key = "diffAdded", mode = mode, fallback = Color(0xFF4FD6BE)),
      diffRemoved = color(node = node, defs = defs, key = "diffRemoved", mode = mode, fallback = Color(0xFFC53B53)),
      diffAddedBg = color(node = node, defs = defs, key = "diffAddedBg", mode = mode, fallback = Color(0xFF20303B)),
      diffRemovedBg = color(node = node, defs = defs, key = "diffRemovedBg", mode = mode, fallback = Color(0xFF37222C)),
      diffContextBg = color(node = node, defs = defs, key = "diffContextBg", mode = mode, fallback = Color(0xFF141414)),
      markdownText = color(node = node, defs = defs, key = "markdownText", mode = mode, fallback = Color(0xFFEEEEEE)),
      markdownCode = color(node = node, defs = defs, key = "markdownCode", mode = mode, fallback = Color(0xFF7FD88F)),
      markdownLink = color(node = node, defs = defs, key = "markdownLink", mode = mode, fallback = Color(0xFFFAB283)),
      syntaxComment = color(node = node, defs = defs, key = "syntaxComment", mode = mode, fallback = Color(0xFF808080)),
      syntaxString = color(node = node, defs = defs, key = "syntaxString", mode = mode, fallback = Color(0xFF7FD88F)),
      syntaxKeyword = color(node = node, defs = defs, key = "syntaxKeyword", mode = mode, fallback = Color(0xFF9D7CD8)),
    )
  }

  private fun color(
    node: JsonObject,
    defs: JsonObject,
    key: String,
    mode: String,
    fallback: Color,
  ): Color {
    val value = node[key] ?: return fallback
    return resolve(value = value, defs = defs, theme = node, mode = mode, seen = setOf(key)) ?: fallback
  }

  private fun resolve(
    value: JsonElement,
    defs: JsonObject,
    theme: JsonObject,
    mode: String,
    seen: Set<String>,
  ): Color? {
    if (value is JsonNull) return null
    if (value is JsonObject) {
      val branch = value[mode] ?: value["dark"] ?: value["light"] ?: return null
      return resolve(branch, defs, theme, mode, seen)
    }
    if (value is JsonPrimitive && !value.isString) {
      val code = value.intOrNull ?: return null
      return ansi(code)
    }
    if (value !is JsonPrimitive || !value.isString) return null

    val text = value.content.trim()
    if (text == "none" || text == "transparent") return Color.Transparent
    if (text.startsWith("#")) return hex(text)

    if (text in seen) return null

    val fromDef = defs[text]
    if (fromDef != null) return resolve(fromDef, defs, theme, mode, seen + text)

    val fromTheme = theme[text]
    if (fromTheme != null) return resolve(fromTheme, defs, theme, mode, seen + text)

    return null
  }

  private fun ansi(code: Int): Color {
    if (code < 16) {
      val list = listOf(
        "#000000",
        "#800000",
        "#008000",
        "#808000",
        "#000080",
        "#800080",
        "#008080",
        "#c0c0c0",
        "#808080",
        "#ff0000",
        "#00ff00",
        "#ffff00",
        "#0000ff",
        "#ff00ff",
        "#00ffff",
        "#ffffff",
      )
      return hex(list.getOrNull(code) ?: "#000000")
    }
    if (code < 232) {
      val index = code - 16
      val b = index % 6
      val g = (index / 6) % 6
      val r = index / 36
      val unit = { x: Int -> if (x == 0) 0 else x * 40 + 55 }
      return Color(unit(r), unit(g), unit(b))
    }
    val gray = (code - 232) * 10 + 8
    return Color(gray, gray, gray)
  }

  private fun hex(input: String): Color {
    val raw = input.removePrefix("#")
    val normalized = when (raw.length) {
      3 -> "ff" + raw.map { "$it$it" }.joinToString("")
      4 -> {
        val rgba = raw.map { "$it$it" }.joinToString("")
        rgbaToArgb(rgba)
      }

      6 -> "ff$raw"
      8 -> rgbaToArgb(raw)
      else -> "ff000000"
    }
    return Color(normalized.toULong(16).toLong())
  }

  private fun rgbaToArgb(value: String): String {
    val r = value.substring(0, 2)
    val g = value.substring(2, 4)
    val b = value.substring(4, 6)
    val a = value.substring(6, 8)
    return "$a$r$g$b"
  }

  private fun fallbackTheme(): OpenCodeThemeColors {
    val dark = OpenCodePalette(
      primary = Color(0xFFFAB283),
      secondary = Color(0xFF5C9CF5),
      accent = Color(0xFF9D7CD8),
      error = Color(0xFFE06C75),
      warning = Color(0xFFF5A742),
      success = Color(0xFF7FD88F),
      info = Color(0xFF56B6C2),
      text = Color(0xFFEEEEEE),
      textMuted = Color(0xFF808080),
      background = Color(0xFF0A0A0A),
      backgroundPanel = Color(0xFF141414),
      backgroundElement = Color(0xFF1E1E1E),
      border = Color(0xFF484848),
      borderActive = Color(0xFF606060),
      borderSubtle = Color(0xFF3C3C3C),
      diffAdded = Color(0xFF4FD6BE),
      diffRemoved = Color(0xFFC53B53),
      diffAddedBg = Color(0xFF20303B),
      diffRemovedBg = Color(0xFF37222C),
      diffContextBg = Color(0xFF141414),
      markdownText = Color(0xFFEEEEEE),
      markdownCode = Color(0xFF7FD88F),
      markdownLink = Color(0xFFFAB283),
      syntaxComment = Color(0xFF808080),
      syntaxString = Color(0xFF7FD88F),
      syntaxKeyword = Color(0xFF9D7CD8),
    )
    val light = dark.copy(
      primary = Color(0xFF3B7DD8),
      secondary = Color(0xFF7B5BB6),
      accent = Color(0xFFD68C27),
      error = Color(0xFFD1383D),
      warning = Color(0xFFD68C27),
      success = Color(0xFF3D9A57),
      info = Color(0xFF318795),
      text = Color(0xFF1A1A1A),
      textMuted = Color(0xFF8A8A8A),
      background = Color(0xFFFFFFFF),
      backgroundPanel = Color(0xFFFAFAFA),
      backgroundElement = Color(0xFFF5F5F5),
      border = Color(0xFFB8B8B8),
      borderActive = Color(0xFFA0A0A0),
      borderSubtle = Color(0xFFD4D4D4),
      diffAdded = Color(0xFF1E725C),
      diffRemoved = Color(0xFFC53B53),
      diffAddedBg = Color(0xFFD5E5D5),
      diffRemovedBg = Color(0xFFF7D8DB),
      markdownText = Color(0xFF1A1A1A),
      markdownCode = Color(0xFF3D9A57),
      markdownLink = Color(0xFF3B7DD8),
      syntaxComment = Color(0xFF8A8A8A),
      syntaxString = Color(0xFF3D9A57),
      syntaxKeyword = Color(0xFFD68C27),
    )
    return OpenCodeThemeColors(dark = dark, light = light)
  }
}

private fun OpenCodePalette.toColorScheme(dark: Boolean): ColorScheme {
  val onPrimary = readable(primary)
  val onSecondary = readable(secondary)
  val onTertiary = readable(accent)
  val onError = readable(error)
  val onSurface = text
  val onBackground = text

  return if (dark) {
    darkColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      secondary = secondary,
      onSecondary = onSecondary,
      tertiary = accent,
      onTertiary = onTertiary,
      error = error,
      onError = onError,
      background = background,
      onBackground = onBackground,
      surface = backgroundPanel,
      onSurface = onSurface,
      surfaceVariant = backgroundElement,
      onSurfaceVariant = textMuted,
      outline = border,
      outlineVariant = borderSubtle,
      primaryContainer = primary.copy(alpha = 0.28f),
      onPrimaryContainer = text,
      secondaryContainer = secondary.copy(alpha = 0.24f),
      onSecondaryContainer = text,
      tertiaryContainer = accent.copy(alpha = 0.24f),
      onTertiaryContainer = text,
      errorContainer = error.copy(alpha = 0.24f),
      onErrorContainer = text,
      inverseSurface = text,
      inverseOnSurface = background,
      inversePrimary = primary,
      surfaceTint = primary,
    )
  } else {
    lightColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      secondary = secondary,
      onSecondary = onSecondary,
      tertiary = accent,
      onTertiary = onTertiary,
      error = error,
      onError = onError,
      background = background,
      onBackground = onBackground,
      surface = backgroundPanel,
      onSurface = onSurface,
      surfaceVariant = backgroundElement,
      onSurfaceVariant = textMuted,
      outline = border,
      outlineVariant = borderSubtle,
      primaryContainer = primary.copy(alpha = 0.16f),
      onPrimaryContainer = text,
      secondaryContainer = secondary.copy(alpha = 0.14f),
      onSecondaryContainer = text,
      tertiaryContainer = accent.copy(alpha = 0.14f),
      onTertiaryContainer = text,
      errorContainer = error.copy(alpha = 0.14f),
      onErrorContainer = text,
      inverseSurface = Color(0xFF111111),
      inverseOnSurface = Color(0xFFFFFFFF),
      inversePrimary = primary,
      surfaceTint = primary,
    )
  }
}

private fun readable(color: Color): Color {
  return if (color.luminance() > 0.45f) Color(0xFF101010) else Color(0xFFFDFDFD)
}
