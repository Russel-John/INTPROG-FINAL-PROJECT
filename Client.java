import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import javax.swing.*;

public class Client extends JFrame
{
   private static final String HOST = "localhost";
   private static final int PORT = 8000;

   private final CardLayout cardLayout = new CardLayout();
   private final JPanel cards = new JPanel(cardLayout);
   private final JTextField usernameField = new JTextField(18);
   private final JPasswordField passwordField = new JPasswordField(18);
   private final DefaultListModel<ChatMessage> messageModel = new DefaultListModel<ChatMessage>();
   private final JList<ChatMessage> messageList = new JList<ChatMessage>(messageModel);
   private final JTextField messageField = new JTextField(28);

   private Socket socket;
   private BufferedReader reader;
   private BufferedWriter writer;

   public static void main(String args[])
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            new Client().setVisible(true);
         }
      });
   }

   Client()
   {
      setTitle("Socket Messenger");
      setSize(560, 420);
      setLocationRelativeTo(null);
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

      cards.add(createLoginPanel(), "login");
      cards.add(createChatPanel(), "chat");
      add(cards);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent event)
         {
            closeConnection();
         }
      });
   }

   private JPanel createLoginPanel()
   {
      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.insets = new Insets(8, 8, 8, 8);
      constraints.fill = GridBagConstraints.HORIZONTAL;

      constraints.gridx = 0;
      constraints.gridy = 0;
      panel.add(new JLabel("Username"), constraints);

      constraints.gridx = 1;
      panel.add(usernameField, constraints);

      constraints.gridx = 0;
      constraints.gridy = 1;
      panel.add(new JLabel("Password"), constraints);

      constraints.gridx = 1;
      panel.add(passwordField, constraints);

      JButton loginButton = new JButton("Login");
      loginButton.addActionListener(event -> login());

      JButton registerButton = new JButton("Register");
      registerButton.addActionListener(event -> register());

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttons.add(registerButton);
      buttons.add(loginButton);

      constraints.gridx = 0;
      constraints.gridy = 2;
      constraints.gridwidth = 2;
      panel.add(buttons, constraints);

      return panel;
   }

   private JPanel createChatPanel()
   {
      JPanel panel = new JPanel(new BorderLayout(8, 8));
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(new JScrollPane(messageList), BorderLayout.CENTER);

      JButton sendButton = new JButton("Send");
      sendButton.addActionListener(event -> sendMessage());

      JButton resendButton = new JButton("Resend Selected");
      resendButton.addActionListener(event -> resendSelectedMessage());

      JButton restoreButton = new JButton("Restore Previous Messages");
      restoreButton.addActionListener(event -> restoreMessages());

      JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
      inputPanel.add(messageField, BorderLayout.CENTER);
      inputPanel.add(sendButton, BorderLayout.EAST);

      JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      actionPanel.add(restoreButton);
      actionPanel.add(resendButton);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(inputPanel, BorderLayout.CENTER);
      bottomPanel.add(actionPanel, BorderLayout.SOUTH);

      panel.add(bottomPanel, BorderLayout.SOUTH);
      return panel;
   }

   private void register()
   {
      try
      {
         connectIfNeeded();
         String response = sendCommand("REGISTER " + getUsername() + " " + encode(getPassword()));
         showResponse(response, "Account created. You can now log in.");
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void login()
   {
      try
      {
         connectIfNeeded();
         String response = sendCommand("LOGIN " + getUsername() + " " + encode(getPassword()));

         if(response.startsWith("OK"))
         {
            cardLayout.show(cards, "chat");
            restoreMessages();
         }
         else
         {
            showError(response);
         }
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void sendMessage()
   {
      String message = messageField.getText().trim();
      if(message.length() == 0)
      {
         return;
      }

      try
      {
         String response = sendCommand("SEND " + encode(message));
         addSentMessage(response);
         messageField.setText("");
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void resendSelectedMessage()
   {
      ChatMessage selected = messageList.getSelectedValue();
      if(selected == null)
      {
         showError("Select a message to resend.");
         return;
      }

      try
      {
         addSentMessage(sendCommand("RESEND " + selected.getId()));
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void restoreMessages()
   {
      try
      {
         writer.write("HISTORY");
         writer.newLine();
         writer.flush();

         String response = reader.readLine();
         if(!"HISTORY_BEGIN".equals(response))
         {
            showError(response);
            return;
         }

         messageModel.clear();

         while((response = reader.readLine()) != null && !"HISTORY_END".equals(response))
         {
            ChatMessage item = ChatMessage.fromServerLine(response);
            if(item != null)
            {
               messageModel.addElement(item);
            }
         }
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void addSentMessage(String response)
   {
      ChatMessage item = ChatMessage.fromSentLine(response);
      if(item == null)
      {
         showError(response);
         return;
      }

      messageModel.addElement(item);
      messageList.ensureIndexIsVisible(messageModel.size() - 1);
   }

   private void connectIfNeeded() throws IOException
   {
      if(socket != null && socket.isConnected() && !socket.isClosed())
      {
         return;
      }

      socket = new Socket(HOST, PORT);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      reader.readLine();
   }

   private String sendCommand(String command) throws IOException
   {
      writer.write(command);
      writer.newLine();
      writer.flush();
      return reader.readLine();
   }

   private String getUsername()
   {
      return usernameField.getText().trim();
   }

   private String getPassword()
   {
      return new String(passwordField.getPassword());
   }

   private String encode(String value)
   {
      return ProtocolUtil.encode(value);
   }

   private void showResponse(String response, String okMessage)
   {
      if(response.startsWith("OK"))
      {
         JOptionPane.showMessageDialog(this, okMessage);
      }
      else
      {
         showError(response);
      }
   }

   private void showError(String message)
   {
      JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.INFORMATION_MESSAGE);
   }

   private void closeConnection()
   {
      try
      {
         if(writer != null)
         {
            writer.write("QUIT");
            writer.newLine();
            writer.flush();
         }

         if(socket != null)
         {
            socket.close();
         }
      }
      catch(IOException e)
      {
         System.out.println(e.getMessage());
      }
   }

}
