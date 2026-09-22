# JevChatKotlin

Native Kotlin Android chat app that works directly from the phone. No PC, local Ollama server, HTML, WebView, Gemini API key, or backend is required.

## Phone-only free mode

The app sends the conversation directly to the Vireonix OpenAI-compatible chat endpoint over HTTPS.

- Public model ID: `auto`
- No API key
- No account
- No payment method
- No personal credential is stored by the app
- Chat history stays in app memory only

Vireonix documents the public API as free for everyone with per-IP fair-use limits. The current documented `auto` budget is 20,000,000 input tokens/hour and 200,000 output tokens/hour per IP. It is not literally unlimited.

## Build

GitHub Actions builds the debug APK and uploads the `JevChat-debug` artifact.

## Network behavior

The app requires only Android `INTERNET` permission and uses HTTPS for the Vireonix endpoint.
