package com.dhasboard.chat;

import com.dhasboard.chat.MessageDao;
import com.dhasboard.chat.Message;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
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
                // Authentification
                userId = Integer.parseInt(in.readLine());
                System.out.println("User " + userId + " connected");

                // Gestion des messages
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.split(":", 2);
                    if (parts.length < 2) continue;

                    int receiverId = Integer.parseInt(parts[0]);
                    String content = parts[1];

                    // Sauvegarde en base
                    Message message = new Message(userId, receiverId, content);
                    messageDao.saveMessage(message);

                    // Envoi au destinataire
                    forwardMessage(receiverId, message);
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
                    client.out.println(message.getSenderId() + ":" + message.getContent());
                }
            }
        }
    }
}