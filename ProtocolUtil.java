import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ProtocolUtil
{
   public static String encode(String value)
   {
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
   }

   public static String decode(String value)
   {
      return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
   }

   public static String formatMessageLine(String prefix, MessageRecord record)
   {
      return prefix + " " + record.getId() + " " + record.getTimestamp() + " " + encode(record.getUsername()) + " " + encode(record.getMessage());
   }
}
