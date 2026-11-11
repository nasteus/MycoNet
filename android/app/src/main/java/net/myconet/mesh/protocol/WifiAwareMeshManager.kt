package net.myconet.mesh.protocol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.myconet.mesh.MycoNetApplication
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

data class MeshPeer(
    val id: String,
    val alias: String,
    val lastSeen: Long,
    val peerHandle: PeerHandle? = null,
    val directDistance: String = "Direct (1 hop)"
)

class WifiAwareMeshManager(
    private val context: Context,
    private val floodRouter: FloodRouter
) {
    private val tag = "MycoNet::MeshManager"
    private val serviceName = "MycoNet_NAN_Mesh"
    private val meshUdpPort = 42880

    private val wifiAwareManager: WifiAwareManager? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        } else null

    private var awareSession: WifiAwareSession? = null
    private var publishSession: PublishDiscoverySession? = null
    private var subscribeSession: SubscribeDiscoverySession? = null

    val connectedPeers = ConcurrentHashMap<String, MeshPeer>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var udpSocket: DatagramSocket? = null

    var onPeerListChanged: ((List<MeshPeer>) -> Unit)? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.i(tag, "Starting MycoNet Radio Mesh Manager...")

        // 1. Initialize Wi-Fi Aware if supported
        initWifiAware()

        // 2. Initialize Complementary UDP Broadcast Socket for direct subnet flooding
        initUdpMeshSocket()
    }

    fun stop() {
        isRunning = false
        publishSession?.close()
        subscribeSession?.close()
        awareSession?.close()
        publishSession = null
        subscribeSession = null
        awareSession = null

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null

        connectedPeers.clear()
        notifyPeersChanged()
        Log.i(tag, "MycoNet Mesh Manager stopped")
    }

    private fun initWifiAware() {
        if (wifiAwareManager == null || !wifiAwareManager.isAvailable) {
            Log.w(tag, "WifiAware is not available or not supported on this hardware. Running in Local UDP Mesh mode.")
            return
        }

        try {
            wifiAwareManager.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    Log.i(tag, "Wi-Fi Aware attached successfully!")
                    awareSession = session
                    startPublishing(session)
                    startSubscribing(session)
                }

                override fun onAttachFailed() {
                    Log.e(tag, "Wi-Fi Aware attach failed. Operating in fallback mode.")
                }
            }, mainHandler)
        } catch (e: Exception) {
            Log.e(tag, "Exception attaching Wi-Fi Aware", e)
        }
    }

    private fun startPublishing(session: WifiAwareSession) {
        val config = PublishConfig.Builder()
            .setServiceName(serviceName)
            .setServiceSpecificInfo(MycoNetApplication.nodeId.toByteArray(Charsets.UTF_8))
            .build()

        session.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                publishSession = session
                Log.i(tag, "PublishDiscoverySession started: $serviceName")
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                Log.d(tag, "Received Aware message from peer: ${message.size} bytes")
                handleIncomingBytes(message, peerHandle)
            }
        }, mainHandler)
    }

    private fun startSubscribing(session: WifiAwareSession) {
        val config = SubscribeConfig.Builder()
            .setServiceName(serviceName)
            .build()

        session.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                subscribeSession = session
                Log.i(tag, "SubscribeDiscoverySession started: $serviceName")
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray?,
                matchFilter: MutableList<ByteArray>?
            ) {
                val peerNodeId = serviceSpecificInfo?.toString(Charsets.UTF_8) ?: "PEER-${peerHandle.hashCode()}"
                Log.i(tag, "Discovered Wi-Fi Aware peer: $peerNodeId")
                registerPeer(peerNodeId, peerHandle)
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                Log.d(tag, "Received Aware message from peer (subscribe): ${message.size} bytes")
                handleIncomingBytes(message, peerHandle)
            }
        }, mainHandler)
    }

    private fun handleIncomingBytes(bytes: ByteArray, peerHandle: PeerHandle?) {
        floodRouter.ingestRawPacket(bytes, peerSource = peerHandle?.toString() ?: "RADIO")
    }

    private fun registerPeer(peerId: String, peerHandle: PeerHandle?) {
        connectedPeers[peerId] = MeshPeer(
            id = peerId,
            alias = peerId,
            lastSeen = System.currentTimeMillis(),
            peerHandle = peerHandle
        )
        notifyPeersChanged()
    }

    private fun notifyPeersChanged() {
        mainHandler.post {
            onPeerListChanged?.invoke(connectedPeers.values.toList())
        }
    }

    /**
     * Retransmits a raw protobuf packet over the radio mesh
     */
    fun broadcastPacket(bytes: ByteArray) {
        // 1. Send via Wi-Fi Aware to all known discovery sessions
        publishSession?.let { pub ->
            for (peer in connectedPeers.values) {
                peer.peerHandle?.let { handle ->
                    try {
                        pub.sendMessage(handle, (System.currentTimeMillis() % 1000).toInt(), bytes)
                    } catch (e: Exception) {
                        Log.e(tag, "Error sending Aware message", e)
                    }
                }
            }
        }

        subscribeSession?.let { sub ->
            for (peer in connectedPeers.values) {
                peer.peerHandle?.let { handle ->
                    try {
                        sub.sendMessage(handle, (System.currentTimeMillis() % 1000).toInt(), bytes)
                    } catch (e: Exception) {
                        Log.e(tag, "Error sending Aware message", e)
                    }
                }
            }
        }

        // 2. Broadcast via Local Direct UDP Socket (ensures coverage across local ad-hoc links)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = udpSocket ?: DatagramSocket().apply { broadcast = true }
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, meshUdpPort)
                socket.send(packet)
            } catch (e: Exception) {
                Log.e(tag, "Error sending UDP broadcast", e)
            }
        }
    }

    private fun initUdpMeshSocket() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                udpSocket = DatagramSocket(meshUdpPort).apply {
                    broadcast = true
                    reuseAddress = true
                }
                Log.i(tag, "UDP Mesh Radio listener bound to port $meshUdpPort")

                val buffer = ByteArray(4096)
                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val incomingData = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                    val senderIp = packet.address.hostAddress ?: "UDP"

                    // Auto register peer IP
                    registerPeer("NODE-$senderIp", null)
                    floodRouter.ingestRawPacket(incomingData, peerSource = senderIp)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(tag, "UDP mesh listener error", e)
                }
            }
        }
    }
}
