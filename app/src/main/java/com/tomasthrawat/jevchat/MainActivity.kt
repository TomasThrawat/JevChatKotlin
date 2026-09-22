package com.tomasthrawat.jevchat

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private data class ChatMessage(val role: String, val content: String)

    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<ChatMessage>()
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var endpoint: EditText
    private lateinit var status: TextView
    private lateinit var send: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 17, 20))
            setPadding(20, 16, 20, 12)
        }

        val title = TextView(this).apply {
            text = "Jev Chat"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title)

        status = TextView(this).apply {
            text = "جاهز"
            textSize = 13f
            setTextColor(Color.LTGRAY)
        }
        root.addView(status)

        endpoint = EditText(this).apply {
            hint = "http://IP:8766/api/chat"
            setText("http://10.0.2.2:8766/api/chat")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        root.addView(endpoint)

        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        input = EditText(this).apply {
            hint = "اكتب لـ Jev..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = Button(this).apply {
            text = "إرسال"
            setOnClickListener { sendMessage() }
        }
        row.addView(send)
        root.addView(row)

        setContentView(root)
        addMessage("Jev", "اكتب هدفك أو سؤالك هنا.")
    }

    private fun addMessage(who: String, text: String) {
        val bubble = TextView(this).apply {
            this.text = "$who\n$text"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            setBackgroundColor(
                if (who == "أنت") Color.rgb(47, 55, 85)
                else Color.rgb(31, 33, 39)
            )
        }
        messages.addView(bubble, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 8
        })
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        val target = endpoint.text.toString().trim()
        if (text.isEmpty() || target.isEmpty() || send.isEnabled.not()) return

        history.add(ChatMessage("user", text))
        addMessage("أنت", text)
        input.setText("")
        send.isEnabled = false
        status.text = "Jev يفكر..."

        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(target).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 90000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

                val jsonMessages = JSONArray()
                history.takeLast(24).forEach { message ->
                    jsonMessages.put(JSONObject().put("role", message.role).put("content", message.content))
                }
                val body = JSONObject().put("messages", jsonMessages).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(response) }.getOrNull()
                val reply = json?.optString("reply")?.takeIf { it.isNotBlank() }
                val error = json?.optString("error")?.takeIf { it.isNotBlank() }
                val display = if (code in 200..299 && reply != null) reply
                else error ?: response.ifBlank { "HTTP $code" }

                if (code in 200..299 && reply != null) {
                    history.add(ChatMessage("assistant", reply))
                }

                runOnUiThread {
                    addMessage("Jev", display)
                    status.text = if (code in 200..299) "متصل بـ Jev" else "HTTP $code"
                    send.isEnabled = true
                    input.requestFocus()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addMessage("Jev", "فشل الاتصال: " + (e.message ?: "خطأ غير معروف"))
                    status.text = "غير متصل"
                    send.isEnabled = true
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
