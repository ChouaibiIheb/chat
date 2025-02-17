package com.dhasboard.chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static int userId;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.print("أدخل معرف المستخدم الخاص بك: ");
            userId = scanner.nextInt();
            scanner.nextLine();
            out.println("LOGIN:" + userId);

            // استقبال الرسائل من الخادم
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // إرسال الرسائل
            while (true) {
                System.out.print("أدخل معرف المستلم والرسالة (مثال: 2:مرحبا!): ");
                String message = scanner.nextLine();
                out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
