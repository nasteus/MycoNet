package net.myconet.mesh.protocol

import net.myconet.mesh.MycoNetApplication
import net.myconet.mesh.model.MeshMessage
import net.myconet.mesh.model.MeshPacketSerializer
import net.myconet.mesh.proto.MeshPacketProto.MeshPacket
import java.util.Collections
import java.util.LinkedHashMap

data class RoutingLog(
    val id: String,
    val timestamp: Long,
    val type: String, // "INCOMING", "OUTGOING", "RELAY", "DROPPED"
    val messageId: String,
    val sender: String,
    val target: String,
    val hops: Int,
    val ttl: Int,
    val note: String
)

class FloodRouter(
    private val selfNodeId: String = MycoNetApplication.nodeId,
    private val onLocalMessageReceived: (MeshMessage) -> Unit,
    private val onRelayRequired: (ByteArray) -> Unit,
    private val onLogEmitted: (RoutingLog) -> Unit
) {

    // Cache of processed message IDs with LRU eviction (max 500 messages)
    private val seenMessageIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(500, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 500
            }
        }
    )

    fun isMessageSeen(messageId: String): Boolean {
        return seenMessageIds.containsKey(messageId)
    }

    fun markMessageSeen(messageId: String) {
        seenMessageIds[messageId] = System.currentTimeMillis()
    }

    /**
     * Process an incoming raw byte packet received from radio
     */
    fun ingestRawPacket(bytes: ByteArray, peerSource: String = "RADIO") {
        val packet = MeshPacketSerializer.deserialize(bytes)
        if (packet == null) {
            emitLog("DROPPED", "UNKNOWN", peerSource, "*", 0, 0, "Corrupt or non-protobuf packet")
            return
        }

        val msgId = packet.messageId
        if (isMessageSeen(msgId)) {
            emitLog("DROPPED", msgId, packet.senderId, packet.recipientId, packet.hopCount, packet.ttl, "Duplicate message already seen (Loop prevented)")
            return
        }

        // Mark as seen immediately
        markMessageSeen(msgId)

        // Validate TTL
        if (packet.ttl <= 0) {
            emitLog("DROPPED", msgId, packet.senderId, packet.recipientId, packet.hopCount, packet.ttl, "TTL exhausted (max hops exceeded)")
            return
        }

        val isForMe = packet.recipientId == "*" || packet.recipientId.equals(selfNodeId, ignoreCase = true)
        val isFromMe = packet.senderId.equals(selfNodeId, ignoreCase = true)

        if (isFromMe) {
            // Reflected echo from peer, ignore
            return
        }

        if (isForMe) {
            val meshMsg = MeshPacketSerializer.toMeshMessage(packet, isRelayed = packet.hopCount > 0)
            emitLog("INCOMING", msgId, packet.senderId, packet.recipientId, packet.hopCount, packet.ttl, "Delivered to local UI")
            onLocalMessageReceived(meshMsg)
        }

        // If broadcast or addressed to another node and TTL allows, RELAY it!
        val shouldRelay = (packet.recipientId == "*" || !isForMe) && (packet.ttl > 1)

        if (shouldRelay) {
            val relayBytes = MeshPacketSerializer.buildRelayPacket(packet, selfNodeId)
            if (relayBytes != null) {
                emitLog("RELAY", msgId, packet.senderId, packet.recipientId, packet.hopCount + 1, packet.ttl - 1, "Flooding packet to neighbor mesh nodes")
                onRelayRequired(relayBytes)
            }
        }
    }

    /**
     * Originates a new message from this node
     */
    fun originateMessage(recipientId: String, text: String, senderAlias: String): MeshMessage {
        val rawBytes = MeshPacketSerializer.buildChatPacket(
            senderId = selfNodeId,
            senderAlias = senderAlias,
            recipientId = recipientId,
            text = text
        )

        val packet = MeshPacketSerializer.deserialize(rawBytes)!!
        markMessageSeen(packet.messageId)

        emitLog("OUTGOING", packet.messageId, selfNodeId, recipientId, 0, MeshPacketSerializer.MAX_TTL, "Broadcasting new packet to radio")
        onRelayRequired(rawBytes)

        return MeshPacketSerializer.toMeshMessage(packet, isRelayed = false)
    }

    private fun emitLog(
        type: String,
        msgId: String,
        sender: String,
        target: String,
        hops: Int,
        ttl: Int,
        note: String
    ) {
        val log = RoutingLog(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            type = type,
            messageId = msgId,
            sender = sender,
            target = target,
            hops = hops,
            ttl = ttl,
            note = note
        )
        onLogEmitted(log)
    }
}
