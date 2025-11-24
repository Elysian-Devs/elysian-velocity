package org.elysian.velocity.api

import com.velocitypowered.api.proxy.server.RegisteredServer
import org.elysian.velocity.ElysianVelocity
import org.elysian.velocity.messaging.MessageType
import java.util.concurrent.CompletableFuture

/**
 * API for cross-server messaging operations
 * Part of the VelocityAPI
 */
class MessagingAPI(private val plugin: ElysianVelocity) {

    /**
     * Send plugin message to server
     */
    fun sendToServer(
        server: RegisteredServer,
        channel: String,
        data: Map<String, Any>
    ): CompletableFuture<Boolean> {
        return plugin.messageHandler.sendToServer(server, channel, data)
    }

    /**
     * Broadcast plugin message to all servers
     */
    fun broadcastToAll(
        channel: String,
        data: Map<String, Any>
    ): CompletableFuture<Int> {
        return plugin.messageHandler.broadcastToAll(channel, data)
    }

    /**
     * Request data from server
     */
    fun requestData(
        server: RegisteredServer,
        dataType: String,
        params: Map<String, Any> = emptyMap()
    ): CompletableFuture<Map<String, Any>?> {
        return plugin.messageHandler.requestData(server, dataType, params)
    }

    /**
     * Register custom message handler
     */
    fun registerHandler(
        type: MessageType,
        handler: (RegisteredServer, Map<String, Any>) -> Unit
    ) {
        plugin.messageHandler.registerHandler(type) { server, data ->
            handler(server, data)
        }
    }

    /**
     * Unregister message handler
     */
    fun unregisterHandler(type: MessageType) {
        plugin.messageHandler.unregisterHandler(type)
    }

    /**
     * Get messaging statistics
     */
    fun getStats(): org.elysian.velocity.messaging.MessageHandler.MessageStats {
        return plugin.messageHandler.getStats()
    }
}