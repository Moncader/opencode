# OpenCode Android

Native Android client for OpenCode.

This app targets feature parity with desktop/web for remote-server workflows, with one intentional gap in the first milestone:

- Terminal/PTTY UI is deferred to a follow-up milestone.
- The app does not run or embed a local OpenCode server.

## Scope (current)

- Connect to remote OpenCode servers
- Browse projects and sessions
- Create/open sessions
- Prompt-first session UI and streaming updates via SSE
- Permission/question interruption handling
- Background sync service and system notifications for completion/attention events

## Build

Prerequisites:

- Android Studio (latest stable)
- Android SDK platform + build tools for API 35
- JDK 17+

Then open `packages/android` as an Android Studio project.

## Notes

- API contracts are implemented against `packages/sdk/openapi.json` semantics.
- Event updates are consumed from `/global/event` using SSE.
