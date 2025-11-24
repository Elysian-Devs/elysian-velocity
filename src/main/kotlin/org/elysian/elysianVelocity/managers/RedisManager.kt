package org.elysian.velocity.managers

import org.elysian.velocity.ElysianVelocity
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Redis connections for cross-server communication
 * Optional feature for real-time data synchronization
 */
class RedisManager(private val plugin: ElysianVelocity) {
    private var jedisPool: JedisPool? = null
    private var pubSubThread: Thread? = null
    private val subscribers: MutableMap<String, MutableList<RedisSubscriber>> = ConcurrentHashMap()
    private var connected = false

    /**
     * Connect to Redis server
     */
    fun connect() {
        if (connected) {
            plugin.logger.warn("Redis is already connected!")
            return
        }

        try {
            val host = plugin.configManager.getString("redis.host", "localhost")
            val port = plugin.configManager.getInt("redis.port", 6379)
            val password = plugin.configManager.getString("redis.password", "")

            val poolConfig = JedisPoolConfig().apply {
                maxTotal = plugin.configManager.getInt("redis.pool.max-total", 20)
                maxIdle = plugin.configManager.getInt("redis.pool.max-idle", 10)
                minIdle = plugin.configManager.getInt("redis.pool.min-idle", 5)
                testOnBorrow = true
                testOnReturn = true
                testWhileIdle = true
            }

            jedisPool = if (password.isNotEmpty()) {
                JedisPool(poolConfig, host, port, 2000, password)
            } else {
                JedisPool(poolConfig, host, port, 2000)
            }

            // Test connection
            getResource().use { jedis ->
                jedis.ping()
            }

            connected = true
            plugin.logger.info("✓ Redis connected to $host:$port")

            // Start pub/sub listener
            startPubSubListener()

        } catch (e: Exception) {
            plugin.logger.error("✗ Failed to connect to Redis: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Disconnect from Redis
     */
    fun disconnect() {
        if (!connected) return

        try {
            // Stop pub/sub thread
            pubSubThread?.interrupt()
            pubSubThread = null

            // Close pool
            jedisPool?.close()
            jedisPool = null

            connected = false
            plugin.logger.info("✓ Redis disconnected")

        } catch (e: Exception) {
            plugin.logger.error("✗ Error disconnecting from Redis: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Get Jedis resource from pool
     */
    fun getResource(): Jedis {
        if (!connected || jedisPool == null) {
            throw IllegalStateException("Redis is not connected!")
        }
        return jedisPool!!.resource
    }

    /**
     * Publish message to Redis channel
     */
    fun publish(channel: String, message: String): Boolean {
        if (!connected) return false

        return try {
            getResource().use { jedis ->
                jedis.publish(channel, message)
            }
            true
        } catch (e: Exception) {
            plugin.logger.error("Failed to publish to Redis channel '$channel': ${e.message}")
            false
        }
    }

    /**
     * Subscribe to Redis channel
     */
    fun subscribe(channel: String, subscriber: RedisSubscriber) {
        subscribers.getOrPut(channel) { mutableListOf() }.add(subscriber)

        if (plugin.configManager.getBoolean("general.debug", false)) {
            plugin.logger.info("Subscribed to Redis channel: $channel")
        }
    }

    /**
     * Unsubscribe from Redis channel
     */
    fun unsubscribe(channel: String, subscriber: RedisSubscriber) {
        subscribers[channel]?.remove(subscriber)

        if (plugin.configManager.getBoolean("general.debug", false)) {
            plugin.logger.info("Unsubscribed from Redis channel: $channel")
        }
    }

    /**
     * Set key-value pair
     */
    fun set(key: String, value: String): Boolean {
        if (!connected) return false

        return try {
            getResource().use { jedis ->
                jedis.set(key, value)
            }
            true
        } catch (e: Exception) {
            plugin.logger.error("Failed to set Redis key '$key': ${e.message}")
            false
        }
    }

    /**
     * Set key-value pair with expiration
     */
    fun setex(key: String, seconds: Int, value: String): Boolean {
        if (!connected) return false

        return try {
            getResource().use { jedis ->
                jedis.setex(key, seconds.toLong(), value)
            }
            true
        } catch (e: Exception) {
            plugin.logger.error("Failed to set Redis key '$key' with expiration: ${e.message}")
            false
        }
    }

    /**
     * Get value by key
     */
    fun get(key: String): String? {
        if (!connected) return null

        return try {
            getResource().use { jedis ->
                jedis.get(key)
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to get Redis key '$key': ${e.message}")
            null
        }
    }

    /**
     * Delete key
     */
    fun delete(key: String): Boolean {
        if (!connected) return false

        return try {
            getResource().use { jedis ->
                jedis.del(key) > 0
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to delete Redis key '$key': ${e.message}")
            false
        }
    }

    /**
     * Check if key exists
     */
    fun exists(key: String): Boolean {
        if (!connected) return false

        return try {
            getResource().use { jedis ->
                jedis.exists(key)
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to check Redis key '$key': ${e.message}")
            false
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = connected

    /**
     * Start pub/sub listener thread
     */
    private fun startPubSubListener() {
        if (subscribers.isEmpty()) return

        pubSubThread = Thread({
            try {
                getResource().use { jedis ->
                    val channelsArray = subscribers.keys.toTypedArray()

                    val jedisPubSub = object : JedisPubSub() {
                        override fun onMessage(channel: String, message: String) {
                            subscribers[channel]?.forEach { subscriber ->
                                try {
                                    subscriber.onMessage(channel, message)
                                } catch (e: Exception) {
                                    plugin.logger.error("Error in Redis subscriber for channel '$channel': ${e.message}")
                                }
                            }
                        }
                    }

                    jedis.subscribe(jedisPubSub, *channelsArray)
                }
            } catch (e: InterruptedException) {
                plugin.logger.info("Redis pub/sub listener stopped")
            } catch (e: Exception) {
                plugin.logger.error("Error in Redis pub/sub listener: ${e.message}")
                e.printStackTrace()
            }
        }, "ElysianVelocity-RedisPubSub")

        pubSubThread?.isDaemon = true
        pubSubThread?.start()

        plugin.logger.info("✓ Redis pub/sub listener started for ${subscribers.size} channels")
    }

    /**
     * Restart pub/sub listener
     */
    fun restartPubSubListener() {
        pubSubThread?.interrupt()
        pubSubThread = null
        startPubSubListener()
    }

    /**
     * Get connection statistics
     */
    fun getStats(): RedisStats {
        return if (connected && jedisPool != null) {
            RedisStats(
                connected = true,
                activeConnections = jedisPool!!.numActive,
                idleConnections = jedisPool!!.numIdle,
                subscribedChannels = subscribers.size
            )
        } else {
            RedisStats(
                connected = false,
                activeConnections = 0,
                idleConnections = 0,
                subscribedChannels = 0
            )
        }
    }

    /**
     * Redis subscriber interface
     */
    interface RedisSubscriber {
        fun onMessage(channel: String, message: String)
    }

    /**
     * Redis statistics data class
     */
    data class RedisStats(
        val connected: Boolean,
        val activeConnections: Int,
        val idleConnections: Int,
        val subscribedChannels: Int
    )
}