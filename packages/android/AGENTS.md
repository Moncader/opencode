# Android package ops

Operational guide for coding agents working in `packages/android`.

## Scope and constraints

- This app is a remote OpenCode client. Do not add local/on-device server behavior.
- Terminal must be PTY-backed (`/pty`, `/pty/{id}/connect`), not `/session/{id}/shell`.
- Keep behavior aligned with `packages/sdk/openapi.json`.

## Architecture map

- Flow: `Compose UI -> MainVm -> OpenCodeRepository -> API/SSE/PTY -> SyncState -> UI`.
- App graph/bootstrap: `app/src/main/java/ai/opencode/android/OpenCodeApp.kt`.
- Activity + top-level wiring: `app/src/main/java/ai/opencode/android/MainActivity.kt`.
- Main UI surface: `app/src/main/java/ai/opencode/android/ui/MainScreen.kt`.
- View model/orchestration: `app/src/main/java/ai/opencode/android/ui/MainVm.kt`.
- State + side effects hub: `app/src/main/java/ai/opencode/android/core/repo/OpenCodeRepository.kt`.

## Important files (quick map)

- `packages/android/app/build.gradle.kts` Android module config/dependencies.
- `packages/android/app/src/main/AndroidManifest.xml` permissions, service declaration, FGS type.
- `packages/android/app/src/main/res/values/strings.xml` user-visible copy and notification text.
- `packages/android/app/src/main/res/xml/network_security_config.xml` cleartext/network policy.
- `packages/android/app/src/main/java/ai/opencode/android/core/model/ApiModels.kt` DTOs for REST/SSE payloads.
- `packages/android/app/src/main/java/ai/opencode/android/core/network/OpenCodeApi.kt` REST client and request building.
- `packages/android/app/src/main/java/ai/opencode/android/core/network/OpenCodeSseClient.kt` `/global/event` stream handling.
- `packages/android/app/src/main/java/ai/opencode/android/core/network/OpenCodePtySocket.kt` PTY websocket transport.
- `packages/android/app/src/main/java/ai/opencode/android/core/sync/EventReducer.kt` event-to-state reducer.
- `packages/android/app/src/main/java/ai/opencode/android/core/sync/SyncState.kt` synced state model.
- `packages/android/app/src/main/java/ai/opencode/android/core/storage/ServerStore.kt` persisted server list/default/active.
- `packages/android/app/src/main/java/ai/opencode/android/sync/OpenCodeSyncService.kt` background sync + notifications.

## Build, run, deploy

Run commands from `packages/android`.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug
```

Useful direct device loop:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ai.opencode.android/.MainActivity
```

## Debug workflow

- Clear logs before reproducing: `adb logcat -c`.
- Reproduce on device, then pull focused logs:
  - `adb logcat -d | rg "FATAL EXCEPTION|AndroidRuntime|OpenCodeSyncService|MainActivity|OpenCodePtySocket|OpenCodeRepository"`
- For live follow:
  - `adb logcat -v time OpenCodeSyncService:V MainActivity:V OkHttp:V *:S`

## Known pitfalls (already hit)

- Foreground service policy is strict on modern Android.
  - `startForegroundService`/`startForeground` can be rejected.
  - Keep defensive `runCatching` guards in `MainActivity` and `OpenCodeSyncService`.
  - Keep service `START_NOT_STICKY` unless there is a strong reason to change.
- PTY websocket URL must be built carefully.
  - OkHttp `HttpUrl.Builder.scheme()` does not accept `ws`/`wss`.
  - Build HTTP(S) URL first, then convert string to `ws://` or `wss://`.
  - Keep auth in `Authorization` header, not URL userinfo.
- API payload shape is not fully rigid.
  - Parse event fields defensively (primitive vs object variants).
  - Keep tolerant JSON config (`ignoreUnknownKeys`, `explicitNulls = false`).
  - `/provider/auth` values can vary by provider.
  - `/session/{id}/todo` items may omit `id`.
  - Client message IDs for prompt paths can require `msg`-prefixed IDs.

## Where to add changes

- UI/layout/composables: `ui/MainScreen.kt` (or split into `ui/*`).
- User actions + screen state transitions: `ui/MainVm.kt`.
- New endpoint wiring: `core/network/OpenCodeApi.kt` + `core/model/ApiModels.kt`.
- Stream event handling: `core/sync/EventReducer.kt` + `core/sync/SyncState.kt`.
- Terminal behavior: `core/network/OpenCodePtySocket.kt` + repo terminal methods.
- Background notifications/service behavior: `sync/OpenCodeSyncService.kt`.

## Do and don't

- Do keep repository as single source of truth for app data.
- Do keep network and parsing work off the main thread.
- Do handle server schema drift with tolerant parsing/fallbacks.
- Do close PTY sockets and clean up PTY server-side where applicable.

- Don't add local server startup logic.
- Don't regress terminal back to shell endpoint behavior.
- Don't crash on unknown event payload shapes.
- Don't bypass repository state with ad-hoc mutable caches in UI.
