package org.elysian.velocity.managers

import org.elysian.velocity.ElysianVelocity
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages plugin configuration
 * Handles loading, saving, and accessing config values
 */
class ConfigManager(
    private val plugin: ElysianVelocity,
    private val dataDirectory: Path
) {

    private var config: MutableMap<String, Any> = mutableMapOf()
    private val yaml = Yaml()
    private val configFile: File

    init {
        // Create data directory if it doesn't exist
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory)
        }

        configFile = dataDirectory.resolve("config.yml").toFile()
    }

    /**
     * Load configuration from file
     */
    fun load() {
        try {
            if (!configFile.exists()) {
                saveDefaultConfig()
            }

            FileInputStream(configFile).use { input ->
                val loadedConfig = yaml.load<Map<String, Any>>(input)
                config = loadedConfig?.toMutableMap() ?: mutableMapOf()
            }

            plugin.logger.info("Configuration loaded from: ${configFile.absolutePath}")

        } catch (e: Exception) {
            plugin.logger.error("Failed to load configuration: ${e.message}")
            e.printStackTrace()

            // Use default config
            config = getDefaultConfig()
        }
    }

    /**
     * Reload configuration from file
     */
    fun reload() {
        config.clear()
        load()
    }

    /**
     * Save current configuration to file
     */
    fun save() {
        try {
            FileWriter(configFile).use { writer ->
                yaml.dump(config, writer)
            }

            plugin.logger.info("Configuration saved")

        } catch (e: Exception) {
            plugin.logger.error("Failed to save configuration: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Save default configuration
     */
    private fun saveDefaultConfig() {
        try {
            config = getDefaultConfig()
            save()
            plugin.logger.info("Default configuration created")
        } catch (e: Exception) {
            plugin.logger.error("Failed to create default configuration: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Get string value from config
     */
    fun getString(path: String, default: String = ""): String {
        return getNestedValue(path) as? String ?: default
    }

    /**
     * Get integer value from config
     */
    fun getInt(path: String, default: Int = 0): Int {
        val value = getNestedValue(path)
        return when (value) {
            is Int -> value
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }

    /**
     * Get long value from config
     */
    fun getLong(path: String, default: Long = 0L): Long {
        val value = getNestedValue(path)
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is String -> value.toLongOrNull() ?: default
            else -> default
        }
    }

    /**
     * Get boolean value from config
     */
    fun getBoolean(path: String, default: Boolean = false): Boolean {
        return getNestedValue(path) as? Boolean ?: default
    }

    /**
     * Get string list from config
     */
    fun getStringList(path: String): List<String> {
        val value = getNestedValue(path)
        return when (value) {
            is List<*> -> value.mapNotNull { it as? String }
            else -> emptyList()
        }
    }

    /**
     * Get map from config
     */
    @Suppress("UNCHECKED_CAST")
    fun getMap(path: String): Map<String, Any> {
        return getNestedValue(path) as? Map<String, Any> ?: emptyMap()
    }

    /**
     * Set value in config
     */
    fun set(path: String, value: Any) {
        setNestedValue(path, value)
    }

    /**
     * Get nested value using dot notation (e.g., "redis.host")
     */
    private fun getNestedValue(path: String): Any? {
        val keys = path.split(".")
        var current: Any? = config

        for (key in keys) {
            current = when (current) {
                is Map<*, *> -> current[key]
                else -> return null
            }
        }

        return current
    }

    /**
     * Set nested value using dot notation
     */
    @Suppress("UNCHECKED_CAST")
    private fun setNestedValue(path: String, value: Any) {
        val keys = path.split(".")
        var current = config as MutableMap<String, Any>

        for (i in 0 until keys.size - 1) {
            val key = keys[i]
            if (current[key] !is MutableMap<*, *>) {
                current[key] = mutableMapOf<String, Any>()
            }
            current = current[key] as MutableMap<String, Any>
        }

        current[keys.last()] = value
    }

    /**
     * Get formatted message from config
     */
    fun getMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        var message = getString("messages.$key", key)

        // Replace prefix
        val prefix = getString("messages.prefix", "")
        message = message.replace("{prefix}", prefix)

        // Replace placeholders
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }

        // Color codes
        message = message.replace("&", "§")

        return message
    }

    /**
     * Get default configuration
     */
    private fun getDefaultConfig(): MutableMap<String, Any> {
        return mutableMapOf(
            "general" to mapOf(
                "debug" to false,
                "hub-server" to "lobby",
                "fallback-servers" to listOf("lobby", "hub")
            ),
            "redis" to mapOf(
                "enabled" to false,
                "host" to "localhost",
                "port" to 6379,
                "password" to "",
                "channels" to mapOf(
                    "player-data" to "elysian:playerdata",
                    "teleport" to "elysian:teleport",
                    "broadcast" to "elysian:broadcast"
                ),
                "pool" to mapOf(
                    "max-total" to 20,
                    "max-idle" to 10,
                    "min-idle" to 5
                )
            ),
            "server-groups" to mapOf(
                "lobbies" to listOf("lobby", "hub"),
                "survival" to listOf("survival"),
                "creative" to listOf("creative"),
                "minigames" to emptyList<String>()
            ),
            "teleport" to mapOf(
                "enabled" to true,
                "cooldown" to 3,
                "delay" to 0,
                "cancel-on-move" to true
            ),
            "messages" to mapOf(
                "prefix" to "§8[§bElysian§8] §7",
                "server-not-found" to "{prefix}§cServer not found: §f{server}",
                "server-offline" to "{prefix}§cServer §f{server}§c is currently offline",
                "already-connected" to "{prefix}§cYou are already connected to §f{server}",
                "connecting" to "{prefix}§aConnecting to §f{server}§a...",
                "teleporting-hub" to "{prefix}§aTeleporting to hub...",
                "hub-not-configured" to "{prefix}§cHub server is not configured",
                "teleport-starting" to "{prefix}§aTeleporting in §f{seconds}§a seconds...",
                "teleport-cancelled" to "{prefix}§cTeleport cancelled!",
                "teleport-success" to "{prefix}§aTeleported successfully!",
                "teleport-failed" to "{prefix}§cTeleport failed. Please try again.",
                "no-permission" to "{prefix}§cYou don't have permission to do that!",
                "player-not-found" to "{prefix}§cPlayer not found: §f{player}",
                "usage-server" to "{prefix}§eUsage: /server <server-name>"
            ),
            "messaging" to mapOf(
                "channel" to "elysian:main",
                "timeout" to 5000,
                "max-size" to 16384
            ),
            "performance" to mapOf(
                "player-cache-expire" to 300,
                "max-cache-size" to 10000,
                "message-queue-size" to 1000
            )
        )
    }
}