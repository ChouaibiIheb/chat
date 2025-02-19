package com.dhasboard.chat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class UserDAO {
    public List<UserMessage> getRecentContactsWithLastMessage(int currentUserId) {
        String query = "SELECT u.id, u.username, m.content, m.sent_at FROM users u "
                + "JOIN ("
                + "  SELECT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS contact_id, "
                + "         content, sent_at, "
                + "         ROW_NUMBER() OVER (PARTITION BY CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END ORDER BY sent_at DESC) AS rn "
                + "  FROM messages "
                + "  WHERE sender_id = ? OR receiver_id = ?"
                + ") m ON u.id = m.contact_id "
                + "WHERE m.rn = 1 AND u.id != ? "
                + "ORDER BY m.sent_at DESC";

        List<UserMessage> contacts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentUserId);
            stmt.setInt(2, currentUserId);
            stmt.setInt(3, currentUserId);
            stmt.setInt(4, currentUserId);
            stmt.setInt(5, currentUserId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String lastMessage = rs.getString("content");
                LocalDateTime sentAt = rs.getTimestamp("sent_at").toLocalDateTime();

                contacts.add(new UserMessage(userId, username, lastMessage, sentAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }
}