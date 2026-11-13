package com.netrunner.core

import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

sealed interface DaemonEvent {
    data class AgentDelta(val sessionId: String, val text: String) : DaemonEvent
    data class PtyOutput(val sessionId: String, val chunk: String) : DaemonEvent
    data class ToolApprovalRequested(
        val sessionId: String,
        val requestId: String,
        val toolName: String,
        val summary: String,
    ) : DaemonEvent
    data class SessionClosed(val sessionId: String) : DaemonEvent
    data class Error(val sessionId: String, val message: String) : DaemonEvent
}

object RustDaemonBridge {
    init {
        System.loadLibrary("netrunner_core")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<DaemonEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<DaemonEvent> = _events.asSharedFlow()

    private external fun nativeStartDaemon(
        dataDir: String,
        rootfsDir: String,
        socketPath: String,
    )

    private external fun nativeDispatchJson(commandJson: String): String

    fun boot(filesDir: File) {
        val rootfs = File(filesDir, "rootfs/ubuntu")
        val sock = File(filesDir, "run/netrunnerd.sock")
        nativeStartDaemon(filesDir.absolutePath, rootfs.absolutePath, sock.absolutePath)
        connectEventStream(sock)
    }

    private fun connectEventStream(socketFile: File) {
        scope.launch {
            runCatching {
                val client = LocalSocket()
                client.connect(
                    LocalSocketAddress(socketFile.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM)
                )
                BufferedReader(InputStreamReader(client.inputStream)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        parseEvent(line)?.let { _events.tryEmit(it) }
                    }
                }
            }
        }
    }

    fun startAgentSession(
        sessionId: String,
        projectId: String,
        agent: String,
        prompt: String,
        model: String,
        apiKey: String,
    ): Boolean {
        val payload = JSONObject()
            .put("cmd", "start_agent_session")
            .put("session_id", sessionId)
            .put("project_id", projectId)
            .put("agent", agent)
            .put("prompt", prompt)
            .put("model", model)
            .put("api_key", apiKey)
            .toString()
        val resp = JSONObject(nativeDispatchJson(payload))
        return resp.optBoolean("ok", false)
    }

    fun spawnPty(sessionId: String, cwd: String, rows: Int, cols: Int, useProot: Boolean): Boolean {
        val payload = JSONObject()
            .put("cmd", "spawn_pty")
            .put("session_id", sessionId)
            .put("cwd", cwd)
            .put("rows", rows)
            .put("cols", cols)
            .put("use_proot", useProot)
            .toString()
        val resp = JSONObject(nativeDispatchJson(payload))
        return resp.optBoolean("ok", false)
    }

    fun writePty(sessionId: String, data: String) {
        val payload = JSONObject()
            .put("cmd", "write_pty")
            .put("session_id", sessionId)
            .put("data", data)
            .toString()
        nativeDispatchJson(payload)
    }

    fun trimCaches() {
        nativeDispatchJson("""{"cmd":"trim_caches"}""")
    }

    private fun parseEvent(jsonLine: String): DaemonEvent? = runCatching {
        val obj = JSONObject(jsonLine)
        when (obj.optString("type")) {
            "agent_delta" -> DaemonEvent.AgentDelta(obj.getString("session_id"), obj.getString("text"))
            "pty_output" -> DaemonEvent.PtyOutput(obj.getString("session_id"), obj.getString("chunk"))
            "tool_approval_requested" -> DaemonEvent.ToolApprovalRequested(
                obj.getString("session_id"),
                obj.getString("request_id"),
                obj.getString("tool_name"),
                obj.getString("summary"),
            )
            "session_closed" -> DaemonEvent.SessionClosed(obj.getString("session_id"))
            "error" -> DaemonEvent.Error(obj.getString("session_id"), obj.getString("message"))
            else -> null
        }
    }.getOrNull()
}
