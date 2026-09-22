package com.tomasthrawat.jevchat

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JSONObject
)

class McpClient(
    private val endpoint: String,
    private val customHeaders: Map<String, String>
) {
    private val nextId = AtomicInteger()
    private var sessionId: String? = null
    private var connected = false

    fun connect() {
        require(endpoint.startsWith("https://")) { "MCP endpoint must use HTTPS." }

        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", nextId.incrementAndGet())
            .put("method", "initialize")
            .put(
                "params",
                JSONObject()
                    .put("protocolVersion", "2025-11-25")
                    .put("capabilities", JSONObject())
                    .put("clientInfo", JSONObject().put("name", "JevChatKotlin").put("version", "1.1"))
            )

        val response = post(request.toString(), null)
        if (response.code !in 200..299) throw IOException("MCP initialize HTTP " + response.code)
        val envelope = jsonObject(response.body) ?: throw IOException("MCP initialize returned no JSON.")
        checkNoError(envelope)
        if (!envelope.has("result")) throw IOException("MCP initialize returned no result.")

        sessionId = response.header("Mcp-Session-Id") ?: response.header("mcp-session-id")
        connected = true

        post(
            JSONObject().put("jsonrpc", "2.0").put("method", "notifications/initialized").toString(),
            sessionId
        )
    }

    fun listTools(): List<McpTool> {
        check(connected) { "MCP is not connected." }
        val found = mutableListOf<McpTool>()
        var cursor: String? = null

        do {
            val params = JSONObject()
            if (!cursor.isNullOrBlank()) params.put("cursor", cursor)
            val request = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", nextId.incrementAndGet())
                .put("method", "tools/list")
                .put("params", params)
            val response = post(request.toString(), sessionId)
            if (response.code !in 200..299) throw IOException("MCP tools/list HTTP " + response.code)
            val envelope = jsonObject(response.body) ?: throw IOException("MCP tools/list returned no JSON.")
            checkNoError(envelope)
            val result = envelope.optJSONObject("result") ?: throw IOException("MCP tools/list returned no result.")
            val tools = result.optJSONArray("tools") ?: JSONArray()
            for (i in 0 until tools.length()) {
                val item = tools.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                val schema = item.optJSONObject("inputSchema")
                    ?: JSONObject().put("type", "object").put("properties", JSONObject())
                found.add(McpTool(name, item.optString("description").trim(), schema))
            }
            cursor = result.optString("nextCursor").trim().ifBlank { null }
        } while (cursor != null)

        return found.distinctBy { it.name }
    }

    fun callTool(name: String, arguments: JSONObject): String {
        check(connected) { "MCP is not connected." }
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", nextId.incrementAndGet())
            .put("method", "tools/call")
            .put("params", JSONObject().put("name", name).put("arguments", arguments))

        val response = post(request.toString(), sessionId)
        if (response.code !in 200..299) throw IOException("MCP tools/call HTTP " + response.code)
        val envelope = jsonObject(response.body) ?: throw IOException("MCP tool returned no JSON.")
        checkNoError(envelope)
        val result = envelope.optJSONObject("result") ?: throw IOException("MCP tool returned no result.")
        val content = result.optJSONArray("content")

        val output = buildString {
            if (content != null) {
                for (i in 0 until content.length()) {
                    val item = content.optJSONObject(i) ?: continue
                    if (item.optString("type") == "text") append(item.optString("text"))
                    else if (item.has("text")) append(item.optString("text"))
                    else append(item.toString())
                    append("\n")
                }
            }
            if (isBlank() && result.has("structuredContent")) append(result.opt("structuredContent")?.toString().orEmpty())
        }.trim()

        return if (result.optBoolean("isError", false)) {
            "MCP tool returned an error: " + output.ifBlank { "unknown tool error" }
        } else {
            output.ifBlank { "Tool completed without textual output." }
        }
    }

    fun disconnect() {
        sessionId = null
        connected = false
    }

    private data class HttpResult(
        val code: Int,
        val body: String,
        val headers: Map<String, String>
    ) {
        fun header(name: String): String? =
            headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private fun connection(method: String, currentSession: String?): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 0
            readTimeout = 0
            setRequestProperty("Accept", "application/json, text/event-stream")
            customHeaders.forEach {
                if (it.key.isNotBlank() && it.value.isNotBlank()) setRequestProperty(it.key, it.value)
            }
            if (!currentSession.isNullOrBlank() &&
                customHeaders.keys.none { it.equals("Mcp-Session-Id", ignoreCase = true) }
            ) {
                setRequestProperty("Mcp-Session-Id", currentSession)
            }
            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

    private fun post(body: String, currentSession: String?): HttpResult {
        val c = connection("POST", currentSession)
        try {
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val headers = linkedMapOf<String, String>()
            c.headerFields.forEach { (key, values) ->
                if (key != null && !values.isNullOrEmpty()) headers[key] = values.first()
            }
            return HttpResult(code, raw, headers)
        } finally {
            c.disconnect()
        }
    }

    private fun jsonObject(raw: String): JSONObject? {
        val t = raw.trim()
        if (t.startsWith("{")) return runCatching { JSONObject(t) }.getOrNull()
        val lines = t.lineSequence()
            .map { it.trim().removePrefix("data:").trim() }
            .filter { it.isNotBlank() && it != "[DONE]" }
            .toList()
        for (line in lines.asReversed()) {
            val obj = runCatching { JSONObject(line) }.getOrNull()
            if (obj != null) return obj
        }
        return null
    }

    private fun checkNoError(envelope: JSONObject) {
        val error = envelope.optJSONObject("error") ?: return
        throw IOException(error.optString("message").ifBlank { "MCP request failed." })
    }
}
