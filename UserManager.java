import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;

public class UserManager
{
   private final Path usersFile;

   public UserManager(String fileName)
   {
      // Initialize user manager with the backing users file
      usersFile = Path.of(fileName);
      createFileIfMissing();
   }

   public synchronized boolean register(String username, String password) throws Exception
   {
      // Register a new user if username valid, password long enough, and username unique
      if(!isValidUsername(username) || password.length() < 4 || userExists(username))
      {
         return false;
      }

      String line = username + "\t" + hashPassword(password);
      Files.writeString(usersFile, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
      return true;
   }

   public synchronized boolean login(String username, String password) throws Exception
   {
      // Verify username/password by comparing stored SHA-256 hash
      String expectedHash = hashPassword(password);

      for(String line : Files.readAllLines(usersFile, StandardCharsets.UTF_8))
      {
         String[] parts = line.split("\t", 2);
         if(parts.length == 2 && parts[0].equals(username) && parts[1].equals(expectedHash))
         {
            return true;
         }
      }

      return false;
   }

   private void createFileIfMissing()
   {
      // Ensure the users file exists on disk
      try
      {
         if(!Files.exists(usersFile))
         {
            Files.createFile(usersFile);
         }
      }
      catch(IOException e)
      {
         throw new RuntimeException("Cannot create users file", e);
      }
   }

   private boolean userExists(String username) throws IOException
   {
      // Check whether a username is already present in users file
      for(String line : Files.readAllLines(usersFile, StandardCharsets.UTF_8))
      {
         String[] parts = line.split("\t", 2);
         if(parts.length > 0 && parts[0].equals(username))
         {
            return true;
         }
      }

      return false;
   }

   private boolean isValidUsername(String username)
   {
      // Validate username against allowed characters and length
      return username != null && username.matches("[A-Za-z0-9_]{3,20}");
   }

   private String hashPassword(String password) throws Exception
   {
      // Compute SHA-256 hex digest of a password
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();

      for(byte value : hash)
      {
         builder.append(String.format("%02x", value));
      }

      return builder.toString();
   }
}
