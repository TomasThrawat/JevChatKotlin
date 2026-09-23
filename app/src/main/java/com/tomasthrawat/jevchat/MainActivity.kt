package com.tomasthrawat.jevchat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private data class DecisionRecord(
        val state: String,
        val instructions: String,
        val type: String,
        val criteria: String,
        val result: String
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<DecisionRecord>()
    private val prefs by lazy { getSharedPreferences("jev_chat_settings", MODE_PRIVATE) }

    private lateinit var apiKey: EditText
    private lateinit var stateInput: EditText
    private lateinit var instructionsInput: EditText
    private lateinit var criteriaInput: EditText
    private lateinit var criteriaLabel: TextView
    private lateinit var typeGroup: RadioGroup
    private lateinit var status: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var evaluate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        restoreHistory()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun bg(fill: Int, stroke: Int, radius: Int = 16) =
        GradientDrawable().apply {
            setColor(fill)
            setStroke(dp(1), stroke)
            cornerRadius = dp(radius).toFloat()
        }

    private fun action(textValue: String, onClick: () -> Unit) =
        TextView(this).apply {
            text = textValue
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(24, 28, 38), Color.rgb(55, 63, 80), 14)
            setPadding(dp(10), 0, dp(10), 0)
            minHeight = dp(42)
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun field(hint: String, singleLine: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            textSize = 15.5f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(100, 108, 125))
            background = bg(Color.rgb(14, 18, 25), Color.rgb(48, 55, 70), 14)
            gravity = Gravity.TOP or Gravity.START
            if (singleLine) {
                isSingleLine = true
            } else {
                minLines = 3
                maxLines = 8
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

    private fun sectionLabel(textValue: String) =
        TextView(this).apply {
            text = textValue
            textSize = 12.5f
            setTextColor(Color.rgb(145, 153, 170))
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(2), dp(10), dp(2), dp(6))
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 11, 16))
            setPadding(dp(16), dp(12), dp(16), dp(10))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        brand.addView(TextView(this).apply {
            text = "Jev"
            textSize = 26f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })
        brand.addView(TextView(this).apply {
            text = "TypeSafe System One • jev-latest"
            textSize = 12.5f
            setTextColor(Color.rgb(145, 153, 170))
        })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        header.addView(action("مسح") {
            history.clear()
            historyContainer.removeAllViews()
            saveHistory()
            status.text = "جاهز"
        }, LinearLayout.LayoutParams(dp(62), dp(42)))
        root.addView(header)

        root.addView(sectionLabel("مفتاح TypeSafe"))
        apiKey = field("sk-...", singleLine = true).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("typesafe_api_key", "").orEmpty())
        }
        root.addView(apiKey, LinearLayout.LayoutParams(-1, dp(52)))

        val keyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        keyRow.addView(TextView(this).apply {
            text = "المفتاح يُستخدم للاتصال بـ api.typesafe.ai فقط ولا يُطبع في النتائج."
            textSize = 11.5f
            setTextColor(Color.rgb(118, 127, 145))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        keyRow.addView(action("حذف") {
            apiKey.setText("")
            prefs.edit().remove("typesafe_api_key").apply()
        }, LinearLayout.LayoutParams(dp(62), dp(40)).apply {
            marginStart = dp(8)
        })
        root.addView(keyRow)

        root.addView(sectionLabel("المحتوى / الحالة"))
        stateInput = field("مثال: هذا النص يتحدث عن سياسة الاسترجاع.")
        root.addView(stateInput, LinearLayout.LayoutParams(-1, dp(112)))

        root.addView(sectionLabel("ما الذي تريد من Jev أن يقرره؟"))
        instructionsInput = field("مثال: هل هذه الرسالة تتعلق بالفوترة؟")
        root.addView(instructionsInput, LinearLayout.LayoutParams(-1, dp(112)))

        root.addView(sectionLabel("نوع الإخراج"))
        typeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }

        val noul = RadioButton(this).apply {
            id = View.generateViewId()
            text = "نعم/لا"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        val choice = RadioButton(this).apply {
            id = View.generateViewId()
            text = "اختيار"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        val score = RadioButton(this).apply {
            id = View.generateViewId()
            text = "تقييم"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        typeGroup.addView(noul)
        typeGroup.addView(choice)
        typeGroup.addView(score)
        noul.isChecked = true
        root.addView(typeGroup, LinearLayout.LayoutParams(-1, dp(48)))

        criteriaLabel = sectionLabel("المعايير (اختياري)")
        root.addView(criteriaLabel)

        criteriaInput = field(
            "في نعم/لا: true | ما الذي يعتبر نعم؟\nfalse | ما الذي يعتبر لا؟"
        )
        root.addView(criteriaInput, LinearLayout.LayoutParams(-1, dp(128)))

        typeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                noul.id -> {
                    criteriaLabel.text = "المعايير (اختياري)"
                    criteriaInput.hint =
                        "true | ما الذي يعتبر نعم؟\nfalse | ما الذي يعتبر لا؟"
                }
                choice.id -> {
                    criteriaLabel.text = "الاختيارات"
                    criteriaInput.hint =
                        "كل سطر: name | description\nمثال:\ncairo | القاهرة\nalexandria | الإسكندرية"
                }
                score.id -> {
                    criteriaLabel.text = "مستويات التقييم"
                    criteriaInput.hint =
                        "كل سطر مستوى بالترتيب من الأقل للأعلى\nمثال:\nمنخفض\nمتوسط\nمرتفع"
                }
            }
        }

        evaluate = action("إرسال إلى Jev") {
            submitDecision()
        }.apply {
            background = bg(Color.rgb(70, 91, 153), Color.rgb(92, 115, 180), 16)
        }
        root.addView(evaluate, LinearLayout.LayoutParams(-1, dp(50)).apply {
            topMargin = dp(8)
        })

        status = TextView(this).apply {
            text = "جاهز"
            textSize = 12f
            setTextColor(Color.rgb(125, 135, 154))
            setPadding(0, dp(7), 0, dp(4))
        }
        root.addView(status)

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        scroll.addView(historyContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun saveHistory() {
        val serialized = JSONArray()
        history.forEach {
            serialized.put(
                JSONObject()
                    .put("state", it.state)
                    .put("instructions", it.instructions)
                    .put("type", it.type)
                    .put("criteria", it.criteria)
                    .put("result", it.result)
            )
        }
        prefs.edit().putString("decision_history", serialized.toString()).apply()
    }

    private fun restoreHistory() {
        val raw = prefs.getString("decision_history", "").orEmpty()
        if (raw.isBlank()) return
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return
        history.clear()
        historyContainer.removeAllViews()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val state = item.optString("state")
            val instructions = item.optString("instructions")
            val type = item.optString("type")
            val criteria = item.optString("criteria")
            val result = item.optString("result")
            if (state.isBlank() || instructions.isBlank() || result.isBlank()) continue
            val record = DecisionRecord(state, instructions, type, criteria, result)
            history.add(record)
            addDecisionCard(record)
        }
    }

    private fun parseLines(raw: String): List<String> =
        raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    private fun parseChoiceCriteria(raw: String): JSONObject {
        val criteria = JSONObject()
        parseLines(raw).forEach { line ->
            val separator = line.indexOf('|')
            if (separator > 0) {
                val name = line.substring(0, separator).trim()
                val description = line.substring(separator + 1).trim()
                if (name.isNotBlank()) {
                    criteria.put(name, description.ifBlank { name })
                }
            } else {
                criteria.put(line, line)
            }
        }
        return criteria
    }

    private fun buildQuestion(type: String): JSONObject {
        val question = JSONObject()
            .put("type", type)
            .put("instructions", instructionsInput.text.toString().trim())

        when (type) {
            "noul" -> {
                val criteriaObject = JSONObject()
                parseLines(criteriaInput.text.toString()).forEach { line ->
                    val separator = line.indexOf('|')
                    if (separator > 0) {
                        val key = line.substring(0, separator).trim()
                        val value = line.substring(separator + 1).trim()
                        if (key == "true" || key == "false") {
                            criteriaObject.put(key, value)
                        }
                    }
                }
                if (criteriaObject.length() > 0) {
                    question.put("criteria", criteriaObject)
                }
            }
            "choice" -> {
                val criteria = parseChoiceCriteria(criteriaInput.text.toString())
                if (criteria.length() < 2) {
                    throw IllegalStateException("أضف اختيارين أو أكثر في خانة الاختيارات.")
                }
                question.put("criteria", criteria)
            }
            "score" -> {
                val levels = JSONArray()
                parseLines(criteriaInput.text.toString()).forEach { levels.put(it) }
                if (levels.length() < 2) {
                    throw IllegalStateException("أضف مستويين تقييم على الأقل.")
                }
                question.put("criteria", levels)
            }
        }
        return question
    }

    private fun submitDecision() {
        val key = apiKey.text.toString().trim()
        val state = stateInput.text.toString().trim()
        val instructions = instructionsInput.text.toString().trim()

        if (key.isBlank()) {
            apiKey.error = "مفتاح TypeSafe مطلوب"
            return
        }
        if (state.isBlank()) {
            stateInput.error = "اكتب المحتوى أو الحالة"
            return
        }
        if (instructions.isBlank()) {
            instructionsInput.error = "اكتب السؤال أو القرار المطلوب"
            return
        }
        if (!evaluate.isEnabled) return

        val checkedIndex = (0 until typeGroup.childCount).firstOrNull {
            typeGroup.getChildAt(it).id == typeGroup.checkedRadioButtonId
        } ?: 0
        val type = when (checkedIndex) {
            1 -> "choice"
            2 -> "score"
            else -> "noul"
        }

        val criteria = criteriaInput.text.toString()
        val request = try {
            JSONObject()
                .put("state", state)
                .put("model", "jev-latest")
                .put(
                    "questions",
                    JSONObject().put("decision", buildQuestion(type))
                )
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "الطلب غير صالح", Toast.LENGTH_LONG).show()
            return
        }

        prefs.edit().putString("typesafe_api_key", key).apply()
        evaluate.isEnabled = false
        status.text = "Jev يحلل..."
        executor.execute {
            try {
                val response = callJev(request, key)
                val result = formatAnswer(response)
                val record = DecisionRecord(state, instructions, type, criteria, result)
                history.add(record)
                saveHistory()
                runOnUiThread {
                    addDecisionCard(record)
                    status.text = "تم"
                    evaluate.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "فشل الطلب"
                    Toast.makeText(
                        this@MainActivity,
                        e.message ?: "خطأ غير معروف",
                        Toast.LENGTH_LONG
                    ).show()
                    evaluate.isEnabled = true
                }
            }
        }
    }

    private fun callJev(request: JSONObject, key: String): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = URL("https://api.typesafe.ai/v1/systemone")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer " + key)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use {
                it.write(request.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(raw).optJSONArray("detail")
                        ?.optJSONObject(0)
                        ?.optString("msg")
                        .orEmpty()
                }.getOrDefault("")
                throw IOException(
                    if (message.isNotBlank()) {
                        "TypeSafe HTTP " + code + ": " + message
                    } else {
                        "TypeSafe HTTP " + code
                    }
                )
            }

            return JSONObject(raw)
        } finally {
            connection?.disconnect()
        }
    }

    private fun formatAnswer(response: JSONObject): String {
        val answers = response.optJSONObject("answers")
            ?: throw IllegalStateException("استجابة Jev لا تحتوي على answers.")
        val answer = answers.optJSONObject("decision")
            ?: throw IllegalStateException("استجابة Jev لا تحتوي على نتيجة القرار.")

        return when (answer.optString("type")) {
            "noul" -> {
                val probability = answer.optDouble("noul", Double.NaN)
                if (probability.isNaN()) {
                    throw IllegalStateException("نتيجة Jev من نوع noul غير صالحة.")
                }
                val label = if (probability >= 0.5) "نعم" else "لا"
                val percent = String.format(Locale.US, "%.1f%%", probability * 100.0)
                "النتيجة: " + label + "\nاحتمال نعم: " + percent
            }
            "choice" -> {
                val choice = answer.optString("choice")
                val confidence = answer.optDouble("confidence", Double.NaN)
                val probabilities = answer.optJSONObject("probabilities")
                buildString {
                    append("الاختيار: ")
                    append(choice.ifBlank { "غير محدد" })
                    if (!confidence.isNaN()) {
                        append("\nالثقة: ")
                        append(String.format(Locale.US, "%.1f%%", confidence * 100.0))
                    }
                    if (probabilities != null) {
                        val names = probabilities.keys().asSequence().toList()
                            .sortedByDescending { probabilities.optDouble(it, 0.0) }
                        if (names.isNotEmpty()) {
                            append("\n\nالاحتمالات:")
                            names.forEach { name ->
                                append("\n• ")
                                append(name)
                                append(": ")
                                append(
                                    String.format(
                                        Locale.US,
                                        "%.1f%%",
                                        probabilities.optDouble(name, 0.0) * 100.0
                                    )
                                )
                            }
                        }
                    }
                }
            }
            "score" -> {
                val score = answer.optDouble("score", Double.NaN)
                val confidence = answer.optDouble("confidence", Double.NaN)
                val legend = answer.optJSONObject("legend")
                val probabilities = answer.optJSONObject("probabilities")
                buildString {
                    append("الدرجة: ")
                    append(
                        if (score.isNaN()) {
                            "غير محددة"
                        } else {
                            String.format(Locale.US, "%.3f", score)
                        }
                    )
                    if (!confidence.isNaN()) {
                        append("\nالثقة: ")
                        append(String.format(Locale.US, "%.1f%%", confidence * 100.0))
                    }
                    if (legend != null && probabilities != null) {
                        append("\n\nالاحتمالات:")
                        val keys = probabilities.keys().asSequence().toList()
                        keys.sortedBy { it.toDoubleOrNull() ?: Double.MAX_VALUE }
                            .forEach { key ->
                                append("\n• ")
                                append(legend.optString(key, key))
                                append(": ")
                                append(
                                    String.format(
                                        Locale.US,
                                        "%.1f%%",
                                        probabilities.optDouble(key, 0.0) * 100.0
                                    )
                                )
                            }
                    }
                }
            }
            else -> throw IllegalStateException("نوع نتيجة Jev غير معروف.")
        }
    }

    private fun addDecisionCard(record: DecisionRecord) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(Color.rgb(18, 22, 30), Color.rgb(43, 49, 62), 19)
            setPadding(dp(14), dp(11), dp(11), dp(10))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Jev • " + record.type
            textSize = 12f
            setTextColor(Color.rgb(145, 155, 174))
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action("نسخ") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Jev", record.result))
            Toast.makeText(this, "تم نسخ النتيجة", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(dp(62), dp(36)))

        card.addView(header)

        card.addView(TextView(this).apply {
            text = "الحالة: " + record.state
            textSize = 14f
            setTextColor(Color.rgb(205, 211, 224))
            setPadding(0, dp(8), 0, dp(5))
        })

        card.addView(TextView(this).apply {
            text = "القرار المطلوب: " + record.instructions
            textSize = 14f
            setTextColor(Color.rgb(183, 191, 207))
            setPadding(0, 0, 0, dp(7))
        })

        card.addView(TextView(this).apply {
            text = record.result
            textSize = 16f
            setTextColor(Color.rgb(241, 244, 249))
            setTextIsSelectable(true)
            setLineSpacing(0f, 1.08f)
            setPadding(0, dp(6), 0, dp(2))
        })

        historyContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(2)
        })
        historyContainer.post {
            val parent = historyContainer.parent
            if (parent is ScrollView) parent.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
