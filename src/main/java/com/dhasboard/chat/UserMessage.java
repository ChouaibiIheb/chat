package com.dhasboard.chat;


import java.time.LocalDateTime;

    public class UserMessage {
        private int userId;
        private String username;
        private String lastMessage;
        private LocalDateTime sentAt;

        public UserMessage(int userId, String username, String lastMessage, LocalDateTime sentAt) {
            this.userId = userId;
            this.username = username;
            this.lastMessage = lastMessage;
            this.sentAt = sentAt;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }
        @Override
        public String toString() {
            return username + ": " + lastMessage + " (" + sentAt.toLocalTime().toString() + ")";
        }
    }


