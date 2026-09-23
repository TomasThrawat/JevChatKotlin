# JevChatKotlin

Native Kotlin Android Chat AI client using a free OpenRouter model endpoint.

## Model

- Model ID: `qwen/qwen3-235b-a22b:free`
- Qwen3-235B-A22B is a 235B-parameter MoE model with 22B active parameters per forward pass.
- The model supports thinking/reasoning, multilingual chat, and tool calling.
- The free endpoint is rate-limited.

## OpenRouter

- The app uses OpenRouter's OpenAI-compatible Chat Completions API.
- Enter your own OpenRouter API key from the API button.
- The API key is stored locally in SharedPreferences and is not committed to the repository.
- OpenRouter documents a Free plan with Chat/API access and 50 requests/day.
- Free model availability and limits can change.

## MCP

The native MCP Streamable HTTP client is retained. Add a Composio session MCP URL and headers from the MCP button. Discovered tools are exposed as function tools and executed with MCP tools/call.

## Storage

- Chat history is persisted in SharedPreferences without a client-side message-count or content-length cap.
- OpenRouter API key and MCP settings are stored locally.

## Build

GitHub Actions builds the debug APK as JevChat-debug.
The app requires INTERNET and uses HTTPS endpoints.
