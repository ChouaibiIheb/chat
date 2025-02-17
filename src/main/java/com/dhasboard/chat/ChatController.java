import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.Socket;
import java.sql.*;

public class ChatController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;

    private Socket socket;
    private PrintWriter out;
    private int userId = 1; // معرف المستخدم الحالي (يمكن تغييره)
    private int receiverId = 2; // معرف الشخص الذي أتحدث معه

    public void initialize() {
        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);

            // إرسال معرف المستخدم للخادم
            out.println(userId);

            loadPreviousMessages();

            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        appendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPreviousMessages() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatdb", "root", "password")) {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT sender_id, content FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY timestamp");
            stmt.setInt(1, userId);
            stmt.setInt(2, receiverId);
            stmt.setInt(3, receiverId);
            stmt.setInt(4, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                String content = rs.getString("content");
                chatArea.appendText((senderId == userId ? "أنا" : "الشخص الآخر") + ": " + content + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            out.println(receiverId + ":" + message);
            appendMessage("أنا: " + message);
            messageField.clear();
        }
    }

    private void appendMessage(String message) {
        chatArea.appendText(message + "\n");
    }
}
