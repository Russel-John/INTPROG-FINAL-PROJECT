public class MessageRecord
{
   private final int id;
   private final String username;
   private final long timestamp;
   private final String message;

   public MessageRecord(int id, String username, long timestamp, String message)
   {
      this.id = id;
      this.username = username;
      this.timestamp = timestamp;
      this.message = message;
   }

   public int getId()
   {
      return id;
   }

   public String getUsername()
   {
      return username;
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   public String getMessage()
   {
      return message;
   }

   public String toFileLine()
   {
      return id + "\t" + username + "\t" + timestamp + "\t" + ProtocolUtil.encode(message);
   }

   public static MessageRecord fromFileLine(String line)
   {
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
