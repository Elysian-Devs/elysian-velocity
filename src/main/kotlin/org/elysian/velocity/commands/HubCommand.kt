package org.elysian.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import org.elysian.velocity.ElysianVelocity

/**
 * /hub command - Teleport to hub/lobby server
 * Aliases: /lobby
 */
class HubCommand(private val plugin: ElysianVelocity) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()

        // Only players can use this command
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command!"))
            return
        }

        val player = source

        // Get hub server
        val hubServer = plugin.serverManager.getHubServer()
        if (hubServer == null) {
            val message = plugin.configManager.getMessage("hub-not-configured", emptyMap())
            player.sendMessage(Component.text(message))
            return
        }

        // Check if already on hub
        val currentServer = player.currentServer.orElse(null)?.server
        if (currentServer?.serverInfo?.name == hubServer.serverInfo.name) {
            val message = plugin.configManager.getMessage(
                "already-connected",
                mapOf("server" to hubServer.serverInfo.name)
            )
            player.sendMessage(Component.text(message))
            return
        }

        // Send teleporting message
        val teleportingMessage = plugin.configManager.getMessage("teleporting-hub", emptyMap())
        player.sendMessage(Component.text(teleportingMessage))

        // Teleport player
        player.createConnectionRequest(hubServer)
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

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return true // Everyone can use /hub
    }
}