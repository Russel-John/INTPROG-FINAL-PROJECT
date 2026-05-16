# Repository Guidelines

## Project Structure & Module Organization

This repository is a minimal Java socket example with source files at the root:

- `Server.java` starts the TCP server on port `8000` and creates shared manager objects.
- `ClientHandler.java` handles one connected client on its own thread.
- `UserManager.java`, `MessageManager.java`, `MessageRecord.java`, and `ProtocolUtil.java` contain the server-side OOP logic for accounts, messages, and protocol formatting.
- `Client.java` and `ChatMessage.java` contain the Swing desktop client for login, registration, sending, resending, and restoring previous messages.

There are currently no separate `src/`, `test/`, or asset directories. If the project grows, move production code into `src/main/java/` and tests into `src/test/java/` before adding packages.

## Build, Test, and Development Commands

Run commands from the repository root.

```powershell
javac *.java
```

Compiles all Java files and creates `.class` outputs in the current directory.

```powershell
java Server
```

Starts the server. Keep this process running before launching the client.

```powershell
java Client
```

Starts the Swing client and connects to the local server.

```powershell
Remove-Item *.class
```

Removes generated compile artifacts on Windows PowerShell.

## Coding Style & Naming Conventions

Use standard Java naming: `PascalCase` for classes, `camelCase` for variables and methods, and descriptive names for sockets, readers, and writers. Keep one public class per file and match the filename to the class name.

The existing code uses three-space indentation and brace-on-new-line style. Follow the current style unless doing a deliberate formatting pass. Prefer explicit imports over broad `java.util.*` style when adding new code.

## Testing Guidelines

No automated test framework is configured yet. For now, verify changes manually by compiling, starting `Server`, then starting `Client`, registering/logging in with two accounts, sending a message, resending it, and using restore to confirm both users see the shared history.

When tests are added, prefer JUnit 5 under `src/test/java/`. Name test classes after the class under test, such as `ServerTest` or `ClientTest`, and keep network tests isolated from fixed ports where possible.

## Commit & Pull Request Guidelines

This directory is not currently a Git repository, so no local commit history is available. Use clear, imperative commit messages such as `Handle client disconnects` or `Add socket timeout handling`.

Pull requests should include a short summary, manual test steps performed, and any relevant console output. For behavior changes, describe how server/client interaction changes and whether port `8000` remains required.

## Security & Configuration Tips

Do not commit generated `.class` files. Avoid hard-coding new hostnames, ports, or credentials; prefer constants or future configuration arguments. Validate socket input before adding command-like behavior.
