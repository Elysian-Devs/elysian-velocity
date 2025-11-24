# ElysianVelocity

<div align="center">

![Velocity](https://img.shields.io/badge/Velocity-3.3.0-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple)
![Java](https://img.shields.io/badge/Java-21-orange)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Bridge plugin for Elysian network enabling cross-server communication**

Works seamlessly with [ElysianCore](https://github.com/Elysian-Devs/elysian-core) on Paper servers.

[Features](#-features) • [Installation](#-installation) • [Configuration](#%EF%B8%8F-configuration) • [API Usage](#-api-usage) • [Integration](#-integration-with-elysiancore)

</div>

---

## 📋 Features

### 🔄 **Cross-Server Communication**
- **Plugin Message System** - Bidirectional communication between Velocity and Paper servers
- **Redis Support** (optional) - Real-time pub/sub for instant updates
- **Message Queue** - Reliable message delivery with timeout handling
- **Request/Response Pattern** - Query data from specific servers

### 🌐 **Server Management**
- **Server Groups** - Organize servers by type (lobby, survival, minigames)
- **Smart Load Balancing** - Find best server in group by player count
- **Health Monitoring** - Track server status and connectivity
- **Fallback System** - Automatic fallback to available servers

### 🚀 **Teleportation System**
- **Cross-Server Teleports** - Move players between servers seamlessly
- **Cooldown Management** - Prevent teleport spam
- **Delayed Teleports** - Optional delay with cancel-on-move
- **Hub Command** - Quick return to lobby/hub server

### 🔌 **Public API**
- **Plugin Integration** - Easy API for other Velocity plugins
- **Custom Messages** - Send custom data between servers
- **Event Handlers** - Register handlers for specific message types
- **Player Data Sync** - Share player data across network

### 💾 **Optional Redis Integration**
- **Pub/Sub Messaging** - Broadcast to all servers instantly
- **Key-Value Storage** - Share data across network
- **Connection Pooling** - Optimized Redis connections
- **Automatic Reconnection** - Resilient connection handling

---

## 📥 Installation

### Requirements
- **Velocity**: 3.3.0 or higher
- **Java**: 21 or higher
- **ElysianCore**: Installed on all Paper servers
- **Redis**: Optional (for enhanced features)

### Steps

1. **Download** the latest release from [Releases](https://github.com/Elysian-Devs/elysian-velocity/releases)

2. **Place** `ElysianVelocity.jar` in your Velocity `plugins/` folder

3. **Start** Velocity to generate configuration files

4. **Configure** `plugins/ElysianVelocity/config.yml` (see [Configuration](#%EF%B8%8F-configuration))

5. **Restart** Velocity

---

## ⚙️ Configuration

### Basic Setup (No Redis)

```yaml
general:
  debug: false
  hub-server: "lobby"
  fallback-servers:
    - "lobby"
    - "hub"

redis:
  enabled: false

server-groups:
  lobbies:
    - "lobby"
  survival:
    - "survival"
  minigames:
    - "skywars"
    - "bedwars"

teleport:
  enabled: true
  cooldown: 3
  delay: 0
  cancel-on-move: true
```

### Advanced Setup (With Redis)

```yaml
redis:
  enabled: true
  host: "localhost"
  port: 6379
  password: ""

  channels:
    player-data: "elysian:playerdata"
    teleport: "elysian:teleport"
    broadcast: "elysian:broadcast"

  pool:
    max-total: 20
    max-idle: 10
    min-idle: 5
```

### Network Architecture

```
┌──────────────────────────────────────────────┐
│              Velocity Proxy                  │
│        (ElysianVelocity Plugin)             │
└───────────────┬──────────────────────────────┘
                │
                │  Plugin Messages / Redis
                │
    ┌───────────┼───────────┬──────────────┐
    │           │           │              │
┌───▼────┐  ┌──▼─────┐  ┌─▼──────┐  ┌───▼───────┐
│ Lobby  │  │Survival│  │Creative│  │  Skywars  │
│ Paper  │  │ Paper  │  │ Paper  │  │   Paper   │
│ Core   │  │ Core   │  │ Core   │  │   Core    │
└────────┘  └────────┘  └────────┘  └───────────┘
     │           │           │              │
     └───────────┴───────────┴──────────────┘
                      │
              ┌───────▼────────┐
              │  MySQL/Redis   │
              │ (Shared Data)  │
              └────────────────┘
```

---

## 🔌 API Usage

### For Velocity Plugins

```kotlin
import org.elysian.velocity.ElysianVelocity
import org.elysian.velocity.api.VelocityAPI

class MyVelocityPlugin {

    fun onEnable() {
        val elysian = ElysianVelocity.instance
        val api = elysian.api

        // Teleport player to server
        api.teleportPlayerToServer(player, "survival")

        // Send message to server
        api.sendMessageToServer(
            server,
            "elysian:main",
            mapOf(
                "type" to "CUSTOM",
                "action" to "give_reward",
                "player" to player.username
            )
        )

        // Get players on server
        val players = api.getPlayersOnServer("survival")

        // Broadcast to all servers
        api.broadcastMessage(
            "elysian:main",
            mapOf("type" to "SERVER_BROADCAST", "message" to "Hello!")
        )
    }
}
```

### For Paper Plugins (with ElysianCore)

```kotlin
import org.elysian.elysianCore.ElysianCore

class MyPaperPlugin : JavaPlugin() {

    fun sendToVelocity(data: Map<String, Any>) {
        // Send plugin message to Velocity
        val channel = "elysian:main"
        val message = serializeData(data)

        server.onlinePlayers.firstOrNull()?.let { player ->
            player.sendPluginMessage(this, channel, message.toByteArray())
        }
    }

    fun onPluginMessageReceived(channel: String, data: ByteArray) {
        if (channel == "elysian:main") {
            val message = deserializeData(String(data))

            when (message["type"]) {
                "TELEPORT_REQUEST" -> {
                    // Handle teleport request from Velocity
                    val uuid = UUID.fromString(message["uuid"] as String)
                    val location = message["location"] as Map<String, Any>
                    // ... teleport player
                }
            }
        }
    }
}
```

---

## 📚 Integration with ElysianCore

### Message Protocol

Both ElysianVelocity and ElysianCore use the same message format:

```json
{
  "type": "MESSAGE_TYPE",
  "data": {
    "key": "value"
  },
  "timestamp": 1234567890
}
```

### Supported Message Types

| Type | Direction | Description |
|------|-----------|-------------|
| `PLAYER_CONNECT` | Paper → Velocity | Player joined server |
| `PLAYER_DISCONNECT` | Paper → Velocity | Player left server |
| `PLAYER_DATA_UPDATE` | Both | Sync player data |
| `TELEPORT_REQUEST` | Both | Request player teleport |
| `SERVER_BROADCAST` | Velocity → Paper | Broadcast message |
| `DATA_REQUEST` | Velocity → Paper | Request data |
| `DATA_RESPONSE` | Paper → Velocity | Response with data |
| `CUSTOM` | Both | Custom plugin messages |

### Example: Cross-Server Teleport Plugin

**VelocityTeleport** (Velocity plugin):
```kotlin
class VelocityTeleport : Plugin {

    fun teleportPlayer(player: Player, targetWorld: String, location: Location) {
        val api = ElysianVelocity.instance.api

        // Find server with target world
        val targetServer = findServerWithWorld(targetWorld)

        // Send teleport request
        api.sendMessageToServer(
            targetServer,
            "elysian:main",
            mapOf(
                "type" to "TELEPORT_REQUEST",
                "uuid" to player.uniqueId.toString(),
                "world" to targetWorld,
                "x" to location.x,
                "y" to location.y,
                "z" to location.z
            )
        )

        // Switch player to server
        api.teleportPlayerToServer(player, targetServer.serverInfo.name)
    }
}
```

**PaperTeleport** (Paper plugin with ElysianCore):
```kotlin
class PaperTeleport : JavaPlugin() {

    fun onEnable() {
        // Register plugin message listener
        server.messenger.registerIncomingPluginChannel(this, "elysian:main") {
                channel, player, message ->
            handleMessage(channel, message)
        }
    }

    fun handleMessage(channel: String, data: ByteArray) {
        val message = deserialize(data)

        if (message["type"] == "TELEPORT_REQUEST") {
            val uuid = UUID.fromString(message["uuid"] as String)
            val world = message["world"] as String
            val x = message["x"] as Double
            val y = message["y"] as Double
            val z = message["z"] as Double

            // Wait for player to connect, then teleport
            schedulePlayerTeleport(uuid, world, x, y, z)
        }
    }
}
```

---

## 🎮 Commands

### `/server <server-name>`
Switch to another server.

**Aliases**: None  
**Permission**: None (everyone can use)  
**Usage**:
- `/server` - Show server list
- `/server survival` - Connect to survival server

### `/hub` or `/lobby`
Return to hub/lobby server.

**Aliases**: `/lobby`  
**Permission**: None (everyone can use)  
**Features**:
- Respects teleport cooldown
- Optional delay with cancel-on-move
- Configurable hub server

---

## 🔧 Advanced Features

### Custom Message Handlers

```kotlin
// Register custom message handler
api.registerMessageHandler(MessageType.CUSTOM) { server, data ->
    val customType = data["customType"] as? String

    when (customType) {
        "reward" -> handleReward(data)
        "punishment" -> handlePunishment(data)
    }
}
```

### Redis Pub/Sub

```kotlin
// Subscribe to Redis channel
api.subscribeToRedis("elysian:announcements") { channel, message ->
    // Broadcast to all players on this proxy
    api.broadcastMessage(
        "announcement",
        mapOf("message" to message)
    )
}

// Publish to Redis
api.publishRedisMessage("elysian:announcements", "Server restart in 5 minutes!")
```

### Server Groups

```kotlin
// Get best survival server (least players)
val servers = api.getServersInGroup("survival")
val bestServer = servers.minByOrNull { it.playersConnected.size }

// Teleport to best server
api.teleportPlayerToServer(player, bestServer.serverInfo.name)
```

---

## 🏗️ Building from Source

```bash
# Clone repository
git clone https://github.com/Elysian-Devs/elysian-velocity.git
cd elysian-velocity

# Build with Gradle
./gradlew build

# Output: build/libs/ElysianVelocity-1.0-SNAPSHOT.jar
```

---

## 📊 Performance

- **Minimal overhead** - Efficient message passing
- **Connection pooling** - Optimized Redis connections
- **Async operations** - Non-blocking message handling
- **Smart caching** - Reduced redundant queries
- **Auto cleanup** - Automatic resource management

### Benchmarks

| Operation | Time | Notes |
|-----------|------|-------|
| Send plugin message | < 1ms | Direct |
| Redis publish | 1-2ms | With Redis |
| Cross-server teleport | 50-200ms | Network dependent |
| Message handling | < 1ms | Per message |

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **ElysianCore**: [GitHub](https://github.com/Elysian-Devs/elysian-core)
- **Documentation**: [Wiki](https://github.com/Elysian-Devs/elysian-velocity/wiki)
- **Issues**: [Bug Reports](https://github.com/Elysian-Devs/elysian-velocity/issues)

---

<div align="center">

**Made with ❤️ by [Elysian Devs](https://github.com/Elysian-Devs)**

*ElysianVelocity - Connecting your Minecraft network*

</div>