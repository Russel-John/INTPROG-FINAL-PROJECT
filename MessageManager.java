import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class MessageManager
{
   private final Path messagesFile;
   private int nextMessageId = 1;

   public MessageManager(String fileName)
   {
      messagesFile = Path.of(fileName);
      initialize();
   }

   public synchronized MessageRecord saveMessage(String username, String message) throws IOException
   {
      MessageRecord record = new MessageRecord(nextMessageId++, username, System.currentTimeMillis(), message);
      Files.writeString(messagesFile, record.toFileLine() + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
      return record;
   }

   public synchronized MessageRecord resendMessage(String username, int messageId) throws IOException
   {
      for(MessageRecord record : getAllMessages())
      {
         if(record.getId() == messageId)
         {
            return saveMessage(username, record.getMessage());
         }
      }

      return null;
   }

   public synchronized List<MessageRecord> getAllMessages() throws IOException
   {
      List<MessageRecord> records = new ArrayList<MessageRecord>();

      for(String line : Files.readAllLines(messagesFile, StandardCharsets.UTF_8))
      {
         MessageRecord record = MessageRecord.fromFileLine(line);
         if(record != null)
         {
            records.add(record);
         }
      }

      return records;
   }

   private void initialize()
   {
      try
      {
         if(!Files.exists(messagesFile))
         {
            Files.createFile(messagesFile);
         }

         for(MessageRecord record : getAllMessages())
         {
            if(record.getId() >= nextMessageId)
            {
               nextMessageId = record.getId() + 1;
            }
         }
      }
      catch(IOException e)
      {
         throw new RuntimeException("Cannot initialize messages file", e);
      }
   }
}
