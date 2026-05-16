import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
   private static final int PORT = 8000;

   public static void main(String args[])
   {
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
}
