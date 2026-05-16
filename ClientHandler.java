import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable
{
   private final Socket socket;
   private final UserManager userManager;
   private final MessageManager messageManager;
   private String username;

   public ClientHandler(Socket socket, UserManager userManager, MessageManager messageManager)
   {
      this.socket = socket;
      this.userManager = userManager;
      this.messageManager = messageManager;
   }

   public void run()
   {
      try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)))
      {
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
         closeSocket();
      }
   }

   private void handleCommand(String line, BufferedWriter writer) throws Exception
   {
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
      if(userManager.login(requestedUsername, password))
      {
         username = requestedUsername;
         writeLine(writer, "OK Logged in");
      }
      else
      {
         writeLine(writer, "ERROR Invalid username or password");
      }
   }

   private void resend(String messageIdText, BufferedWriter writer) throws IOException
   {
      if(!isLoggedIn(writer))
      {
         return;
      }

      try
      {
         MessageRecord record = messageManager.resendMessage(username, Integer.parseInt(messageIdText));
         writeLine(writer, record == null ? "ERROR Message not found" : ProtocolUtil.formatMessageLine("SENT", record));
      }
      catch(NumberFormatException e)
      {
         writeLine(writer, "ERROR Invalid message id");
      }
   }

   private void sendHistory(BufferedWriter writer) throws IOException
   {
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
      if(username == null)
      {
         writeLine(writer, "ERROR Login required");
         return false;
      }

      return true;
   }

   private void writeLine(BufferedWriter writer, String line) throws IOException
   {
      writer.write(line);
      writer.newLine();
      writer.flush();
   }

   private void closeSocket()
   {
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
