package org.elysian.velocity.messaging

import com.google.gson.Gson
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import org.elysian.velocity.ElysianVelocity
import org.elysian.velocity.utils.DataSerializer
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Handles cross-server plugin messaging
 * Manages message sending, receiving, and processing
 */
class MessageHandler(private val plugin: ElysianVelocity) {

    private val gson = Gson()

    // Pending requests: requestId -> CompletableFuture
    private val pendingRequests: MutableMap<String, CompletableFuture<Map<String, Any>>> = ConcurrentHashMap()

    // Message handlers: MessageType -> Handler function
    private val handlers: MutableMap<MessageType, MessageHandlerFunction> = ConcurrentHashMap()

    init {
        registerDefaultHandlers()
        startCleanupTask()
    }

    /**
     * Send message to specific server
     */
    fun sendToServer(
        server: RegisteredServer,
        channel: String,
        data: Map<String, Any>
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        try {
            val serialized = DataSerializer.serialize(data)
            val bytes = serialized.toByteArray(Charsets.UTF_8)

            // Check message size
            val maxSize = plugin.configManager.getInt("messaging.max-size", 16384)
            if (bytes.size > maxSize) {
                plugin.logger.warn("Message too large: ${bytes.size} bytes (max: $maxSize)")
                future.complete(false)
                return future
            }

            // Send to first player on server, or fail if no players
            val player = server.playersConnected.firstOrNull()
            if (player == null) {
                if (plugin.configManager.getBoolean("general.debug", false)) {
                    plugin.logger.warn("Cannot send message to ${server.serverInfo.name}: no players connected")
                }
                future.complete(false)
                return future
            }

            player.sendPluginMessage(
                com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier.from(channel),
                bytes
            )

            future.complete(true)

        } catch (e: Exception) {
            plugin.logger.error("Failed to send message to ${server.serverInfo.name}: ${e.message}")
            future.complete(false)
        }

        return future
    }

    /**
     * Broadcast message to all servers
     */
    fun broadcastToAll(
        channel: String,
        data: Map<String, Any>
    ): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        var successCount = 0

        try {
            val servers = plugin.server.allServers

            servers.forEach { server ->
                val result = sendToServer(server, channel, data)
                if (result.get(1, TimeUnit.SECONDS)) {
                    successCount++
                }
            }

            future.complete(successCount)

        } catch (e: Exception) {
            plugin.logger.error("Failed to broadcast message: ${e.message}")
            future.complete(successCount)
        }

        return future
    }

    /**
     * Send message to specific player's server
     */
    fun sendToPlayerServer(
        player: Player,
        channel: String,
        data: Map<String, Any>
    ): CompletableFuture<Boolean> {
        val server = player.currentServer.orElse(null)?.server
            ?: return CompletableFuture.completedFuture(false)

        return sendToServer(server, channel, data)
    }

    /**
     * Request data from server with response
     */
    fun requestData(
        server: RegisteredServer,
        dataType: String,
        params: Map<String, Any> = emptyMap()
    ): CompletableFuture<Map<String, Any>?> {
        val requestId = UUID.randomUUID().toString()
        val future = CompletableFuture<Map<String, Any>?>()

        // Store pending request
        pendingRequests[requestId] = future as CompletableFuture<Map<String, Any>>

        // Send request
        val requestData = mutableMapOf<String, Any>(
            "type" to MessageType.DATA_REQUEST.name,
            "requestId" to requestId,
            "dataType" to dataType
        )
        requestData.putAll(params)

        val channel = plugin.configManager.getString("messaging.channel", "elysian:main")
        sendToServer(server, channel, requestData)

        // Set timeout
        val timeout = plugin.configManager.getLong("messaging.timeout", 5000)
        plugin.server.scheduler.buildTask(plugin, Runnable {
            if (!future.isDone) {
                pendingRequests.remove(requestId)
                future.complete(null)
            }
        }).delay(Duration.ofMillis(timeout)).schedule()

        return future
    }

    /**
     * Handle incoming message
     */
    fun handleMessage(
        server: RegisteredServer,
        data: Map<String, Any>
    ) {
        try {
            val typeString = data["type"] as? String ?: return
            val messageType = MessageType.fromString(typeString) ?: return

            if (plugin.configManager.getBoolean("general.debug", false)) {
                plugin.logger.info("Received message type: $messageType from ${server.serverInfo.name}")
            }

            // Check if this is a response to a pending request
            val requestId = data["requestId"] as? String
            if (requestId != null && messageType == MessageType.DATA_RESPONSE) {
                handleDataResponse(requestId, data)
                return
            }

            // Handle message with registered handler
            handlers[messageType]?.handle(server, data)

        } catch (e: Exception) {
            plugin.logger.error("Error handling message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Handle data response
     */
    private fun handleDataResponse(requestId: String, data: Map<String, Any>) {
        val future = pendingRequests.remove(requestId)
        future?.complete(data)
    }

    /**
     * Register message handler
     */
    fun registerHandler(type: MessageType, handler: MessageHandlerFunction) {
        handlers[type] = handler
    }

    /**
     * Unregister message handler
     */
    fun unregisterHandler(type: MessageType) {
        handlers.remove(type)
    }

    /**
     * Register default handlers
     */
    private fun registerDefaultHandlers() {
        // Ping handler
        registerHandler(MessageType.PING) { server, data ->
            val channel = plugin.configManager.getString("messaging.channel", "elysian:main")
            sendToServer(server, channel, mapOf(
                "type" to MessageType.PONG.name,
                "timestamp" to System.currentTimeMillis()
            ))
        }

        // Player data update handler
        registerHandler(MessageType.PLAYER_DATA_UPDATE) { server, data ->
            val uuidString = data["uuid"] as? String ?: return@registerHandler
            val uuid = UUID.fromString(uuidString)

            val customData = data["customData"] as? Map<String, Any> ?: return@registerHandler

            val playerData = plugin.playerManager.getPlayerData(uuid)
            playerData.customData.putAll(customData)
            plugin.playerManager.updatePlayerData(uuid, playerData)

            if (plugin.configManager.getBoolean("general.debug", false)) {
                plugin.logger.info("Updated player data for $uuid")
            }
        }

        // Teleport request handler
        registerHandler(MessageType.TELEPORT_REQUEST) { server, data ->
            val uuidString = data["uuid"] as? String ?: return@registerHandler
            val targetServer = data["targetServer"] as? String ?: return@registerHandler

            val uuid = UUID.fromString(uuidString)
            val player = plugin.playerManager.getPlayer(uuid) ?: return@registerHandler

            plugin.api.teleportPlayerToServer(player, targetServer)
        }
    }

    /**
     * Start cleanup task for expired pending requests
     */
    private fun startCleanupTask() {
        plugin.server.scheduler.buildTask(plugin, Runnable {
            pendingRequests.entries.removeIf { (_, future) ->
                future.isDone || future.isCancelled
            }
        }).repeat(Duration.ofSeconds(30)).schedule()
    }

    /**
     * Get statistics
     */
    fun getStats(): MessageStats {
        return MessageStats(
            pendingRequests = pendingRequests.size,
            registeredHandlers = handlers.size
        )
    }

    /**
     * Message handler functional interface
     */
    fun interface MessageHandlerFunction {
        fun handle(server: RegisteredServer, data: Map<String, Any>)
    }

    /**
     * Message statistics data class
     */
    data class MessageStats(
        val pendingRequests: Int,
        val registeredHandlers: Int
    )
}