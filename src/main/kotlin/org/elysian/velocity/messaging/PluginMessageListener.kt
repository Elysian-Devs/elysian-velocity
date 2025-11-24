package org.elysian.velocity.messaging

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import org.elysian.velocity.ElysianVelocity
import org.elysian.velocity.utils.DataSerializer

/**
 * Listens for plugin messages from backend servers
 * Handles incoming cross-server communication
 */
class PluginMessageListener(private val plugin: ElysianVelocity) {

    val CHANNEL_IDENTIFIER: MinecraftChannelIdentifier =
        MinecraftChannelIdentifier.from(plugin.configManager.getString("messaging.channel", "elysian:main"))

    /**
     * Handle incoming plugin message
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        // Check if it's our channel
        if (event.identifier != CHANNEL_IDENTIFIER) {
            return
        }

        // Only handle messages from backend servers
        val source = event.source
        if (source !is com.velocitypowered.api.proxy.ServerConnection) {
            return
        }

        try {
            // Deserialize message data
            val data = event.data
            val json = String(data, Charsets.UTF_8)
            val messageData = DataSerializer.deserialize(json)

            if (plugin.configManager.getBoolean("general.debug", false)) {
                plugin.logger.info("Received plugin message from ${source.serverInfo.name}")
            }

            // Handle message through MessageHandler
            plugin.messageHandler.handleMessage(source.server, messageData)

        } catch (e: Exception) {
            plugin.logger.error("Failed to handle plugin message: ${e.message}")
            e.printStackTrace()
        }
    }
}