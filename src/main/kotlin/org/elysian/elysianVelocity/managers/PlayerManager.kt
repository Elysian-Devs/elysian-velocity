package org.elysian.velocity.managers

import com.velocitypowered.api.proxy.Player
import org.elysian.velocity.ElysianVelocity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages player data and cross-server operations
 * Provides caching and utilities for player management
 */
class PlayerManager(private val plugin: ElysianVelocity) {

    // Player cache: UUID -> PlayerData
    private val playerCache: MutableMap<UUID, PlayerData> = ConcurrentHashMap()

    // Teleport cooldowns: UUID -> expiry timestamp
    private val teleportCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    // Pending teleports: UUID -> PendingTeleport
    private val pendingTeleports: MutableMap<UUID, PendingTeleport> = ConcurrentHashMap()

    init {
        startCleanupTask()
    }

    /**
     * Get player by UUID
     */
    fun getPlayer(uuid: UUID): Player? {
        return plugin.server.getPlayer(uuid).orElse(null)
    }

    /**
     * Get player by name
     */
    fun getPlayer(name: String): Player? {
        return plugin.server.getPlayer(name).orElse(null)
    }

    /**
     * Get or create player data
     */
    fun getPlayerData(uuid: UUID): PlayerData {
        return playerCache.getOrPut(uuid) {
            PlayerData(
                uuid = uuid,
                lastServer = null,
                customData = mutableMapOf(),
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * Update player data
     */
    fun updatePlayerData(uuid: UUID, data: PlayerData) {
        playerCache[uuid] = data.copy(lastUpdated = System.currentTimeMillis())
    }

    /**
     * Remove player data from cache
     */
    fun removePlayerData(uuid: UUID) {
        playerCache.remove(uuid)
    }

    /**
     * Get player's current server name
     */
    fun getPlayerServer(player: Player): String? {
        return player.currentServer.orElse(null)?.serverInfo?.name
    }

    /**
     * Check if player has teleport cooldown
     */
    fun hasTeleportCooldown(uuid: UUID): Boolean {
        val expiryTime = teleportCooldowns[uuid] ?: return false

        if (System.currentTimeMillis() >= expiryTime) {
            teleportCooldowns.remove(uuid)
            return false
        }

        return true
    }

    /**
     * Get remaining teleport cooldown in seconds
     */
    fun getRemainingCooldown(uuid: UUID): Long {
        val expiryTime = teleportCooldowns[uuid] ?: return 0
        val remaining = (expiryTime - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0)
    }

    /**
     * Set teleport cooldown
     */
    fun setTeleportCooldown(uuid: UUID, seconds: Int) {
        if (seconds > 0) {
            val expiryTime = System.currentTimeMillis() + (seconds * 1000L)
            teleportCooldowns[uuid] = expiryTime
        }
    }

    /**
     * Remove teleport cooldown
     */
    fun removeTeleportCooldown(uuid: UUID) {
        teleportCooldowns.remove(uuid)
    }

    /**
     * Create pending teleport
     */
    fun createPendingTeleport(
        uuid: UUID,
        targetServer: String,
        delay: Int,
        cancelOnMove: Boolean
    ): PendingTeleport {
        val pending = PendingTeleport(
            uuid = uuid,
            targetServer = targetServer,
            startTime = System.currentTimeMillis(),
            delay = delay,
            cancelOnMove = cancelOnMove
        )

        pendingTeleports[uuid] = pending
        return pending
    }

    /**
     * Get pending teleport
     */
    fun getPendingTeleport(uuid: UUID): PendingTeleport? {
        return pendingTeleports[uuid]
    }

    /**
     * Cancel pending teleport
     */
    fun cancelPendingTeleport(uuid: UUID): Boolean {
        return pendingTeleports.remove(uuid) != null
    }

    /**
     * Complete pending teleport
     */
    fun completePendingTeleport(uuid: UUID) {
        pendingTeleports.remove(uuid)
    }

    /**
     * Get all online players
     */
    fun getOnlinePlayers(): Collection<Player> {
        return plugin.server.allPlayers
    }

    /**
     * Get online player count
     */
    fun getOnlinePlayerCount(): Int {
        return plugin.server.playerCount
    }

    /**
     * Broadcast message to all players
     */
    fun broadcast(message: String) {
        getOnlinePlayers().forEach { player ->
            player.sendMessage(net.kyori.adventure.text.Component.text(message))
        }
    }

    /**
     * Send message to player
     */
    fun sendMessage(player: Player, message: String) {
        player.sendMessage(net.kyori.adventure.text.Component.text(message))
    }

    /**
     * Get custom player data
     */
    fun getCustomData(uuid: UUID, key: String): Any? {
        return getPlayerData(uuid).customData[key]
    }

    /**
     * Set custom player data
     */
    fun setCustomData(uuid: UUID, key: String, value: Any) {
        val data = getPlayerData(uuid)
        data.customData[key] = value
        updatePlayerData(uuid, data)
    }

    /**
     * Remove custom player data
     */
    fun removeCustomData(uuid: UUID, key: String) {
        val data = getPlayerData(uuid)
        data.customData.remove(key)
        updatePlayerData(uuid, data)
    }

    /**
     * Clear all cache
     */
    fun clearCache() {
        playerCache.clear()
        teleportCooldowns.clear()
        pendingTeleports.clear()
    }

    /**
     * Start cleanup task for expired data
     */
    private fun startCleanupTask() {
        plugin.server.scheduler.buildTask(plugin) {
            val now = System.currentTimeMillis()
            val cacheExpireTime = plugin.configManager.getLong("performance.player-cache-expire", 300) * 1000

            // Clean up expired player cache
            playerCache.entries.removeIf { (_, data) ->
                now - data.lastUpdated > cacheExpireTime
            }

            // Clean up expired cooldowns
            teleportCooldowns.entries.removeIf { (_, expiryTime) ->
                now >= expiryTime
            }

            // Clean up expired pending teleports (older than 1 minute)
            pendingTeleports.entries.removeIf { (_, pending) ->
                now - pending.startTime > 60000
            }

            if (plugin.configManager.getBoolean("general.debug", false)) {
                plugin.logger.info("Cleanup: ${playerCache.size} cached players, " +
                        "${teleportCooldowns.size} cooldowns, " +
                        "${pendingTeleports.size} pending teleports")
            }

        }.repeat(1, TimeUnit.MINUTES).schedule()
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            cachedPlayers = playerCache.size,
            activeCooldowns = teleportCooldowns.size,
            pendingTeleports = pendingTeleports.size
        )
    }

    /**
     * Player data class
     */
    data class PlayerData(
        val uuid: UUID,
        var lastServer: String?,
        val customData: MutableMap<String, Any>,
        var lastUpdated: Long
    )

    /**
     * Pending teleport data class
     */
    data class PendingTeleport(
        val uuid: UUID,
        val targetServer: String,
        val startTime: Long,
        val delay: Int,
        val cancelOnMove: Boolean,
        var cancelled: Boolean = false
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - startTime > (delay * 1000L)
        }

        fun getRemainingSeconds(): Int {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            return (delay - elapsed).toInt().coerceAtLeast(0)
        }
    }

    /**
     * Cache statistics data class
     */
    data class CacheStats(
        val cachedPlayers: Int,
        val activeCooldowns: Int,
        val pendingTeleports: Int
    )
}