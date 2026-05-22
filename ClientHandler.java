import java.io.*;
import java.net.*;
import java.nio.charset.*;

public class ClientHandler implements Runnable
{
   // Handles one client connection: reads lines, executes commands, and responds.
   private final Socket socket;
   private final UserManager userManager;
   private final MessageManager messageManager;
   private BufferedReader reader;
   private BufferedWriter writer;
   private final Object writeLock = new Object();
   private String username;

   public ClientHandler(Socket socket, UserManager userManager, MessageManager messageManager)
   {
      // Initialize handler with the accepted socket and shared managers
      this.socket = socket;
      this.userManager = userManager;
      this.messageManager = messageManager;
   }

   public void run()
   {
      // Main loop: accept and process incoming client lines until QUIT or disconnect
      try
      {
         reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

         writeLine(writer, "OK Connected");

         String line;
         while((line = reader.readLine()) != null)
         {
            if(line.equals("QUIT"))
            {
               break;
            }

            handleCommand(line, writer);
         }
      }
      catch(Exception e)
      {
         System.out.println("Client error: " + e.getMessage());
      }
      finally
      {
         Server.unregisterClient(this);
         closeSocket();
      }
   }

   private void handleCommand(String line, BufferedWriter writer) throws Exception
   {
      // Parse and dispatch a single protocol command line from the client
      String[] parts = line.split(" ", 3);
      String command = parts[0];

      if(command.equals("REGISTER") && parts.length == 3)
      {
         boolean created = userManager.register(parts[1], ProtocolUtil.decode(parts[2]));
         writeLine(writer, created ? "OK Registered" : "ERROR Username must be unique, 3-20 letters/numbers/underscores, and password must be 4+ characters");
      }
      else if(command.equals("LOGIN") && parts.length == 3)
      {
         login(parts[1], ProtocolUtil.decode(parts[2]), writer);
      }
      else if(command.equals("SEND") && parts.length == 2)
      {
         if(isLoggedIn(writer))
         {
            MessageRecord record = messageManager.saveMessage(username, ProtocolUtil.decode(parts[1]));
            writeLine(writer, ProtocolUtil.formatMessageLine("SENT", record));
            Server.broadcastMessage(record, this);
         }
      }
      else if(command.equals("RESEND") && parts.length == 2)
      {
         resend(parts[1], writer);
      }
      else if(command.equals("HISTORY"))
      {
         sendHistory(writer);
      }
      else
      {
         writeLine(writer, "ERROR Unknown command");
      }
   }

   private void login(String requestedUsername, String password, BufferedWriter writer) throws Exception
   {
      // Attempt to authenticate; set `username` and respond accordingly
      if(userManager.login(requestedUsername, password))
      {
         username = requestedUsername;
         Server.registerClient(this);
         writeLine(writer, "OK Logged in");
      }
      else
      {
         writeLine(writer, "ERROR Invalid username or password");
      }
   }

   private void resend(String messageIdText, BufferedWriter writer) throws IOException
   {
      // Resend a past message by id on behalf of the logged-in user
      if(!isLoggedIn(writer))
      {
         return;
      }

      try
      {
         MessageRecord record = messageManager.resendMessage(username, Integer.parseInt(messageIdText));
         if(record == null)
         {
            writeLine(writer, "ERROR Message not found");
            return;
         }

         writeLine(writer, ProtocolUtil.formatMessageLine("SENT", record));
         Server.broadcastMessage(record, this);
      }
      catch(NumberFormatException e)
      {
         writeLine(writer, "ERROR Invalid message id");
      }
   }

   private void sendHistory(BufferedWriter writer) throws IOException
   {
      // Send the entire message history to the client (requires login)
      if(!isLoggedIn(writer))
      {
         return;
      }

      writeLine(writer, "HISTORY_BEGIN");
      for(MessageRecord record : messageManager.getAllMessages())
      {
         writeLine(writer, ProtocolUtil.formatMessageLine("MESSAGE", record));
      }
      writeLine(writer, "HISTORY_END");
   }

   private boolean isLoggedIn(BufferedWriter writer) throws IOException
   {
      // Verify the client has logged in; send an error response if not
      if(username == null)
      {
         writeLine(writer, "ERROR Login required");
         return false;
      }

      return true;
   }

   private void writeLine(BufferedWriter writer, String line) throws IOException
   {
      // Send a single line to the client and flush the writer
      synchronized(writeLock)
      {
         writer.write(line);
         writer.newLine();
         writer.flush();
      }
   }

   public void sendLiveMessage(MessageRecord record)
   {
      if(writer == null)
      {
         return;
      }

      try
      {
         writeLine(writer, ProtocolUtil.formatMessageLine("LIVE_MESSAGE", record));
      }
      catch(IOException e)
      {
         System.out.println("Broadcast error: " + e.getMessage());
      }
   }

   private void closeSocket()
   {
      // Close the underlying socket, ignoring errors
      try
      {
         socket.close();
      }
      catch(IOException e)
      {
         System.out.println("Socket close error: " + e.getMessage());
      }
   }
}
