import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
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
   private final Object sendLock = new Object();
   private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<String>();
   private final Object historyLock = new Object();
   private final java.util.List<ChatMessage> historyMessages = new ArrayList<ChatMessage>();
   private final java.util.List<ChatMessage> liveMessagesDuringHistory = new ArrayList<ChatMessage>();
   private volatile boolean running;
   private volatile boolean historyPending;
   private volatile boolean restoringHistory;
   private CountDownLatch historyLatch;
   private String historyError;
   private Thread listenerThread;

   public static void main(String args[])
   {
      // Start the Swing UI on the EDT
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
      // Build and initialize the main window and UI cards
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
      // Construct the login/register form panel
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
      // Construct the chat UI panel with message list and controls
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
      // Send REGISTER command to the server with provided credentials
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
      // Send LOGIN command and switch to chat view on success
      try
      {
         connectIfNeeded();
         String response = sendCommand("LOGIN " + getUsername() + " " + encode(getPassword()));

         if(response.startsWith("OK"))
         {
            cardLayout.show(cards, "chat");
            messageModel.clear();
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
      // Send current typed message as `SEND` command to server
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
      // Send `RESEND` command for the selected message id
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
      // Request `HISTORY` from server and populate message list
      try
      {
         beginHistoryRequest();
         writeCommand("HISTORY");

         if(!historyLatch.await(10, TimeUnit.SECONDS))
         {
            showError("Timed out restoring message history.");
            clearHistoryRequestState();
            return;
         }

         if(historyError != null)
         {
            showError(historyError);
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         showError(e.getMessage());
      }
      catch(Exception e)
      {
         showError(e.getMessage());
      }
   }

   private void addSentMessage(String response)
   {
      // Parse a `SENT` response and add it to the UI list
      ChatMessage item = ChatMessage.fromSentLine(response);
      if(item == null)
      {
         showError(response);
         return;
      }

      appendMessage(item);
   }

   private void connectIfNeeded() throws IOException
   {
      // Establish socket connection to server and read initial greeting
      if(socket != null && socket.isConnected() && !socket.isClosed())
      {
         return;
      }

      socket = new Socket(HOST, PORT);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      reader.readLine();
      startListener();
   }

   private String sendCommand(String command) throws IOException
   {
      // Send a single-line command and wait for the matching server reply
      writeCommand(command);

      try
      {
         return responseQueue.take();
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new IOException(e);
      }
   }

   private void writeCommand(String command) throws IOException
   {
      synchronized(sendLock)
      {
         writer.write(command);
         writer.newLine();
         writer.flush();
      }
   }

   private void startListener()
   {
      running = true;
      listenerThread = new Thread(new Runnable()
      {
         public void run()
         {
            listenLoop();
         }
      });
      listenerThread.setDaemon(true);
      listenerThread.start();
   }

   private void listenLoop()
   {
      try
      {
         String line;
         while(running && (line = reader.readLine()) != null)
         {
            handleIncomingLine(line);
         }
      }
      catch(Exception e)
      {
         if(running)
         {
            SwingUtilities.invokeLater(() -> showError(e.getMessage()));
         }
      }
      finally
      {
         running = false;

         if(historyPending || restoringHistory)
         {
            historyError = "Connection closed while restoring history.";
            clearHistoryRequestState();
            if(historyLatch != null)
            {
               historyLatch.countDown();
            }
         }

         responseQueue.offer("ERROR Connection closed");
      }
   }

   private void handleIncomingLine(String line)
   {
      if(historyPending)
      {
         if("HISTORY_BEGIN".equals(line))
         {
            historyPending = false;
            restoringHistory = true;

            synchronized(historyLock)
            {
               historyMessages.clear();
               liveMessagesDuringHistory.clear();
            }

            return;
         }

         if(line.startsWith("ERROR"))
         {
            historyError = line;
            clearHistoryRequestState();

            if(historyLatch != null)
            {
               historyLatch.countDown();
            }

            return;
         }
      }

      if(restoringHistory)
      {
         if("HISTORY_END".equals(line))
         {
            applyHistoryMessages();
            return;
         }

         if(line.startsWith("MESSAGE "))
         {
            ChatMessage item = ChatMessage.fromServerLine(line);
            if(item != null)
            {
               synchronized(historyLock)
               {
                  historyMessages.add(item);
               }
            }

            return;
         }

         if(line.startsWith("LIVE_MESSAGE "))
         {
            ChatMessage item = ChatMessage.fromLiveLine(line);
            if(item != null)
            {
               synchronized(historyLock)
               {
                  liveMessagesDuringHistory.add(item);
               }
            }

            return;
         }
      }

      if(line.startsWith("LIVE_MESSAGE "))
      {
         ChatMessage item = ChatMessage.fromLiveLine(line);
         if(item != null)
         {
            appendMessage(item);
         }

         return;
      }

      responseQueue.offer(line);
   }

   private void beginHistoryRequest()
   {
      historyError = null;
      historyLatch = new CountDownLatch(1);
      historyPending = true;
      restoringHistory = false;
   }

   private void clearHistoryRequestState()
   {
      historyPending = false;
      restoringHistory = false;
   }

   private void applyHistoryMessages()
   {
      java.util.List<ChatMessage> snapshot;
      java.util.List<ChatMessage> liveSnapshot;

      synchronized(historyLock)
      {
         snapshot = new ArrayList<ChatMessage>(historyMessages);
         liveSnapshot = new ArrayList<ChatMessage>(liveMessagesDuringHistory);
         historyMessages.clear();
         liveMessagesDuringHistory.clear();
      }

      try
      {
         SwingUtilities.invokeAndWait(new Runnable()
         {
            public void run()
            {
               messageModel.clear();

               for(ChatMessage item : snapshot)
               {
                  messageModel.addElement(item);
               }

               for(ChatMessage item : liveSnapshot)
               {
                  messageModel.addElement(item);
               }

               if(messageModel.size() > 0)
               {
                  messageList.ensureIndexIsVisible(messageModel.size() - 1);
               }
            }
         });
      }
      catch(Exception e)
      {
         SwingUtilities.invokeLater(() -> showError(e.getMessage()));
      }
      finally
      {
         restoringHistory = false;
         clearHistoryRequestState();

         if(historyLatch != null)
         {
            historyLatch.countDown();
         }
      }
   }

   private void appendMessage(ChatMessage item)
   {
      SwingUtilities.invokeLater(() ->
      {
         messageModel.addElement(item);
         messageList.ensureIndexIsVisible(messageModel.size() - 1);
      });
   }

   private String getUsername()
   {
      // Return trimmed username from input field
      return usernameField.getText().trim();
   }

   private String getPassword()
   {
      // Return password from password field
      return new String(passwordField.getPassword());
   }

   private String encode(String value)
   {
      // Base64-encode a value for protocol transmission
      return ProtocolUtil.encode(value);
   }

   private void showResponse(String response, String okMessage)
   {
      // Display a friendly dialog for OK or error responses
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
      // Show an informational dialog with an error message
      JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.INFORMATION_MESSAGE);
   }

   private void closeConnection()
   {
      // Send QUIT and close the socket cleanly
      try
      {
         running = false;

         if(writer != null)
         {
            synchronized(sendLock)
            {
               writer.write("QUIT");
               writer.newLine();
               writer.flush();
            }
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
