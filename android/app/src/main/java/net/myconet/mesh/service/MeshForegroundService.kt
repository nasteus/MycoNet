package net.myconet.mesh.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import net.myconet.mesh.MainActivity
import net.myconet.mesh.MycoNetApplication
import net.myconet.mesh.R
import net.myconet.mesh.model.MeshMessage
import net.myconet.mesh.protocol.FloodRouter
import net.myconet.mesh.protocol.MeshPeer
import net.myconet.mesh.protocol.RoutingLog
import net.myconet.mesh.protocol.WifiAwareMeshManager
import java.util.concurrent.CopyOnWriteArrayList

class MeshForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "myconet_mesh_service_channel"
        const val NOTIFICATION_ID = 4288

        var activeService: MeshForegroundService? = null
            private set
    }

    private val tag = "MycoNet::Service"
    private lateinit var wakeLockManager: WakeLockManager
    lateinit var floodRouter: FloodRouter
        private set
    lateinit var meshManager: WifiAwareMeshManager
        private set

    val recentLogs = CopyOnWriteArrayList<RoutingLog>()
    val receivedMessages = CopyOnWriteArrayList<MeshMessage>()

    var onNewMessageListener: ((MeshMessage) -> Unit)? = null
    var onNewLogListener: ((RoutingLog) -> Unit)? = null
    var onPeersChangedListener: ((List<MeshPeer>) -> Unit)? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MeshForegroundService = this@MeshForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        activeService = this
        Log.i(tag, "MeshForegroundService initializing...")

        wakeLockManager = WakeLockManager(this)
        wakeLockManager.acquireWakeLock()

        createNotificationChannel()
        startInForeground()

        floodRouter = FloodRouter(
            selfNodeId = MycoNetApplication.nodeId,
            onLocalMessageReceived = { msg ->
                receivedMessages.add(msg)
                onNewMessageListener?.invoke(msg)
            },
            onRelayRequired = { bytes ->
                meshManager.broadcastPacket(bytes)
            },
            onLogEmitted = { log ->
                if (recentLogs.size > 200) {
                    recentLogs.removeAt(0)
                }
                recentLogs.add(log)
                onNewLogListener?.invoke(log)
            }
        )

        meshManager = WifiAwareMeshManager(this, floodRouter).apply {
            onPeerListChanged = { peers ->
                onPeersChangedListener?.invoke(peers)
            }
            start()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MycoNet Mesh Router",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent mesh routing engine running in background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startInForeground() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MycoNet Mesh Node [${MycoNetApplication.nodeId}]")
            .setContentText("Off-grid D2D relay active • Screen-off routing enabled")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stop()
        wakeLockManager.releaseWakeLock()
        activeService = null
        Log.i(tag, "MeshForegroundService destroyed")
    }
}
