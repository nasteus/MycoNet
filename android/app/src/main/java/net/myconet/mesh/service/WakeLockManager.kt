package net.myconet.mesh.service

import android.content.Context
import android.os.PowerManager
import android.util.Log

class WakeLockManager(private val context: Context) {

    private val tag = "MycoNet::WakeLock"
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquireWakeLock() {
        if (wakeLock != null && wakeLock?.isHeld == true) {
            Log.d(tag, "WakeLock already held")
            return
        }

        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MycoNet:MeshRoutingLock").apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.i(tag, "Partial WakeLock successfully acquired for 24/7 background mesh")
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire WakeLock", e)
        }
    }

    @Synchronized
    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(tag, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
    }
}
