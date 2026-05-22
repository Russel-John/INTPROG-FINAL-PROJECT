import java.net.*;
import java.util.*;

public class Server
{
   private static final int PORT = 8000;
   private static final List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<ClientHandler>());

   public static void main(String args[])
   {
      // Create managers and accept incoming client connections on PORT
      UserManager userManager = new UserManager("users.txt");
      MessageManager messageManager = new MessageManager("messages.txt");

      try(ServerSocket serverSocket = new ServerSocket(PORT))
      {
         System.out.println("Server running on port " + PORT);

         while(true)
         {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, userManager, messageManager);
            Thread clientThread = new Thread(handler);
            clientThread.start();
         }
      }
      catch(Exception e)
      {
         System.out.println("Server error: " + e.getMessage());
      }
   }

   public static void registerClient(ClientHandler handler)
   {
      synchronized(connectedClients)
      {
         if(!connectedClients.contains(handler))
         {
            connectedClients.add(handler);
         }
      }
   }

   public static void unregisterClient(ClientHandler handler)
   {
      synchronized(connectedClients)
      {
         connectedClients.remove(handler);
      }
   }

   public static void broadcastMessage(MessageRecord record, ClientHandler sender)
   {
      synchronized(connectedClients)
      {
         for(ClientHandler handler : connectedClients)
         {
            if(handler != sender)
            {
               handler.sendLiveMessage(record);
            }
         }
      }
   }
}
