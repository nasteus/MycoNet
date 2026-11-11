package net.myconet.mesh.model

import com.google.protobuf.ByteString
import net.myconet.mesh.proto.MeshPacketProto.MeshPacket
import net.myconet.mesh.proto.MeshPacketProto.PacketType
import java.nio.charset.StandardCharsets
import java.util.UUID

data class MeshMessage(
    val messageId: String,
    val senderId: String,
    val senderAlias: String,
    val recipientId: String,
    val text: String,
    val hopCount: Int,
    val ttl: Int,
    val timestamp: Long,
    val pathTrace: List<String>,
    val isRelayed: Boolean = false
)

object MeshPacketSerializer {

    const val MAX_TTL = 15

    fun buildChatPacket(
        senderId: String,
        senderAlias: String,
        recipientId: String,
        text: String
    ): ByteArray {
        val packet = MeshPacket.newBuilder()
            .setMessageId(UUID.randomUUID().toString())
            .setSenderId(senderId)
            .setSenderAlias(senderAlias)
            .setRecipientId(recipientId)
            .setHopCount(0)
            .setTtl(MAX_TTL)
            .setTimestamp(System.currentTimeMillis())
            .setPacketType(PacketType.CHAT)
            .setPayload(ByteString.copyFrom(text, StandardCharsets.UTF_8))
            .addPathTrace(senderId)
            .build()

        return packet.toByteArray()
    }

    fun buildRelayPacket(original: MeshPacket, relayNodeId: String): ByteArray? {
        val newTtl = original.ttl - 1
        if (newTtl <= 0) return null // Drop packet, TTL exhausted

        val relayPacket = original.toBuilder()
            .setHopCount(original.hopCount + 1)
            .setTtl(newTtl)
            .addPathTrace(relayNodeId)
            .build()

        return relayPacket.toByteArray()
    }

    fun deserialize(bytes: ByteArray): MeshPacket? {
        return try {
            MeshPacket.parseFrom(bytes)
        } catch (e: Exception) {
            null
        }
    }

    fun toMeshMessage(packet: MeshPacket, isRelayed: Boolean = false): MeshMessage {
        val text = packet.payload.toString(StandardCharsets.UTF_8)
        return MeshMessage(
            messageId = packet.messageId,
            senderId = packet.senderId,
            senderAlias = packet.senderAlias,
            recipientId = packet.recipientId,
            text = text,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            timestamp = packet.timestamp,
            pathTrace = packet.pathTraceList,
            isRelayed = isRelayed
        )
    }
}
