package com.dhasboard.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static MessageDao messageDao = new MessageDao();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int userId;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                userId = Integer.parseInt(in.readLine());
                System.out.println("User " + userId + " connected");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("SEEN:")) {
                        int senderId = Integer.parseInt(inputLine.split(":")[1]);
                        messageDao.markMessagesAsSeen(userId, senderId);
                        for (ClientHandler client : clients) {
                            if (client.userId == senderId) {
                                client.out.println("SEEN_UPDATE:" + userId);
                            }
                        }
                    } else if (inputLine.startsWith("MSG:")) {
                        String[] parts = inputLine.split(":", 4);
                        int receiverId = Integer.parseInt(parts[1]);
                        String content = parts[2];
                        int messageId = Integer.parseInt(parts[3]);

                        LocalDateTime sentAt = LocalDateTime.now();
                        Message message = new Message(userId, receiverId, content, sentAt);
                        message.setId(messageId);
                        messageDao.saveMessage(message);
                        forwardMessage(receiverId, message);
                    }
                }
            } catch (IOException | SQLException | NumberFormatException e) {
                System.err.println("Client error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
                clients.remove(this);
            }
        }

        private void forwardMessage(int receiverId, Message message) {
            for (ClientHandler client : clients) {
                if (client.userId == receiverId) {
                    client.out.println("MSG:" + message.getSenderId() + ":" + message.getContent() + ":" + message.getId());
                } else if (client.userId == message.getSenderId()) {
                    client.out.println("SEEN_UPDATE:" + message.getId());
                }
            }
        }
    }
}