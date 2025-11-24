package org.elysian.velocity.api

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import org.elysian.velocity.ElysianVelocity
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Public API for ElysianVelocity
 * Provides methods for cross-server operations and communication
 *
 * Usage example:
 * ```kotlin
 * val api = ElysianVelocity.instance.api
 * api.teleportPlayerToServer(player, "survival")
 * api.messaging.sendToServer(server, channel, data)
 * ```
 */
class VelocityAPI(private val plugin: ElysianVelocity) {

    /**
     * Messaging API for cross-server communication
     */
    val messaging: MessagingAPI = MessagingAPI(plugin)

    // ========== Player Operations ==========

    /**
     * Get player by UUID
     */
    fun getPlayer(uuid: UUID): Player? {
        return plugin.playerManager.getPlayer(uuid)
    }

    /**
     * Get player by name
     */
    fun getPlayer(name: String): Player? {
        return plugin.playerManager.getPlayer(name)
    }

    /**
     * Get all online players
     */
    fun getOnlinePlayers(): Collection<Player> {
        return plugin.playerManager.getOnlinePlayers()
    }

    /**
     * Get online player count
     */
    fun getOnlinePlayerCount(): Int {
        return plugin.playerManager.getOnlinePlayerCount()
    }

    /**
     * Get player's current server
     */
    fun getPlayerServer(player: Player): RegisteredServer? {
        return player.currentServer.orElse(null)?.server
    }

    /**
     * Get player's current server name
     */
    fun getPlayerServerName(player: Player): String? {
        return plugin.playerManager.getPlayerServer(player)
    }

    // ========== Teleportation ==========

    /**
     * Teleport player to server
     */
    fun teleportPlayerToServer(
        player: Player,
        serverName: String
    ): CompletableFuture<Boolean> {
        val server = plugin.serverManager.getServer(serverName)
            ?: return CompletableFuture.completedFuture(false)

        // Check if player is already on this server
        if (getPlayerServerName(player) == serverName) {
            return CompletableFuture.completedFuture(false)
        }

        return player.createConnectionRequest(server)
            .connect()
            .thenApply { result -> result.isSuccessful }
    }

    /**
     * Teleport player to server with cooldown check
     */
    fun teleportPlayerToServerWithCooldown(
        player: Player,
        serverName: String
    ): CompletableFuture<TeleportResult> {
        val future = CompletableFuture<TeleportResult>()

        // Check cooldown
        if (plugin.playerManager.hasTeleportCooldown(player.uniqueId)) {
            val remaining = plugin.playerManager.getRemainingCooldown(player.uniqueId)
            future.complete(TeleportResult.COOLDOWN(remaining))
            return future
        }

        // Check if server exists
        val server = plugin.serverManager.getServer(serverName)
        if (server == null) {
            future.complete(TeleportResult.SERVER_NOT_FOUND)
            return future
        }

        // Check if already on server
        if (getPlayerServerName(player) == serverName) {
            future.complete(TeleportResult.ALREADY_CONNECTED)
            return future
        }

        // Teleport
        player.createConnectionRequest(server)
            .connect()
            .thenApply { result ->
                if (result.isSuccessful) {
                    // Set cooldown
                    val cooldown = plugin.configManager.getInt("teleport.cooldown", 3)
                    if (cooldown > 0) {
                        plugin.playerManager.setTeleportCooldown(player.uniqueId, cooldown)
                    }
                    future.complete(TeleportResult.SUCCESS)
                } else {
                    future.complete(TeleportResult.FAILED)
                }
            }

        return future
    }

    /**
     * Teleport player to hub server
     */
    fun teleportPlayerToHub(player: Player): CompletableFuture<Boolean> {
        val hubServer = plugin.serverManager.getHubServer()
            ?: return CompletableFuture.completedFuture(false)

        return teleportPlayerToServer(player, hubServer.serverInfo.name)
    }

    // ========== Server Operations ==========

    /**
     * Get server by name
     */
    fun getServer(name: String): RegisteredServer? {
        return plugin.serverManager.getServer(name)
    }

