# bnemu

A Battle.net 1.0 server emulator written in Java using Netty. Supports Diablo II (1.14d) and legacy Blizzard chat clients such as StealthBot.

## Architecture

bnemu is a multi-module Maven project:

| Module | Description |
|--------|-------------|
| `core` | Shared models, configuration, session management, authentication stores, and packet utilities |
| `bnftp` | Battle.net File Transfer Protocol (BNFTP) — serves version check files to game clients |
| `bncs-server` | Battle.net Chat Server (BNCS) — handles login, chat, and realm discovery on port 6112. Also serves BNFTP requests on the same port (protocol byte 0x02) |
| `d2cs-server` | Diablo II Character Server (D2CS) — handles realm login, character management, and game creation/join via MCP on port 6113 |
| `d2gs-server` | Diablo II Game Server (D2GS) — stub, not yet implemented |
| `telnet-gateway` | Telnet chat gateway — stub, not yet implemented |
| `persistence` | MongoDB data access layer (accounts, characters) and BrokenSHA1 password hashing |

### Connection flow (Diablo II)

```
D2 Client
  │
  ├─ [0x02] BNFTP ──→ BNCS Server (port 6112) ──→ sends ver-IX86-0.mpq, closes
  │
  ├─ [0x01] BNCS ───→ BNCS Server (port 6112) ──→ auth, chat, realm discovery
  │                         │
  │                         └─ SID_LOGONREALMEX ──→ issues realm auth token
  │
  └─ [MCP] ─────────→ D2CS Server (port 6113) ──→ character select, game create/join
                            │
                            └─ join game ──→ D2GS Server (port 4000) [not yet implemented]
```

## Prerequisites

- Java 21+
- Maven 3.8+
- MongoDB (via Docker or local install)

## Building

```bash
mvn clean install -DskipTests
```

## Running

### 1. Start MongoDB

```bash
docker-compose up -d
```

This starts MongoDB on `localhost:27017` with the default credentials from `docker-compose.yml`.

### 2. Start the BNCS server

```bash
mvn -pl bncs-server exec:java -Dexec.mainClass=org.bnemu.bncs.BncsServer
```

This starts the BNCS server on port 6112. BNFTP runs inside the BNCS server — when a client connects with protocol byte `0x02`, the server automatically switches to BNFTP mode and serves the requested file. There is no separate BNFTP process.

### 3. Start the D2CS server

```bash
mvn -pl d2cs-server exec:java -Dexec.mainClass=org.bnemu.d2cs.D2csServer
```

This starts the D2CS realm server on port 6113.

## BNFTP Setup

The Diablo II client downloads version check files via BNFTP before logging in. You need to provide these files.

### File directory

Create a `files/` directory in your working directory (project root if running via Maven):

```bash
mkdir files
```

The path is configurable via `bnftp.filesDir` in `config.yml` (default: `"files"`).

### Required files

The D2 client typically requests:

- `ver-IX86-0.mpq` — version check archive (CheckRevision)
- `IX86ver0.dll` — version check library (optional, depends on client)

These files can be obtained from:

- A **PvPGN** server's `files/` directory
- A **Diablo II** installation (extracted from game data)
- Other Battle.net server emulator distributions

Place the files directly in the `files/` directory (no subdirectories).

## Configuration

Each server module includes a `config.yml` at `src/main/resources/config.yml`. Both BNCS and D2CS share the same config format:

```yaml
# MongoDB connection
mongo:
  host: localhost
  port: 27017
  database: bnemu
  username: root
  password: rootpass

# Realm settings
realm:
  name: "bnemu"
  description: "bnemu Realm"

# BNFTP file serving
bnftp:
  filesDir: "files"

# Server ports and addresses
server:
  bncs:
    port: 6112
  d2cs:
    port: 6113
    host: "172.16.1.38"    # IP the BNCS server advertises to clients for realm connection
  d2gs:
    port: 4000
    host: "172.16.1.38"    # IP the D2CS server sends to clients for game server connection
  telnet:
    port: 23
```

The `host` fields under `d2cs` and `d2gs` should be set to the IP address your clients can reach. If running everything on a single machine, use that machine's LAN IP (not `127.0.0.1`, since the D2 client connects to these addresses separately).

## Diablo II Client Setup

To point your Diablo II client at this server, edit the Windows registry:

**Key:** `HKEY_CURRENT_USER\Software\Battle.net\Configuration`

**Value name:** `Gateways`

**Value data (REG_SZ):**
```
1000
01
bnemu
YOUR_SERVER_IP
-8
bnemu
bnemu
```

Format breakdown:
- `1000` — version number
- `01` — number of gateways
- `bnemu` — config name
- `YOUR_SERVER_IP` — server IP or hostname
- `-8` — timezone offset
- `bnemu` — gateway display name
- `bnemu` — gateway region

Replace `YOUR_SERVER_IP` with the IP address of the machine running bnemu.

## Compatibility

Tested with:

- Diablo II 1.14d (full game: login, realm, character management)
- StealthBot v2.7 (chat bot: login, chat, whisper)

## Project Structure

```
bnemu/
├── core/                  # Shared config, models, session management, auth
├── bnftp/                 # BNFTP protocol handler and file provider
├── bncs-server/           # BNCS server (login, chat, realm discovery, BNFTP)
├── d2cs-server/           # D2CS realm server (characters, games)
├── d2gs-server/           # D2GS game server (stub)
├── telnet-gateway/        # Telnet chat gateway (stub)
├── persistence/           # MongoDB DAOs and password hashing
├── docker-compose.yml     # MongoDB dev instance
└── pom.xml                # Parent POM
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Credits

- [PvPGN](https://github.com/pvpgn/pvpgn-server) — reference implementation of a classic Battle.net emulator
- [BNETDocs](https://bnetdocs.org/) — protocol documentation and packet reference
- [init6](https://github.com/fjaros/init6) — inspiration for modular architecture
