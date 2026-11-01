package helium314.keyboard.latin.ai

import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI-compatible router for an AI keyboard.
 * Supports cloud providers (OpenAI, Groq, OpenRouter, ...) and local models
 * served via Ollama / llama.cpp / vLLM on localhost.
 */
class AiRouter(private val config: Config) {

    data class Config(
        val baseUrl: String = "",
        val apiKey: String = "",
        val model: String = "",
        val timeoutMs: Long = 30000L
    )

    fun complete(system: String, user: String): String =
        run(system, user, stream = false)

    fun rewrite(text: String, instruction: String = "Rewrite for clarity."): String =
        run(instruction, text)

    fun smartReply(context: String): String =
        run("Generate a concise, professional reply.", context)

    private fun run(system: String, user: String, stream: Boolean = false): String {
        if (config.baseUrl.isBlank() || config.model.isBlank()) return ""
        val conn = (URL(config.baseUrl.trimEnd('/') + "/v1/chat/completions")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connectTimeout = config.timeoutMs.toInt()
            readTimeout = config.timeoutMs.toInt()
            doOutput = true
        }
        val body = """{"model":"${config.model}","stream":$stream,"messages":[
            {"role":"system","content":${json(system)}},
            {"role":"user","content":${json(user)}}
        ]}"""
        conn.outputStream.use { it.write(body.toByteArray()) }
        if (conn.responseCode !in 200..299) return ""
        val raw = conn.inputStream.bufferedReader().readText()
        val idx = raw.indexOf("\"content\":\"")
        if (idx < 0) return ""
        val start = idx + "\"content\":\"".length
        val end = raw.indexOf('"', start)
        return if (end > start) raw.substring(start, end) else ""
    }

    private fun json(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
