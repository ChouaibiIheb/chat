package com.dhasboard.chat;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    private static final Connection connection;

    static {
        try {
            connection = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMessage(Message message) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content, sent_at, seen) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, message.getSenderId());
            stmt.setInt(2, message.getReceiverId());
            stmt.setString(3, message.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(message.getSentAt()));
            stmt.setBoolean(5, message.isSeen());
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
        String sql = "SELECT *, (seen IS FALSE AND sender_id != ?) AS is_unread FROM messages " +
                "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY sent_at ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user1);
            stmt.setInt(2, user1);
            stmt.setInt(3, user2);
            stmt.setInt(4, user2);
            stmt.setInt(5, user1);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message(
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
                    );
                    message.setId(rs.getInt("id"));
                    message.setSeen(rs.getBoolean("seen"));
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public List<Message> getNewMessages(int user1, int user2, LocalDateTime lastCheck) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages " +
                "WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                "AND sent_at > ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
                    message.setSeen(rs.getBoolean("seen"));
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public List<UserMessage> getUsersWithLastMessage(int userId) throws SQLException {
        List<UserMessage> userMessages = new ArrayList<>();
        String sql = "SELECT u.id AS user_id, u.username, m.content, m.sent_at, m.seen, m.sender_id " +
                "FROM users u " +
                "JOIN messages m ON (u.id = m.sender_id OR u.id = m.receiver_id) " +
                "WHERE (m.sender_id = ? OR m.receiver_id = ?) AND u.id != ? " +
                "AND m.id = ( " +
                "    SELECT MAX(id) " +
                "    FROM messages " +
                "    WHERE (sender_id = u.id AND receiver_id = ?) OR (sender_id = ? AND receiver_id = u.id) " +
                ") " +
                "ORDER BY m.sent_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.setInt(5, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserMessage userMessage = new UserMessage(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime(),
                            rs.getBoolean("seen"),
                            rs.getInt("sender_id")
                    );
                    userMessages.add(userMessage);
                }
            }
        }
        return userMessages;
    }

    public List<UserMessage> searchUsers(int currentUserId, String query) throws SQLException {
        List<UserMessage> userMessages = new ArrayList<>();
        String sql = "SELECT u.id AS user_id, u.username, m.content, m.sent_at, m.seen, m.sender_id " +
                "FROM users u " +
                "LEFT JOIN messages m ON (u.id = m.sender_id OR u.id = m.receiver_id) " +
                "WHERE u.username LIKE ? AND u.id != ? " +
                "GROUP BY u.id " +
                "ORDER BY m.sent_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            stmt.setInt(2, currentUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserMessage userMessage = new UserMessage(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at") != null ?
                                    rs.getTimestamp("sent_at").toLocalDateTime() : LocalDateTime.now(),
                            rs.getBoolean("seen"),
                            rs.getInt("sender_id")
                    );
                    userMessages.add(userMessage);
                }
            }
        }
        return userMessages;
    }

    public void markMessagesAsSeen(int receiverId, int senderId) throws SQLException {
        String sql = "UPDATE messages SET seen = true, seen_at = NOW() " +
                "WHERE sender_id = ? AND receiver_id = ? AND seen = false";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            stmt.executeUpdate();
        }
    }
}