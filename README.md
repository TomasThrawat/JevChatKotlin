# JevChatKotlin

Native Kotlin Android chat app that works directly from the phone. No PC, local Ollama server, HTML, or WebView is required.

## Phone-only mode

The app sends the conversation directly to the Gemini Developer API over HTTPS. No backend server is needed on your side.

Default model: \`gemini-3.8-flash\`

The API key and chat history are held in app memory only and are not written to files.

## Setup

Open Google AI Studio from the button in the app, create an API key with an eligible account/project, paste the key into the app, and start chatting.

Google documents a Free Tier for selected Gemini API models with free input and output tokens, subject to the active rate limits for the project.

Do not put a personal API key into GitHub, source files, screenshots, or public posts.

## Build

GitHub Actions builds the debug APK and uploads the \`JevChat-debug\` artifact.
