# JevChatKotlin

Native Kotlin Android chat client for a Jev Ultrafast backend. It is a real Android app, not HTML and not a WebView.

## Architecture

The Android app sends a conversation as JSON to the Jev backend:

```text
POST /api/chat
Content-Type: application/json

{"messages":[{"role":"user","content":"Hello Jev"}]}
```

The backend returns:

```json
{"reply":"Hello...","model":"gpt-oss:20b","usage":{},"latency_ms":1234}
```

Conversation history is kept in memory by the app and is not written to files.

## Run Jev with free local Ollama

In `browser-use/jev-ultrafast` or its fork, use an OpenAI-compatible Ollama endpoint:

```env
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://127.0.0.1:11434/v1
OLLAMA_MODEL=gpt-oss:20b
JEV_BIND_HOST=127.0.0.1
```

Start Jev with:

```bash
uv run jev
```

For the Android phone to reach the backend over the same LAN, bind the server to the LAN interface and set `JEV_ALLOW_NETWORK=1`:

```env
JEV_BIND_HOST=0.0.0.0
JEV_ALLOW_NETWORK=1
```

Then set the Android endpoint to `http://<computer-lan-ip>:8766/api/chat`.

Do not expose the development server directly to the public internet.

## Build

GitHub Actions builds a debug APK and uploads the `JevChat-debug` artifact.
