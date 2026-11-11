package net.myconet.mesh

import android.app.Application
import android.content.Context
import android.provider.Settings
import java.util.UUID

class MycoNetApplication : Application() {

    companion object {
        lateinit var instance: MycoNetApplication
            private set

        lateinit var nodeId: String
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        nodeId = resolveNodeId()
    }

    private fun resolveNodeId(): String {
        val prefs = getSharedPreferences("myconet_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("node_id", null)
        if (id == null) {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            id = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                "NODE-" + androidId.take(6).uppercase()
            } else {
                "NODE-" + UUID.randomUUID().toString().take(6).uppercase()
            }
            prefs.edit().putString("node_id", id).apply()
        }
        return id
    }
}
