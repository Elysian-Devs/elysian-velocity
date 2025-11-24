# ElysianVelocity

<div align="center">

![Velocity](https://img.shields.io/badge/Velocity-3.3.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Professional bridge plugin for Velocity proxy networks**

Provides cross-server communication, player management, teleportation, and Redis integration.

[Features](#-features) • [Installation](#-installation) • [Configuration](#%EF%B8%8F-configuration) • [API Usage](#-api-usage) • [Network Setup](#-network-setup)

</div>

---

## 📋 Features

### 🔌 **Cross-Server Communication**
- **Plugin messaging** between backend servers
- **Request/Response pattern** for data exchange
- **Message handlers** with predefined types
- **Broadcast support** to all servers
- Thread-safe message queue

### 👥 **Player Management**
- **Player data caching** with automatic expiration
- **Custom player data** storage (unlimited key-value pairs)
- **Teleport cooldowns** with configurable times
- **Pending teleports** with delay and cancel-on-move
- **Lazy loading** - data loaded only when needed

### 🎯 **Server Management**
- **Server groups** for easy organization
- **Status monitoring** with caching
- **Server statistics** (player count, online status)
- **Fallback servers** for connection failures
- Automatic cleanup of expired data

### 🔄 **Redis Support** (Optional)
- **Pub/Sub messaging** for real-time communication
- **Key-value storage** for shared data
- **Connection pooling** with HikariCP
- **Channel subscriptions** with custom handlers
- Automatic reconnection on failure

### 📝 **Commands**
- `/hub` (`/lobby`) - Teleport to hub/lobby server
- `/server <name>` - Switch between servers
- Tab completion for server names
- Permission support

### 🛠️ **Developer API**
- **VelocityAPI** - Main API for all operations
- **MessagingAPI** - Dedicated messaging system
- **CompletableFuture** support for async operations
- Full Kotlin coroutines compatibility
- Comprehensive JavaDocs

---

## 📥 Installation

### Requirements
- **Velocity**: 3.3.0 or higher
- **Java**: 21 or higher
- **Backend**: Paper/Purpur servers with [ElysianCore](https://github.com/Elysian-Devs/elysian-core)
- **Redis**: Optional (for advanced features)

### Steps

1. **Download** the latest release from [Releases](https://github.com/Elysian-Devs/elysian-velocity/releases)

2. **Place** `ElysianVelocity.jar` in your Velocity `plugins/` folder

3. **Start** the proxy to generate configuration files

4. **Configure** `plugins/ElysianVelocity/config.yml` (see [Configuration](#%EF%B8%8F-configuration))

5. **Install** [ElysianCore](https://github.com/Elysian-Devs/elysian-core) on all backend Paper servers

6. **Restart** the proxy

---

## ⚙️ Configuration

### `config.yml`

```yaml
# General Settings
general:
  # Enable debug mode (verbose logging)
  debug: false
  
  # Default hub/lobby server
  hub-server: "lobby"
  
  # Fallback servers in priority order
  fallback-servers:
    - "lobby"
    - "hub"

# Redis Configuration (Optional)
redis:
  # Enable Redis for cross-server communication
  enabled: false
  
  # Redis server connection
  host: "localhost"
  port: 6379
  password: ""
  
  # Redis pub/sub channels
  channels:
    player-data: "elysian:playerdata"
    teleport: "elysian:teleport"
    broadcast: "elysian:broadcast"
  
  # Connection pool settings
  pool:
    max-total: 20
    max-idle: 10
    min-idle: 5

# Server Groups
server-groups:
  lobbies:
    - "lobby"
    - "hub"
  
  survival:
    - "survival-1"
    - "survival-2"
  
  creative:
    - "creative"
  
  minigames:
    - "bedwars"
    - "skywars"

# Teleportation Settings
teleport:
  # Enable teleportation features
  enabled: true
  
  # Cooldown between teleports (seconds)
  cooldown: 3
  
  # Delay before teleport (seconds)
  delay: 0
  
  # Cancel teleport if player moves
  cancel-on-move: true

# Plugin Messaging
messaging:
  # Plugin message channel identifier
  channel: "elysian:main"
  
  # Request timeout (milliseconds)
  timeout: 5000
  
  # Maximum message size (bytes)
  max-size: 16384

# Performance Settings
performance:
  # Player cache expiry time (seconds)
  player-cache-expire: 300
  
  # Maximum cache size
  max-cache-size: 10000
  
  # Message queue size
  message-queue-size: 1000

# Messages
messages:
  prefix: "&8[&bElysian&8] &7"
  
  server-not-found: "{prefix}&cServer not found: &f{server}"
  server-offline: "{prefix}&cServer &f{server}&c is currently offline"
  already-connected: "{prefix}&cYou are already connected to &f{server}"
  connecting: "{prefix}&aConnecting to &f{server}&a..."
  
  teleporting-hub: "{prefix}&aTeleporting to hub..."
  hub-not-configured: "{prefix}&cHub server is not configured"
  
  teleport-starting: "{prefix}&aTeleporting in &f{seconds}&a seconds..."
  teleport-cancelled: "{prefix}&cTeleport cancelled!"
  teleport-success: "{prefix}&aTeleported successfully!"
  teleport-failed: "{prefix}&cTeleport failed. Please try again."
  
  no-permission: "{prefix}&cYou don't have permission to do that!"
  player-not-found: "{prefix}&cPlayer not found: &f{player}"
  usage-server: "{prefix}&eUsage: /server <server-name>"
```

---

## 🔌 API Usage

### Adding ElysianVelocity as Dependency

#### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenLocal() // or your repository
}

dependencies {
    compileOnly("org.elysian:ElysianVelocity:1.0-SNAPSHOT")
}
```

#### velocity-plugin.json
```json
{
  "id": "yourplugin",
  "dependencies": [
    {
      "id": "elysianvelocity",
      "optional": false
    }
  ]
}
```

### Basic Usage

```kotlin
import org.elysian.velocity.ElysianVelocity
import org.elysian.velocity.api.VelocityAPI

class YourPlugin @Inject constructor(
    private val server: ProxyServer
) {
    private lateinit var elysian: ElysianVelocity
    private lateinit var api: VelocityAPI
    
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // Get ElysianVelocity instance
        elysian = ElysianVelocity.instance
        api = elysian.api
        
        // Now you can use the API!
        logger.info("ElysianVelocity API loaded: ${api.getVersion()}")
    }
}
```

---

## 📚 API Examples

### 1. Player Operations

```kotlin
// Get player by UUID
val player = api.getPlayer(uuid)

// Get player by name
val player = api.getPlayer("Steve")

// Get all online players
val onlinePlayers = api.getOnlinePlayers()

// Get player's current server
val serverName = api.getPlayerServerName(player)

// Get player count
val count = api.getOnlinePlayerCount()
```

### 2. Teleportation

```kotlin
// Simple teleportation
api.teleportPlayerToServer(player, "survival").thenAccept { success ->
    if (success) {
        player.sendMessage(Component.text("Teleported!"))
    }
}

// Teleportation with cooldown check
val result = api.teleportPlayerToServerWithCooldown(player, "survival").get()
when (result) {
    is VelocityAPI.TeleportResult.SUCCESS -> {
        player.sendMessage(Component.text("Teleported successfully!"))
    }
    is VelocityAPI.TeleportResult.COOLDOWN -> {
        val remaining = result.remainingSeconds
        player.sendMessage(Component.text("Wait ${remaining}s!"))
    }
    is VelocityAPI.TeleportResult.SERVER_NOT_FOUND -> {
        player.sendMessage(Component.text("Server not found!"))
    }
    else -> {
        player.sendMessage(Component.text("Teleport failed!"))
    }
}

// Teleport to hub
api.teleportPlayerToHub(player)
```

### 3. Server Management

```kotlin
// Get server by name
val server = api.getServer("survival")

// Get all servers
val allServers = api.getAllServers()

// Get servers in group
val lobbyServers = api.getServersInGroup("lobbies")

// Check if server exists
if (api.serverExists("survival")) {
    // Server registered
}

// Check if server is online
if (api.isServerOnline("survival")) {
    // Server has players
}

// Get players on server
val players = api.getPlayersOnServer("survival")

// Get player count on server
val count = api.getPlayerCountOnServer("survival")

// Get total player count
val totalPlayers = api.getTotalPlayerCount()
```

### 4. Cross-Server Messaging

```kotlin
val messaging = api.messaging

// Send message to specific server
messaging.sendToServer(server, "elysian:main", mapOf(
    "type" to "CUSTOM",
    "action" to "update_stats",
    "data" to mapOf("kills" to 10, "deaths" to 5)
)).thenAccept { success ->
    if (success) {
        logger.info("Message sent successfully!")
    }
}

// Broadcast to all servers
messaging.broadcastToAll("elysian:main", mapOf(
    "type" to "SERVER_BROADCAST",
    "message" to "Server restart in 5 minutes!"
)).thenAccept { count ->
    logger.info("Broadcast sent to $count servers")
}

// Request data from server
messaging.requestData(
    server,
    "player_stats",
    mapOf("uuid" to player.uniqueId.toString())
).thenAccept { response ->
    if (response != null) {
        val kills = response["kills"] as? Int ?: 0
        player.sendMessage(Component.text("Kills: $kills"))
    }
}

// Register custom message handler
messaging.registerHandler(MessageType.CUSTOM) { server, data ->
    val action = data["action"] as? String
    when (action) {
        "player_joined" -> {
            val playerName = data["player"] as? String
            logger.info("$playerName joined ${server.serverInfo.name}")
        }
        "player_left" -> {
            val playerName = data["player"] as? String
            logger.info("$playerName left ${server.serverInfo.name}")
        }
    }
}

// Get messaging statistics
val stats = messaging.getStats()
logger.info("Pending requests: ${stats.pendingRequests}")
logger.info("Registered handlers: ${stats.registeredHandlers}")
```

### 5. Redis Integration

```kotlin
// Check if Redis is enabled
if (api.isRedisEnabled()) {
    // Publish message to Redis channel
    api.publishRedisMessage("elysian:broadcast", "Server maintenance soon")
    
    // Subscribe to Redis channel
    api.subscribeToRedis("elysian:events") { channel, message ->
        logger.info("Received on $channel: $message")
        
        // Process message
        when (channel) {
            "elysian:events" -> {
                // Handle event
            }
        }
    }
    
    // Set Redis key-value
    api.setRedisValue("maintenance_mode", "true")
    
    // Get Redis value
    val isMaintenance = api.getRedisValue("maintenance_mode")
    if (isMaintenance == "true") {
        // Server in maintenance
    }
}
```

### 6. Player Data

```kotlin
// Set custom player data
api.setCustomPlayerData(uuid, "coins", 1000)
api.setCustomPlayerData(uuid, "level", 50)
api.setCustomPlayerData(uuid, "rank", "VIP")

// Get custom player data
val coins = api.getCustomPlayerData(uuid, "coins") as? Int ?: 0
val level = api.getCustomPlayerData(uuid, "level") as? Int ?: 1
val rank = api.getCustomPlayerData(uuid, "rank") as? String ?: "Default"

// Remove custom player data
api.removeCustomPlayerData(uuid, "temporary_buff")
```

### 7. Messages

```kotlin
// Get formatted message from config
val message = api.getMessage("teleport-success", mapOf(
    "server" to "survival"
))

// Send message to player
api.sendMessage(player, "connecting", mapOf(
    "server" to "survival"
))

// Broadcast to all players
api.broadcastMessage("server-restart", mapOf(
    "time" to "5 minutes"
))
```

---

## 🌐 Network Setup

### Setting Up Velocity Network with ElysianCore

For a complete network with shared data between all servers:

#### 1. **Network Architecture**

```
         ┌─────────────┐
         │   Players   │
         └──────┬──────┘
                │
         ┌──────▼──────┐
         │  Velocity   │ ◄── ElysianVelocity
         │   (Proxy)   │
         └──────┬──────┘
                │
    ┌───────────┼───────────┐
    │           │           │
┌───▼───┐  ┌───▼───┐  ┌───▼────┐
│Lobby  │  │Survival│ │Creative│
│       │  │        │ │        │
│Core   │  │ Core   │ │ Core   │  ◄── ElysianCore
└───┬───┘  └───┬───┘ └───┬────┘
    │          │         │
    └──────────┼─────────┘
               │
        ┌──────▼──────┐
        │   MySQL     │
        │  (Shared)   │
        └─────────────┘
               │
        ┌──────▼──────┐
        │    Redis    │
        │ (Optional)  │
        └─────────────┘
```

#### 2. **Install MySQL/PostgreSQL**

```bash
# MySQL
sudo apt install mysql-server
sudo mysql_secure_installation

# Create database
mysql -u root -p
CREATE DATABASE elysian_network;
CREATE USER 'elysian'@'%' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON elysian_network.* TO 'elysian'@'%';
FLUSH PRIVILEGES;
```

#### 3. **Install Redis** (Optional but Recommended)

```bash
# Install Redis
sudo apt install redis-server

# Configure Redis for network access
sudo nano /etc/redis/redis.conf

# Change:
bind 0.0.0.0
requirepass your_strong_password

# Restart Redis
sudo systemctl restart redis
```

#### 4. **Configure Velocity (ElysianVelocity)**

```yaml
# plugins/ElysianVelocity/config.yml
general:
  hub-server: "lobby"

redis:
  enabled: true
  host: "192.168.1.100"  # Redis server IP
  port: 6379
  password: "your_strong_password"

server-groups:
  lobbies:
    - "lobby-1"
    - "lobby-2"
  survival:
    - "survival"
  creative:
    - "creative"
```

#### 5. **Configure Backend Servers (ElysianCore)**

On **each** Paper server:

```yaml
# plugins/ElysianCore/config.yml
database:
  type: mysql
  mysql:
    host: "192.168.1.100"  # MySQL server IP
    port: 3306
    database: "elysian_network"
    username: "elysian"
    password: "strong_password"
```

#### 6. **Benefits**

- ✅ **Shared player data** across all servers
- ✅ **Real-time communication** between servers
- ✅ **Synchronized teleportation** with cooldowns
- ✅ **Cross-server messaging** for events
- ✅ **Redis pub/sub** for instant updates
- ✅ **No data loss** when switching servers

---

## 📊 Performance

- **Message queue** prevents message spam
- **Connection pooling** for Redis
- **Async operations** prevent proxy lag
- **Cache system** reduces network calls
- **Automatic cleanup** of expired data

### Benchmarks

| Operation | Time | Notes |
|-----------|------|-------|
| Send plugin message | 1-3ms | Non-blocking |
| Teleport player | 5-10ms | With cooldown check |
| Redis publish | 1-2ms | With connection pool |
| Cache lookup | < 0.1ms | In-memory |

---

## 🏗️ Building from Source

```bash
# Clone repository
git clone https://github.com/Elysian-Devs/elysian-velocity.git
cd elysian-velocity

# Build with Gradle
./gradlew shadowJar

# Output: build/libs/ElysianVelocity-1.0-SNAPSHOT.jar
```

### Development Environment

```bash
# Build and copy to test environment
./gradlew dev
```

---

## 📦 Message Types

The plugin supports the following message types for cross-server communication:

| Type | Description | Usage |
|------|-------------|-------|
| `PLAYER_CONNECT` | Player joins network | Auto-sent by backend |
| `PLAYER_DISCONNECT` | Player leaves network | Auto-sent by backend |
| `PLAYER_TELEPORT` | Player teleportation | Teleport API |
| `PLAYER_DATA_UPDATE` | Player data changed | Custom data updates |
| `PLAYER_DATA_REQUEST` | Request player data | Data synchronization |
| `SERVER_STATUS` | Server status update | Server monitoring |
| `SERVER_BROADCAST` | Broadcast message | Network announcements |
| `SERVER_COMMAND` | Execute command | Cross-server commands |
| `DATA_SYNC` | Data synchronization | Cache updates |
| `DATA_REQUEST` | Request data | API calls |
| `DATA_RESPONSE` | Response with data | API responses |
| `TELEPORT_REQUEST` | Request teleportation | Teleport system |
| `TELEPORT_RESPONSE` | Teleport result | Teleport feedback |
| `PING` / `PONG` | Health check | Connection monitoring |
| `HEARTBEAT` | Keep-alive | Connection health |
| `CUSTOM` | Custom messages | Plugin extensions |

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **Documentation**: [Wiki](https://github.com/Elysian-Devs/elysian-velocity/wiki)
- **Issues**: [Bug Reports](https://github.com/Elysian-Devs/elysian-velocity/issues)
- **ElysianCore**: [Backend Plugin](https://github.com/Elysian-Devs/elysian-core)
- **Velocity**: [Official Docs](https://docs.papermc.io/velocity)

---

## 💖 Support

If you find this plugin useful, consider:
- ⭐ **Starring** the repository
- 🐛 **Reporting bugs** you find
- 💡 **Suggesting features** you'd like
- 📖 **Contributing** to documentation

---

<div align="center">

**Made with ❤️ by [Elysian TEAM](https://github.com/Elysian-Devs)**

*ElysianVelocity - Connecting your Minecraft network*

</div>