import java.time.*;
import java.time.format.*;

public class ChatMessage
{
   private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
      .withZone(ZoneId.systemDefault());

   private final int id;
   private final long timestamp;
   private final String username;
   private final String text;

   public ChatMessage(int id, long timestamp, String username, String text)
   {
      // Create a ChatMessage value object with id, timestamp, username and text
      this.id = id;
      this.timestamp = timestamp;
      this.username = username;
      this.text = text;
   }

   public int getId()
   {
      // Return the message id
      return id;
   }

   public String toString()
   {
      // Format the message for display in the UI
      return "[" + FORMATTER.format(Instant.ofEpochMilli(timestamp)) + "] " + username + ": " + text;
   }

   public static ChatMessage fromSentLine(String line)
   {
      // Parse a `SENT` protocol line into a ChatMessage
      return fromLine(line, "SENT");
   }

   public static ChatMessage fromServerLine(String line)
   {
      // Parse a `MESSAGE` protocol line received from server into a ChatMessage
      return fromLine(line, "MESSAGE");
   }

   public static ChatMessage fromLiveLine(String line)
   {
      // Parse a `LIVE_MESSAGE` protocol line received from server into a ChatMessage
      return fromLine(line, "LIVE_MESSAGE");
   }

   private static ChatMessage fromLine(String line, String expectedPrefix)
   {
      try
      {
         String[] parts = line.split(" ", 5);
         if(parts.length != 5 || !parts[0].equals(expectedPrefix))
         {
            return null;
         }

         return new ChatMessage(Integer.parseInt(parts[1]), Long.parseLong(parts[2]), ProtocolUtil.decode(parts[3]), ProtocolUtil.decode(parts[4]));
      }
      catch(Exception e)
      {
         return null;
      }
   }
}
