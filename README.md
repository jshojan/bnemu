# bnemu

`bnemu` is a lightweight Battle.net 1.0 server emulator written in Java using Netty. It supports core protocol functionality to allow clients such as StealthBot to connect, authenticate, and chat using Blizzard’s legacy protocol format.

This project is built for reliability, readability, and long-term preservation of the original protocol while using modern development practices.

## Features

- In-memory and MongoDB-based account management
- Compatible with StealthBot v2.7
- Uses Netty 4.x for non-blocking IO
- Battle.net BrokenSHA1 hash-based authentication
- Debug logging via SLF4J

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- Docker (optional, used for running MongoDB)
- MongoDB (running locally at `localhost:27017`)
- IntelliJ IDEA (recommended)

### Run with Maven
To compile all modules:
```bash
mvn clean install -DskipTests
```

### IntelliJ Run Setup

#### Build Configuration

- **Type**: Maven
- **Name**: `Install All`
- **Working Directory**: `bnemu/`
- **Command**: `clean install -DskipTests`
- **VM Options**: `-Dnet.bytebuddy.experimental=true`

#### Run Configuration

- **Type**: Maven
- **Name**: `Run BNCS Server`
- **Working Directory**: `bncs-server/`
- **Command**: `exec:java -Dexec.mainClass=org.bnemu.bncs.BncsServer`
- **VM Options**: `-Dnet.bytebuddy.experimental=true`
- **JRE**: Use JDK 21 or newer

### MongoDB (via Docker)

To start a local MongoDB instance quickly for development, run:

```bash
docker-compose up -d

```

This uses the provided `docker-compose.yml` file at the project root. It creates a MongoDB container on `localhost:27017` with default credentials and database (`bnemu`).

### Build and Run

### Configuration

A `config.yml` file is included here: `bncs-server/src/main/resources/config.yml`

This file controls runtime options such as:

- MongoDB connection URI
- Server bind port
- Logging levels and debug flags

You may modify this file before running the server to adjust environment-specific settings.

## Compatibility

This emulator is tested with:

- StealthBot v2.7 (Build 493)
- Chat and presence features required for standard user list syncing

### Project Structure

- `core/`: Core Netty server, protocol handlers, chat logic, and session management
- `persistence/`: MongoDB DAO and BrokenSHA1 password hashing
- `bncs-server/`: Contains main class to launch the server and bundled `config.yml`
- `docker-compose.yml`: Used to quickly launch a local MongoDB instance for development/testing

## License

This project is licensed under the MIT License. See the `LICENSE` file for more information.

## Credits

- [PvPGN](https://github.com/pvpgn/pvpgn-server) — reference implementation of a classic BNCS emulator
- [BNETDocs](https://bnetdocs.org/) — protocol documentation and packet reference
- [init6](https://github.com/init6) — inspiration for modular architecture and code cleanliness