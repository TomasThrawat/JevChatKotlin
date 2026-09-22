# JevChatKotlin

Native Kotlin Android chat client for Jev.

## Current architecture

- Vireonix OpenAI-compatible chat endpoint.
- No Vireonix API key or account is required.
- Native MCP Streamable HTTP client.
- Composio Session MCP can be connected by pasting its hosted MCP URL and headers.
- MCP tools are discovered with tools/list.
- Discovered MCP tools are exposed to Vireonix as OpenAI-compatible function tools.
- Vireonix tool calls are executed with MCP tools/call and results are returned to the model for the final response.
- Chat history is memory-only for the current process.
- MCP URL and headers are saved only in local SharedPreferences after a successful connection.
- Every message has a native Copy action.
- No WebView or HTML UI is used.

## Composio

Create a Composio session with mcp: true. The session exposes session.mcp.url and session.mcp.headers. Paste those values into the app's MCP settings.

Docs: https://docs.composio.dev/docs/sessions-via-mcp

The exact tools available depend on the Composio session configuration. A session can expose search, browser, fetch, scrape, or other connected tools.

## Flow

User -> Vireonix -> OpenAI-style tool call -> native MCP tools/call -> tool result -> Vireonix final answer

The app allows up to four model/tool rounds for a single user message and limits each returned tool result to 12,000 characters.

## Build

GitHub Actions builds the debug APK as JevChat-debug.

The app requires INTERNET and uses HTTPS endpoints.
