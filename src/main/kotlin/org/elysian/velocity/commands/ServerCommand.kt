package org.elysian.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import org.elysian.velocity.ElysianVelocity

/**
 * /server command - Switch between servers
 * Usage: /server <server-name>
 */
class ServerCommand(private val plugin: ElysianVelocity) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        // Only players can use this command
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command!"))
            return
        }

        val player = source

        // Show server list if no arguments
        if (args.isEmpty()) {
            showServerList(player)
            return
        }

        val targetServerName = args[0]

        // Get target server
        val targetServer = plugin.serverManager.getServer(targetServerName)
        if (targetServer == null) {
            val message = plugin.configManager.getMessage(
                "server-not-found",
                mapOf("server" to targetServerName)
            )
            player.sendMessage(Component.text(message))
            return
        }

        // Check if already on this server
        val currentServer = player.currentServer.orElse(null)?.server
        if (currentServer?.serverInfo?.name == targetServerName) {
            val message = plugin.configManager.getMessage(
                "already-connected",
                mapOf("server" to targetServerName)
            )
            player.sendMessage(Component.text(message))
            return
        }

        // Send connecting message
        val connectingMessage = plugin.configManager.getMessage(
            "connecting",
            mapOf("server" to targetServerName)
        )
        player.sendMessage(Component.text(connectingMessage))

        // Teleport player
        player.createConnectionRequest(targetServer)
            .connect()
            .thenAccept { result ->
                if (!result.isSuccessful) {
                    val failedMessage = plugin.configManager.getMessage(
                        "teleport-failed",
                        emptyMap()
                    )
                    player.sendMessage(Component.text(failedMessage))
                }
            }
    }

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val args = invocation.arguments()

        // Suggest server names
        if (args.isEmpty()) {
            return plugin.serverManager.getAllServers()
                .map { it.serverInfo.name }
                .sorted()
        }

        // Filter servers by partial name
        val partial = args[0].lowercase()
        return plugin.serverManager.getAllServers()
            .map { it.serverInfo.name }
            .filter { it.lowercase().startsWith(partial) }
            .sorted()
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return true // Everyone can use /server
    }

    /**
     * Show list of available servers
     */
    private fun showServerList(player: Player) {
        val prefix = plugin.configManager.getString("messages.prefix", "§8[§bElysian§8] §7")

        player.sendMessage(Component.text("${prefix}§eAvailable servers:"))

        val currentServerName = player.currentServer.orElse(null)?.serverInfo?.name

        plugin.serverManager.getAllServers().forEach { server ->
            val serverName = server.serverInfo.name
            val playerCount = server.playersConnected.size

            val indicator = if (serverName == currentServerName) {
                "§a▶"
            } else {
                "§7-"
            }

            val status = if (plugin.serverManager.isServerOnline(serverName)) {
                "§a✓"
            } else {
                "§c✗"
            }

            player.sendMessage(
                Component.text("  $indicator §f$serverName §7($playerCount players) $status")
            )
        }

        player.sendMessage(Component.text(""))
        player.sendMessage(Component.text("${prefix}§eUse: §f/server <name>"))
    }
}