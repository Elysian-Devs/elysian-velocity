package org.elysian.velocity.managers

import com.velocitypowered.api.proxy.server.RegisteredServer
import org.elysian.velocity.ElysianVelocity
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages server information and groups
 * Provides utilities for server operations
 */
class ServerManager(private val plugin: ElysianVelocity) {

    private val serverGroups: MutableMap<String, MutableList<String>> = ConcurrentHashMap()
    private val serverStatus: MutableMap<String, ServerStatus> = ConcurrentHashMap()

    init {
        loadServerGroups()
        startStatusMonitor()
    }

    /**
     * Load server groups from configuration
     */
    private fun loadServerGroups() {
        serverGroups.clear()

        val groupsConfig = plugin.configManager.getMap("server-groups")
        groupsConfig.forEach { (groupName, servers) ->
            if (servers is List<*>) {
                val serverList = servers.mapNotNull { it as? String }.toMutableList()
                serverGroups[groupName] = serverList
            }
        }

        plugin.logger.info("Loaded ${serverGroups.size} server groups")
    }

    /**
     * Get server by name
     */
    fun getServer(name: String): RegisteredServer? {
        return plugin.server.getServer(name).orElse(null)
    }

    /**
     * Get all registered servers
     */
    fun getAllServers(): Collection<RegisteredServer> {
        return plugin.server.allServers
    }

    /**
     * Get servers in a specific group
     */
    fun getServersInGroup(groupName: String): List<RegisteredServer> {
        val serverNames = serverGroups[groupName] ?: return emptyList()
        return serverNames.mapNotNull { getServer(it) }
    }

    /**
     * Get group name for a server
     */
    fun getGroupForServer(serverName: String): String? {
        return serverGroups.entries
            .firstOrNull { (_, servers) -> serverName in servers }
            ?.key
    }

    /**
     * Check if server exists
     */
    fun serverExists(name: String): Boolean {
        return plugin.server.getServer(name).isPresent
    }

    /**
     * Check if server is online (has players or is reachable)
     */
    fun isServerOnline(name: String): Boolean {
        val server = getServer(name) ?: return false

        // Check cache first
        val status = serverStatus[name]
        if (status != null && System.currentTimeMillis() - status.lastCheck < 30000) {
            return status.online
        }

        // Simple check: if server has players, it's online
        val isOnline = server.playersConnected.isNotEmpty()

        // Update cache
        serverStatus[name] = ServerStatus(
            online = isOnline,
            lastCheck = System.currentTimeMillis()
        )

        return isOnline
    }

    /**
     * Get hub server
     */
    fun getHubServer(): RegisteredServer? {
        val hubServerName = plugin.configManager.getString("general.hub-server", "lobby")
        return getServer(hubServerName)
    }

    /**
     * Get fallback servers in priority order
     */
    fun getFallbackServers(): List<RegisteredServer> {
        val fallbackNames = plugin.configManager.getStringList("general.fallback-servers")
        return fallbackNames.mapNotNull { getServer(it) }
    }

    /**
     * Find best server in group (least players)
     */
    fun getBestServerInGroup(groupName: String): RegisteredServer? {
        return getServersInGroup(groupName)
            .filter { it.playersConnected.isNotEmpty() || isServerOnline(it.serverInfo.name) }
            .minByOrNull { it.playersConnected.size }
    }

    /**
     * Get server statistics
     */
    fun getServerStats(serverName: String): ServerStats? {
        val server = getServer(serverName) ?: return null

        return ServerStats(
            name = serverName,
            playerCount = server.playersConnected.size,
            online = isServerOnline(serverName),
            group = getGroupForServer(serverName)
        )
    }

    /**
     * Get all server statistics
     */
    fun getAllServerStats(): Map<String, ServerStats> {
        return getAllServers().associate { server ->
            val name = server.serverInfo.name
            name to (getServerStats(name) ?: ServerStats(
                name = name,
                playerCount = 0,
                online = false,
                group = null
            ))
        }
    }

    /**
     * Get total player count across all servers
     */
    fun getTotalPlayerCount(): Int {
        return getAllServers().sumOf { it.playersConnected.size }
    }

    /**
     * Get player count in group
     */
    fun getGroupPlayerCount(groupName: String): Int {
        return getServersInGroup(groupName).sumOf { it.playersConnected.size }
    }

    /**
     * Add server to group
     */
    fun addServerToGroup(groupName: String, serverName: String) {
        val group = serverGroups.getOrPut(groupName) { mutableListOf() }
        if (!group.contains(serverName)) {
            group.add(serverName)
        }
    }

    /**
     * Remove server from group
     */
    fun removeServerFromGroup(groupName: String, serverName: String) {
        serverGroups[groupName]?.remove(serverName)
    }

    /**
     * Start monitoring server status
     */
    private fun startStatusMonitor() {
        // Clear old status entries every 5 minutes
        plugin.server.scheduler.buildTask(plugin) {
            val now = System.currentTimeMillis()
            serverStatus.entries.removeIf { (_, status) ->
                now - status.lastCheck > 300000 // 5 minutes
            }
        }.repeat(java.time.Duration.ofMinutes(5)).schedule()
    }

    /**
     * Clear status cache
     */
    fun clearStatusCache() {
        serverStatus.clear()
    }

    /**
     * Server status data class
     */
    data class ServerStatus(
        val online: Boolean,
        val lastCheck: Long
    )

    /**
     * Server statistics data class
     */
    data class ServerStats(
        val name: String,
        val playerCount: Int,
        val online: Boolean,
        val group: String?
    )
}