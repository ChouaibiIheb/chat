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
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
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
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
                    );
                    message.setId(rs.getInt("id"));
                    message.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                    messages.add(message);
                }
            }
        }
        return messages;
    }
    public List<UserMessage> getUsersWithLastMessage(int userId) throws SQLException {
        List<UserMessage> userMessages = new ArrayList<>();
        String sql = "SELECT u.id AS user_id, u.username, m.content, m.sent_at " +
                "FROM users u " +
                "JOIN messages m ON (u.id = m.sender_id OR u.id = m.receiver_id) " +
                "WHERE (m.sender_id = ? OR m.receiver_id = ?) AND u.id != ? " +
                "AND m.id = ( " +
                "    SELECT MAX(id) " +
                "    FROM messages " +
                "    WHERE (sender_id = u.id AND receiver_id = ?) OR (sender_id = ? AND receiver_id = u.id) " +
                "    AND sent_at = ( " +
                "        SELECT MAX(sent_at) " +
                "        FROM messages " +
                "        WHERE (sender_id = u.id AND receiver_id = ?) OR (sender_id = ? AND receiver_id = u.id) " +
                "    ) " +
                ") " +
                "ORDER BY m.sent_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.setInt(5, userId);
            stmt.setInt(6, userId);
            stmt.setInt(7, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserMessage userMessage = new UserMessage(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
                    );
                    userMessages.add(userMessage);
                }
            }
        }
        return userMessages;
    }

}