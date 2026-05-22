import java.nio.charset.*;
import java.util.*;

public class ProtocolUtil
{
   public static String encode(String value)
   {
      // Base64-encode a UTF-8 string for safe protocol transport
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
   }

   public static String decode(String value)
   {
      // Decode a Base64-encoded UTF-8 string from the protocol
      return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
   }

   public static String formatMessageLine(String prefix, MessageRecord record)
   {
      // Build a single protocol line (prefix + id + timestamp + encoded user + encoded message)
      return prefix + " " + record.getId() + " " + record.getTimestamp() + " " + encode(record.getUsername()) + " " + encode(record.getMessage());
   }
}
