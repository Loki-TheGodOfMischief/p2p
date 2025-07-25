package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import common.Message;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ChatClientGUI extends Application {
    private ClientConnectionGUI clientConnection;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button connectButton;
    private Button disconnectButton;
    private Label statusLabel;
    private Label usernameLabel;
    
    // Connection settings
    private TextField serverAddressField;
    private TextField serverPortField;
    
    private Stage primaryStage;
    private boolean isConnected = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Secure Chat Client");
        primaryStage.setOnCloseRequest(e -> {
            if (clientConnection != null) {
                clientConnection.disconnect();
            }
            Platform.exit();
        });

        VBox root = createMainLayout();
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createMainLayout() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Connection panel
        VBox connectionPanel = createConnectionPanel();
        
        // Status panel
        HBox statusPanel = createStatusPanel();
        
        // Chat area
        VBox chatPanel = createChatPanel();
        
        // Message input panel
        HBox messagePanel = createMessagePanel();

        root.getChildren().addAll(connectionPanel, statusPanel, chatPanel, messagePanel);
        VBox.setVgrow(chatPanel, Priority.ALWAYS);

        return root;
    }

    private VBox createConnectionPanel() {
        VBox panel = new VBox(5);
        panel.getStyleClass().add("connection-panel");
        
        Label titleLabel = new Label("Connection Settings");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        HBox serverSettings = new HBox(10);
        serverSettings.setAlignment(Pos.CENTER_LEFT);
        
        Label addressLabel = new Label("Server:");
        serverAddressField = new TextField("localhost");
        serverAddressField.setPrefWidth(150);
        
        Label portLabel = new Label("Port:");
        serverPortField = new TextField("12345");
        serverPortField.setPrefWidth(80);
        
        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> connectToServer());
        connectButton.getStyleClass().add("connect-button");
        
        disconnectButton = new Button("Disconnect");
        disconnectButton.setOnAction(e -> disconnectFromServer());
        disconnectButton.setDisable(true);
        disconnectButton.getStyleClass().add("disconnect-button");
        
        serverSettings.getChildren().addAll(
            addressLabel, serverAddressField,
            portLabel, serverPortField,
            connectButton, disconnectButton
        );
        
        panel.getChildren().addAll(titleLabel, serverSettings);
        return panel;
    }

    private HBox createStatusPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.getStyleClass().add("status-panel");
        
        statusLabel = new Label("Disconnected");
        statusLabel.getStyleClass().add("status-disconnected");
        
        usernameLabel = new Label("");
        usernameLabel.getStyleClass().add("username-label");
        
        panel.getChildren().addAll(new Label("Status:"), statusLabel, usernameLabel);
        return panel;
    }

    private VBox createChatPanel() {
        VBox panel = new VBox(5);
        
        Label chatLabel = new Label("Chat Messages");
        chatLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.getStyleClass().add("chat-area");
        chatArea.setFont(Font.font("Consolas", 12));
        
        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        panel.getChildren().addAll(chatLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return panel;
    }

    private HBox createMessagePanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER);
        
        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        messageField.setDisable(true);
        messageField.setOnAction(e -> sendMessage());
        
        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());
        sendButton.getStyleClass().add("send-button");
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> chatArea.clear());
        clearButton.getStyleClass().add("clear-button");
        
        // Command buttons
        Button passwordButton = new Button("Change Password");
        passwordButton.setDisable(true);
        passwordButton.setOnAction(e -> changePassword());
        passwordButton.getStyleClass().add("command-button");
        
        Button infoButton = new Button("User Info");
        infoButton.setDisable(true);
        infoButton.setOnAction(e -> getUserInfo());
        infoButton.getStyleClass().add("command-button");
        
        HBox.setHgrow(messageField, Priority.ALWAYS);
        
        panel.getChildren().addAll(
            messageField, sendButton, clearButton, 
            new Separator(), passwordButton, infoButton
        );
        
        return panel;
    }

    private void connectToServer() {
        String address = serverAddressField.getText().trim();
        String portText = serverPortField.getText().trim();
        
        if (address.isEmpty() || portText.isEmpty()) {
            showAlert("Error", "Please enter server address and port");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid port number");
            return;
        }
        
        // Disable connection controls
        connectButton.setDisable(true);
        serverAddressField.setDisable(true);
        serverPortField.setDisable(true);
        updateStatus("Connecting...", false);
        
        // Create connection in background thread
        Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                clientConnection = new ClientConnectionGUI(address, port, ChatClientGUI.this);
                clientConnection.connect();
                return null;
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    serverAddressField.setDisable(false);
                    serverPortField.setDisable(false);
                    updateStatus("Connection failed", false);
                    showAlert("Connection Error", "Failed to connect to server: " + getException().getMessage());
                });
            }
        };
        
        new Thread(connectTask).start();
    }

    private void disconnectFromServer() {
        if (clientConnection != null) {
            clientConnection.disconnect();
        }
        onDisconnected();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || !isConnected) {
            return;
        }
        
        messageField.clear();
        
        if (clientConnection != null) {
            clientConnection.sendChatMessage(message);
        }
    }

    private void changePassword() {
        PasswordChangeDialog dialog = new PasswordChangeDialog();
        Optional<PasswordChangeDialog.PasswordData> result = dialog.showAndWait();
        
        result.ifPresent(passwordData -> {
            if (clientConnection != null) {
                clientConnection.changePassword(passwordData.oldPassword, passwordData.newPassword);
            }
        });
    }

    private void getUserInfo() {
        if (clientConnection != null) {
            clientConnection.requestUserInfo();
        }
    }

    // Methods called by ClientConnectionGUI
    public void onConnectionEstablished(String username) {
        Platform.runLater(() -> {
            this.isConnected = true;
            disconnectButton.setDisable(false);
            messageField.setDisable(false);
            sendButton.setDisable(false);
            
            // Enable command buttons
            ((Button) ((HBox) ((VBox) primaryStage.getScene().getRoot()).getChildren().get(3))
                .getChildren().get(4)).setDisable(false); // Password button
            ((Button) ((HBox) ((VBox) primaryStage.getScene().getRoot()).getChildren().get(3))
                .getChildren().get(5)).setDisable(false); // Info button
            
            updateStatus("Connected", true);
            usernameLabel.setText("User: " + username);
            messageField.requestFocus();
        });
    }

    public void onDisconnected() {
        Platform.runLater(() -> {
            this.isConnected = false;
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            serverAddressField.setDisable(false);
            serverPortField.setDisable(false);
            messageField.setDisable(true);
            sendButton.setDisable(true);
            
            // Disable command buttons
            ((Button) ((HBox) ((VBox) primaryStage.getScene().getRoot()).getChildren().get(3))
                .getChildren().get(4)).setDisable(true); // Password button
            ((Button) ((HBox) ((VBox) primaryStage.getScene().getRoot()).getChildren().get(3))
                .getChildren().get(5)).setDisable(true); // Info button
            
            updateStatus("Disconnected", false);
            usernameLabel.setText("");
        });
    }

    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            String timestamp = message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String displayMessage;
            
            if ("SYSTEM".equals(message.getFrom())) {
                displayMessage = String.format("[%s] SYSTEM: %s\n", timestamp, message.getContent());
            } else {
                displayMessage = String.format("[%s] %s: %s\n", timestamp, message.getFrom(), message.getContent());
            }
            
            chatArea.appendText(displayMessage);
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void onServerResponse(String response) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            chatArea.appendText(String.format("[%s] SERVER: %s\n", timestamp, response));
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void showAuthenticationDialog() {
        Platform.runLater(() -> {
            AuthenticationDialog dialog = new AuthenticationDialog();
            Optional<AuthenticationDialog.AuthData> result = dialog.showAndWait();
            
            result.ifPresent(authData -> {
                if (clientConnection != null) {
                    clientConnection.authenticateUser(authData);
                }
            });
        });
    }

    private void updateStatus(String status, boolean connected) {
        statusLabel.setText(status);
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add(connected ? "status-connected" : "status-disconnected");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}