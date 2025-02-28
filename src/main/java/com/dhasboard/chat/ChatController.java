package com.dhasboard.chat;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatController {

    private Set<String> displayedMessages = new HashSet<>();
    @FXML private TextField messageField;
    @FXML private VBox chatBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField searchField;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final int userId = 2;
    private int receiverId;
    private MessageDao messageDao = new MessageDao();
    private LocalDateTime sent_at = LocalDateTime.now();
    @FXML private Label UserMessage;
    @FXML
    private ListView<UserMessage> userList;
    private boolean userScrolledUp = false;

    public void initialize() {
        try {
            setupNetworkConnection();
            loadPreviousMessages();
            startMessageUpdater();
            startMessageListener();
            loadUserList();
            setupSearchListener();
            setupUserSelectionListener();
            setupScrollListener();
            startAutoRefresh();
        } catch (IOException e) {
            showError("Connection error: " + e.getMessage());
        }
    }

    private void setupUserSelectionListener() {
        userList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                receiverId = newValue.getUserId();
                System.out.println("Selected User ID: " + receiverId);
                loadChatWithUser(receiverId);
                UserMessage.setText(newValue.getUsername());
            }
        });
    }

    private void setupScrollListener() {
        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            userScrolledUp = (newValue.doubleValue() < 1.0);
        });
    }

    private void startAutoRefresh() {
        Timeline refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> userList.refresh())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void setupNetworkConnection() throws IOException {
        socket = new Socket("localhost", 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(userId);
    }

    private void loadPreviousMessages() {
        try {
            List<Message> messages = messageDao.getMessagesBetweenUsers(userId, receiverId);
            for (Message message : messages) {
                addMessageToUI(message.getSenderId(), message.getContent(), message.isSeen());
            }
            scrollToBottom();
        } catch (SQLException e) {
            showError("Failed to load messages: " + e.getMessage());
        }
    }

    private void startMessageUpdater() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            checkForNewMessages();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("SEEN_UPDATE:")) {
                        int senderId = Integer.parseInt(message.split(":")[1]);
                        Platform.runLater(() -> {
                            try {
                                messageDao.markMessagesAsSeen(userId, senderId);
                                refreshUserList();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });
                    } else if (message.startsWith("MSG:")) {
                        String[] parts = message.split(":", 4);
                        int senderId = Integer.parseInt(parts[1]);
                        String content = parts[2];
                        int messageId = Integer.parseInt(parts[3]);
                        Platform.runLater(() -> {
                            addMessageToUI(senderId, content, false);
                            scrollToBottom();
                            refreshUserList();
                        });
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> showError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    private void checkForNewMessages() {
        try {
            List<Message> newMessages = messageDao.getNewMessages(userId, receiverId, sent_at);
            for (Message message : newMessages) {
                addMessageToUI(message.getSenderId(), message.getContent(), message.isSeen());
            }
            sent_at = LocalDateTime.now();
            scrollToBottom();
        } catch (SQLException e) {
            showError("Error checking new messages: " + e.getMessage());
        }
    }

    @FXML
    private void sendMessage(ActionEvent event) {
        String content = messageField.getText().trim();
        if (!content.isEmpty()) {
            Message message = new Message(userId, receiverId, content, sent_at);
            try {
                messageDao.saveMessage(message);
                out.println("MSG:" + receiverId + ":" + content + ":" + message.getId());
                addMessageToUI(userId, content, true);
                messageField.clear();
                scrollToBottom();
                refreshUserList();
            } catch (SQLException e) {
                showError("Failed to send message: " + e.getMessage());
            }
        }
    }

    private void addMessageToUI(int senderId, String content, boolean isSeen) {
        String messageKey = senderId + ":" + content + sent_at;
        if (!displayedMessages.contains(messageKey)) {
            HBox messageContainer = new HBox();
            messageContainer.setAlignment(senderId == userId ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            messageContainer.setPadding(new Insets(5, 10, 5, 10));

            TextFlow textFlow = new TextFlow(new Text(content));
            textFlow.setMaxWidth(300);
            textFlow.setPadding(new Insets(8));
            String baseStyle = senderId == userId ?
                    "-fx-background-color: #0084ff; -fx-background-radius: 15; -fx-text-fill: white;" :
                    "-fx-background-color: #e0e0e0; -fx-background-radius: 15;";

            if (senderId != userId && !isSeen) {
                baseStyle += "-fx-font-weight: bold;";
            }

            textFlow.setStyle(baseStyle);
            messageContainer.getChildren().add(textFlow);
            chatBox.getChildren().add(messageContainer);

            displayedMessages.add(messageKey);
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (scrollPane != null && !userScrolledUp) {
                scrollPane.setVvalue(1.0);
                scrollPane.requestLayout();
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void loadUserList() {
        try {
            List<UserMessage> userMessages = messageDao.getUsersWithLastMessage(userId);

            userList.getItems().setAll(userMessages);

            userList.setCellFactory(param -> new ListCell<UserMessage>() {
                @Override
                protected void updateItem(UserMessage userMessage, boolean empty) {
                    super.updateItem(userMessage, empty);

                    if (empty || userMessage == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        HBox hbox = new HBox(10);
                        hbox.setAlignment(Pos.CENTER_LEFT);

                        Label usernameLabel = new Label(userMessage.getUsername());
                        usernameLabel.setStyle("-fx-font-weight: bold;");

                        Label lastMessageLabel = new Label(userMessage.getLastMessage());

                        if (userMessage.getSenderId() != userId && !userMessage.isSeen()) {
                            lastMessageLabel.setStyle("-fx-font-weight: bold;");
                        } else {
                            lastMessageLabel.setStyle("-fx-font-weight: normal;");
                        }

                        Label timeAgoLabel = new Label(userMessage.getTimeAgo());
                        timeAgoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

                        hbox.getChildren().addAll(usernameLabel, lastMessageLabel, timeAgoLabel);
                        setGraphic(hbox);
                    }
                }
            });
        } catch (SQLException e) {
            showError("Failed to load user list: " + e.getMessage());
        }
    }

    private void loadChatWithUser(int otherUserId) {
        try {
            messageDao.markMessagesAsSeen(userId, otherUserId);
            out.println("SEEN:" + otherUserId);

            chatBox.getChildren().clear();
            List<Message> messages = messageDao.getMessagesBetweenUsers(userId, otherUserId);
            for (Message message : messages) {
                addMessageToUI(message.getSenderId(), message.getContent(), message.isSeen());
            }
            scrollToBottom();
        } catch (SQLException e) {
            showError("Failed to load messages: " + e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
                if (newValue.isEmpty()) {
                    loadUserList();
                } else {
                    searchUsers(newValue);
                }
            }));
            timeline.stop();
            timeline.play();
        });
    }

    private void searchUsers(String query) {
        try {
            List<UserMessage> searchResults = messageDao.searchUsers(userId, query);
            userList.getItems().setAll(searchResults);
        } catch (SQLException e) {
            showError("Search error: " + e.getMessage());
        }
    }

    private void refreshUserList() {
        try {
            List<UserMessage> updatedList = messageDao.getUsersWithLastMessage(userId);
            Platform.runLater(() -> {
                userList.getItems().setAll(updatedList);
            });
        } catch (SQLException e) {
            showError("Error refreshing user list: " + e.getMessage());
        }
    }
}