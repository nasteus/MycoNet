package net.myconet.mesh.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import net.myconet.mesh.MycoNetApplication
import net.myconet.mesh.model.MeshMessage
import net.myconet.mesh.protocol.MeshPeer
import net.myconet.mesh.protocol.RoutingLog
import net.myconet.mesh.service.MeshForegroundService

class HybridBridge(private val webView: WebView) {

    private val gson = Gson()

    @JavascriptInterface
    fun getSelfNodeId(): String {
        return MycoNetApplication.nodeId
    }

    @JavascriptInterface
    fun sendMessage(recipientId: String, text: String, alias: String): String {
        val service = MeshForegroundService.activeService
        if (service == null) {
            return gson.toJson(mapOf("error" to "MeshService not active"))
        }

        val msg = service.floodRouter.originateMessage(
            recipientId = if (recipientId.isBlank()) "*" else recipientId.trim(),
            text = text,
            senderAlias = if (alias.isBlank()) "MyNode" else alias.trim()
        )
        service.receivedMessages.add(msg)
        return gson.toJson(msg)
    }

    @JavascriptInterface
    fun getRecentLogs(): String {
        val service = MeshForegroundService.activeService ?: return "[]"
        return gson.toJson(service.recentLogs)
    }

    @JavascriptInterface
    fun getRecentMessages(): String {
        val service = MeshForegroundService.activeService ?: return "[]"
        return gson.toJson(service.receivedMessages)
    }

    @JavascriptInterface
    fun getConnectedPeers(): String {
        val service = MeshForegroundService.activeService ?: return "[]"
        return gson.toJson(service.meshManager.connectedPeers.values.toList())
    }

    fun dispatchMessage(msg: MeshMessage) {
        val json = gson.toJson(msg)
        dispatchWebEvent("message", json)
    }

    fun dispatchLog(log: RoutingLog) {
        val json = gson.toJson(log)
        dispatchWebEvent("log", json)
    }

    fun dispatchPeers(peers: List<MeshPeer>) {
        val json = gson.toJson(peers)
        dispatchWebEvent("peers", json)
    }

    private fun dispatchWebEvent(eventName: String, json: String) {
        webView.post {
            val script = "if (window.onMycoNetEvent) { window.onMycoNetEvent('$eventName', $json); }"
            webView.evaluateJavascript(script, null)
        }
    }
}
