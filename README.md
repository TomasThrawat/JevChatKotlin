# Jev Chat Kotlin

Native Kotlin Android app, not HTML or WebView.

The app provides a chat UI and sends POST JSON to a configurable Jev-compatible endpoint:
POST /api/chat
{ "message": "..." }

Expected response:
{ "reply": "..." }

The endpoint is configurable in the app. No API key is embedded in the APK.
