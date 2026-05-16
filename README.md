# Java Swing Socket Messenger

This project is a simple Java client-server messaging system. The server runs on port `8000`, accepts multiple client connections using threads, and stores account plus message data in text files. The client is a Swing desktop application where users can register, log in, send messages, resend selected messages, and restore previous shared messages.

## Files

- `Server.java` - starts the socket server and creates shared manager objects.
- `ClientHandler.java` - handles one connected client on its own thread.
- `UserManager.java` - manages registration, login, and `users.txt`.
- `MessageManager.java` - manages shared chat history and `messages.txt`.
- `MessageRecord.java` - represents a stored server-side message.
- `ProtocolUtil.java` - encodes/decodes messages used by the socket protocol.
- `Client.java` - opens the Swing user interface and communicates with the server.
- `ChatMessage.java` - represents a message displayed in the Swing UI.
- `users.txt` - stores registered usernames and hashed passwords. This file is created automatically by the server.
- `messages.txt` - stores the shared message history. This file is created automatically by the server.

## Requirements

Install a Java Development Kit (JDK). This project was compiled successfully with `javac 24.0.2`, but any recent JDK with Swing support should work.

Check Java from PowerShell:

```powershell
javac -version
java -version
```

## How to Run

Open PowerShell in this folder:

```powershell
cd "D:\All Incoming Download Files\Server"
```

Compile the program:

```powershell
javac *.java
```

Start the server first:

```powershell
java Server
```

Keep the server window open. Then open another PowerShell window in the same folder and start the client:

```powershell
java Client
```

To test multiple users, open `java Client` in more than one terminal.

## How to Use

1. Register a new account with a username and password.
2. Log in using the same account.
3. Type a message and click **Send**.
4. Select a previous message and click **Resend Selected** to send it again.
5. Click **Restore Previous Messages** to reload the shared message history.

All users can see the same shared message history after logging in or restoring messages.

## Notes

- The server must be running before the client can connect.
- The app uses `localhost:8000`, so it is intended for local testing.
- Messages are not live-broadcasted yet. Other open clients should click **Restore Previous Messages** to see newly sent messages.
- Do not delete `users.txt` unless you want to remove all registered accounts.
- Do not delete `messages.txt` unless you want to clear the message history.

## Clean Build Files

Compiled `.class` files can be removed with:

```powershell
Remove-Item *.class
```
