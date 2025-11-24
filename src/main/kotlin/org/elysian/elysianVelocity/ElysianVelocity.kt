package org.elysian.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.elysian.velocity.api.VelocityAPI
import org.elysian.velocity.commands.HubCommand
import org.elysian.velocity.commands.ServerCommand
import org.elysian.velocity.managers.*
import org.elysian.velocity.messaging.MessageHandler
import org.elysian.velocity.messaging.PluginMessageListener
import org.slf4j.Logger
import java.nio.file.Path

/**
 * ElysianVelocity - Bridge plugin for Elysian network
 *
 * Main plugin class that initializes all managers and services.
 * Provides cross-server communication and API for other plugins.
 *
 * @author nvtmre
 */
@Plugin(
    id = "elysianvelocity",
    name = "ElysianVelocity",
    version = "1.0-SNAPSHOT",
    description = "Bridge plugin for Elysian network",
    authors = ["nvtmre"]
)
class ElysianVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) {

    // Managers
    lateinit var configManager: ConfigManager
        private set

    lateinit var serverManager: ServerManager
        private set

    lateinit var playerManager: PlayerManager
        private set

    lateinit var messageHandler: MessageHandler
        private set

    var redisManager: RedisManager? = null
        private set

    // Public API
    lateinit var api: VelocityAPI
        private set

    companion object {
        lateinit var instance: ElysianVelocity
            private set
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        instance = this
        val startTime = System.currentTimeMillis()

        logger.info("========================================")
        logger.info("  Initializing ElysianVelocity...")
        logger.info("========================================")

        try {
            // Load configuration
            configManager = ConfigManager(this, dataDirectory)
            configManager.load()
            logger.info("✓ Configuration loaded")

            // Initialize server manager
            serverManager = ServerManager(this)
            logger.info("✓ Server manager initialized")

            // Initialize player manager
            playerManager = PlayerManager(this)
            logger.info("✓ Player manager initialized")

            // Initialize Redis if enabled
            if (configManager.getBoolean("redis.enabled", false)) {
                try {
                    redisManager = RedisManager(this)
                    redisManager?.connect()
                    logger.info("✓ Redis connected")
                } catch (e: Exception) {
                    logger.warn("✗ Failed to connect to Redis: ${e.message}")
                    logger.warn("  Continuing without Redis support")
                }
            } else {
                logger.info("ℹ Redis disabled in configuration")
            }

            // Initialize message handler
            messageHandler = MessageHandler(this)
            logger.info("✓ Message handler initialized")

            // Register plugin message listener
            val pluginMessageListener = PluginMessageListener(this)
            server.channelRegistrar.register(pluginMessageListener.CHANNEL_IDENTIFIER)
            server.eventManager.register(this, pluginMessageListener)
            logger.info("✓ Plugin messaging registered")

            // Initialize public API
            api = VelocityAPI(this)
            logger.info("✓ Public API initialized")

            // Register commands
            registerCommands()
            logger.info("✓ Commands registered")

            val loadTime = System.currentTimeMillis() - startTime
            logger.info("")
            logger.info("========================================")
            logger.info("  ElysianVelocity v1.0 Enabled!")
            logger.info("  Loaded in ${loadTime}ms")
            logger.info("  Servers: ${server.allServers.size}")
            logger.info("  Redis: ${if (redisManager != null) "Enabled" else "Disabled"}")
            logger.info("========================================")
            logger.info("")

        } catch (e: Exception) {
            logger.error("========================================")
            logger.error("  FAILED TO ENABLE ELYSIANVELOCITY!")
            logger.error("  Error: ${e.message}")
            logger.error("========================================")
            e.printStackTrace()
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("Shutting down ElysianVelocity...")

        try {
            // Disconnect Redis
            redisManager?.disconnect()

            // Clear caches
            playerManager.clearCache()

            logger.info("✓ ElysianVelocity disabled successfully")

        } catch (e: Exception) {
            logger.error("Error during shutdown: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Register all plugin commands
     */
    private fun registerCommands() {
        val commandManager = server.commandManager

        // Register /hub command
        val hubMeta = commandManager.metaBuilder("hub")
            .aliases("lobby")
            .build()
        commandManager.register(hubMeta, HubCommand(this))

        // Register /server command
        val serverMeta = commandManager.metaBuilder("server")
            .build()
        commandManager.register(serverMeta, ServerCommand(this))
    }

    /**
     * Reload configuration
     */
    fun reload() {
        try {
            configManager.reload()
            logger.info("✓ Configuration reloaded")
        } catch (e: Exception) {
            logger.error("✗ Failed to reload configuration: ${e.message}")
            e.printStackTrace()
        }
    }
}