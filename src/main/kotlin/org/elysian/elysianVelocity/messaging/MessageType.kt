package org.elysian.velocity.messaging

/**
 * Message types for plugin messaging protocol
 * Defines all supported cross-server message types
 */
enum class MessageType {
    // Player operations
    PLAYER_CONNECT,
    PLAYER_DISCONNECT,
    PLAYER_TELEPORT,
    PLAYER_DATA_UPDATE,
    PLAYER_DATA_REQUEST,

    // Server operations
    SERVER_STATUS,
    SERVER_BROADCAST,
    SERVER_COMMAND,

    // Data synchronization
    DATA_SYNC,
    DATA_REQUEST,
    DATA_RESPONSE,

    // Teleportation
    TELEPORT_REQUEST,
    TELEPORT_RESPONSE,
    TELEPORT_PLAYER,
    TELEPORT_CANCEL,

    // General
    PING,
    PONG,
    HEARTBEAT,

    // Custom
    CUSTOM;

    companion object {
        /**
         * Get MessageType from string
         */
        fun fromString(value: String): MessageType? {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}