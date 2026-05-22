public class MessageRecord
{
   private final int id;
   private final String username;
   private final long timestamp;
   private final String message;

   public MessageRecord(int id, String username, long timestamp, String message)
   {
      // Construct a message record used for storage and protocol formatting
      this.id = id;
      this.username = username;
      this.timestamp = timestamp;
      this.message = message;
   }

   public int getId()
   {
      // Return the message id
      return id;
   }

   public String getUsername()
   {
      // Return the username who sent the message
      return username;
   }

   public long getTimestamp()
   {
      // Return the epoch milli timestamp for the message
      return timestamp;
   }

   public String getMessage()
   {
      // Return the raw message text
      return message;
   }

   public String toFileLine()
   {
      // Serialize the record to a tab-separated line for messages.txt
      return id + "\t" + username + "\t" + timestamp + "\t" + ProtocolUtil.encode(message);
   }

   public static MessageRecord fromFileLine(String line)
   {
      // Parse a tab-separated file line into a MessageRecord or return null
      try
      {
         String[] parts = line.split("\t", 4);
         if(parts.length != 4)
         {
            return null;
         }

         return new MessageRecord(Integer.parseInt(parts[0]), parts[1], Long.parseLong(parts[2]), ProtocolUtil.decode(parts[3]));
      }
      catch(Exception e)
      {
         return null;
      }
   }
}