    /**
     * Get all registered servers
     */
    fun getAllServers(): Collection<RegisteredServer> {
        return plugin.serverManager.getAllServers()
    }

    /**
     * Get servers in group
     */
    fun getServersInGroup(groupName: String): List<RegisteredServer> {
        return plugin.serverManager.getServersInGroup(groupName)
    }

    /**
     * Check if server exists
     */
    fun serverExists(name: String): Boolean {
        return plugin.serverManager.serverExists(name)
    }

    /**
     * Check if server is online
     */
    fun isServerOnline(name: String): Boolean {
        return plugin.serverManager.isServerOnline(name)
    }

    /**
     * Get players on server
     */
    fun getPlayersOnServer(serverName: String): List<Player> {
        return plugin.serverManager.getServer(serverName)
            ?.playersConnected?.toList()
            ?: emptyList()
    }

    /**
     * Get player count on server
     */
    fun getPlayerCountOnServer(serverName: String): Int {
        return getPlayersOnServer(serverName).size
    }

    /**
     * Get total player count across all servers
     */
    fun getTotalPlayerCount(): Int {
        return plugin.serverManager.getTotalPlayerCount()
    }

    // ========== Redis Operations ==========

    /**
     * Publish message to Redis channel
     */
    fun publishRedisMessage(channel: String, message: String): Boolean {
        return plugin.redisManager?.publish(channel, message) ?: false
    }

    /**
     * Subscribe to Redis channel
     */
    fun subscribeToRedis(channel: String, onMessage: (String, String) -> Unit) {
        plugin.redisManager?.subscribe(channel, object : org.elysian.velocity.managers.RedisManager.RedisSubscriber {
            override fun onMessage(ch: String, message: String) {
                onMessage(ch, message)
            }
        })
    }

    /**
     * Set Redis key-value
     */
    fun setRedisValue(key: String, value: String): Boolean {
        return plugin.redisManager?.set(key, value) ?: false
    }

    /**
     * Get Redis value
     */
    fun getRedisValue(key: String): String? {
        return plugin.redisManager?.get(key)
    }

    /**
     * Check if Redis is enabled
     */
    fun isRedisEnabled(): Boolean {
        return plugin.redisManager?.isConnected() ?: false
    }

    // ========== Player Data ==========

    /**
     * Get custom player data
     */
    fun getCustomPlayerData(uuid: UUID, key: String): Any? {
        return plugin.playerManager.getCustomData(uuid, key)
    }

    /**
     * Set custom player data
     */
    fun setCustomPlayerData(uuid: UUID, key: String, value: Any) {
        plugin.playerManager.setCustomData(uuid, key, value)
    }

    /**
     * Remove custom player data
     */
    fun removeCustomPlayerData(uuid: UUID, key: String) {
        plugin.playerManager.removeCustomData(uuid, key)
    }

    // ========== Utility ==========

    /**
     * Get formatted message from config
     */
    fun getMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        return plugin.configManager.getMessage(key, placeholders)
    }

    /**
     * Send formatted message to player
     */
    fun sendMessage(player: Player, key: String, placeholders: Map<String, String> = emptyMap()) {
        val message = getMessage(key, placeholders)
        plugin.playerManager.sendMessage(player, message)
    }

    /**
     * Broadcast formatted message to all players
     */
    fun broadcastMessage(key: String, placeholders: Map<String, String> = emptyMap()) {
        val message = getMessage(key, placeholders)
        plugin.playerManager.broadcast(message)
    }

    /**
     * Get plugin version
     */
    fun getVersion(): String {
        return "1.0-SNAPSHOT"
    }

    /**
     * Teleport result enum
     */
    sealed class TeleportResult {
        object SUCCESS : TeleportResult()
        object FAILED : TeleportResult()
        object SERVER_NOT_FOUND : TeleportResult()
        object ALREADY_CONNECTED : TeleportResult()
        data class COOLDOWN(val remainingSeconds: Long) : TeleportResult()
    }
}