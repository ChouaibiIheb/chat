package com.dhasboard.chat;

import com.dhasboard.chat.DBConnection;
import com.dhasboard.chat.Message;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    public void saveMessage(Message message) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content, sent_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, message.getSenderId());
            stmt.setInt(2, message.getReceiverId());
            stmt.setString(3, message.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(message.getSentAt()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<Message> getMessagesBetweenUsers(int user1, int user2) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY sent_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user1);
            stmt.setInt(2, user2);
            stmt.setInt(3, user2);
            stmt.setInt(4, user1);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message(
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("content")
                    );
                    message.setId(rs.getInt("id"));
                    message.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public List<Message> getNewMessages(int user1, int user2, LocalDateTime lastCheck) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) AND sent_at > ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user1);
            stmt.setInt(2, user2);
            stmt.setInt(3, user2);
            stmt.setInt(4, user1);
            stmt.setTimestamp(5, Timestamp.valueOf(lastCheck));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message(
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("content")
                    );
                    message.setId(rs.getInt("id"));
                    message.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                    messages.add(message);
                }
            }
        }
        return messages;
    }
}