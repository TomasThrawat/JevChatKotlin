package com.tomasthrawat.jevchat

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : android.app.Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var endpoint: EditText
    private lateinit var status: TextView

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
            singleLine = true
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
            textColor = Color.WHITE
            hintTextColor = Color.GRAY
            maxLines = 4
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        val send = Button(this).apply {
            text = "إرسال"
            setOnClickListener { sendMessage() }
        }
        row.addView(send)
        root.addView(row)

        setContentView(root)
        addMessage("Jev", "اكتب هدفك هنا.")
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
        if (text.isEmpty()) return

        addMessage("أنت", text)
        input.setText("")
        status.text = "Jev يفكر..."

        executor.execute {
            try {
                val connection = (URL(endpoint.text.toString().trim()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

                val body = JSONObject().put("message", text).toString()
                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()

                val reply = try {
                    val json = JSONObject(response)
                    json.optString("reply").ifBlank {
                        json.optString("message").ifBlank { response }
                    }
                } catch (_: Exception) {
                    response.ifBlank { "HTTP $code" }
                }

                runOnUiThread {
                    addMessage("Jev", reply)
                    status.text = if (code in 200..299) "متصل بـ Jev" else "HTTP $code"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addMessage("Jev", "فشل الاتصال: " + (e.message ?: "خطأ غير معروف"))
                    status.text = "غير متصل"
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
