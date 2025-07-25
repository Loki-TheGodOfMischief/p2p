package client;

import java.io.*;
import java.net.Socket;
import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import common.Message;
import client.AESUtil;
import javafx.application.Platform;

public class ClientConnectionGUI {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SecretKey aesKey;
    private String username;
    private boolean isConnected = false;
    private ChatClientGUI gui;

    public ClientConnectionGUI(String serverAddress, int serverPort, ChatClientGUI gui) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.gui = gui;
    }

    public void connect() throws Exception {
        socket = new Socket(serverAddress, serverPort);
        
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // 1. Perform key exchange
        if (!performKeyExchange()) {
            throw new Exception("Key exchange failed!");
        }

        // 2. Perform RSA authentication (optional)
        if (!performRSAAuthentication()) {
            throw new Exception("RSA authentication failed!");
        }

        // 3. Perform user authentication
        if (!performUserAuthentication()) {
            throw new Exception("User authentication failed!");
        }

        isConnected = true;

        // Start message listener thread
        new Thread(this::listenForMessages).start();
    }

    private boolean performKeyExchange() throws Exception {
        // Receive server's public key
        PublicKey serverPublicKey = (PublicKey) in.readObject();

        // Generate AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        aesKey = keyGen.generateKey();

        // Encrypt AES key with server's public key
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Send encrypted AES key to server
        out.writeObject(encryptedAESKey);

        return true;
    }

    private boolean performRSAAuthentication() throws Exception {
        try {
            // Load client's keys (optional - create dummy keys if not available)
            PrivateKey clientPrivateKey;
            PublicKey clientPublicKey;
            
            try {
                clientPrivateKey = CryptoUtil.loadPrivateKey("client/private_key.der");
                clientPublicKey = CryptoUtil.loadPublicKey("client/public_key.der");
            } catch (Exception e) {
                // Generate temporary keys if files not found
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                clientPrivateKey = kp.getPrivate();
                clientPublicKey = kp.getPublic();
            }

            // Send public key to server
            out.writeObject(clientPublicKey);

            // Receive challenge from server
            byte[] challenge = (byte[]) in.readObject();

            // Sign challenge
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(clientPrivateKey);
            signature.update(challenge);
            byte[] signedChallenge = signature.sign();

            // Send signature to server
            out.writeObject(signedChallenge);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean performUserAuthentication() throws Exception {
        // Wait for authentication request
        String authRequest = (String) decryptMessage();
        if (!"AUTH_REQUEST".equals(authRequest)) {
            return false;
        }

        // Show authentication dialog on GUI thread
        Platform.runLater(() -> gui.showAuthenticationDialog());
        
        // Wait for authentication to complete
        synchronized (this) {
            while (username == null && isConnected) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        
        return username != null;
    }

    public void authenticateUser(AuthenticationDialog.AuthData authData) {
        try {
            if (authData.isLogin) {
                sendEncryptedMessage("LOGIN");
                
                // Wait for login request
                String loginRequest = (String) decryptMessage();
                if (!"LOGIN_REQUEST".equals(loginRequest)) {
                    return;
                }

                // Send credentials
                sendEncryptedMessage(authData.username);
                sendEncryptedMessage(authData.password);
                
                // Wait for response
                String response = (String) decryptMessage();
                if ("AUTH_SUCCESS".equals(response)) {
                    synchronized (this) {
                        username = authData.username;
                        notify();
                    }
                    gui.onConnectionEstablished(username);
                } else {
                    Platform.runLater(() -> {
                        gui.onServerResponse("Login failed: " + response);
                        gui.showAuthenticationDialog(); // Show dialog again
                    });
                }
            } else {
                // Registration
                sendEncryptedMessage("REGISTER");
                
                // Wait for registration request
                String registerRequest = (String) decryptMessage();
                if (!"REGISTER_REQUEST".equals(registerRequest)) {
                    return;
                }

                // Send credentials
                sendEncryptedMessage(authData.username);
                sendEncryptedMessage(authData.password);
                
                // Wait for response
                String response = (String) decryptMessage();
                if ("AUTH_SUCCESS".equals(response)) {
                    synchronized (this) {
                        username = authData.username;
                        notify();
                    }
                    gui.onConnectionEstablished(username);
                } else {
                    Platform.runLater(() -> {
                        gui.onServerResponse("Registration failed: " + response);
                        gui.showAuthenticationDialog(); // Show dialog again
                    });
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                gui.onServerResponse("Authentication error: " + e.getMessage());
                gui.showAuthenticationDialog(); // Show dialog again
            });
        }
    }

    public void sendChatMessage(String message) {
        try {
            Message chatMessage = new Message(username, message, "CHAT");
            sendEncryptedMessage(chatMessage);
        } catch (Exception e) {
            Platform.runLater(() -> gui.onServerResponse("Error sending message: " + e.getMessage()));
        }
    }

    public void changePassword(String oldPassword, String newPassword) {
        try {
            String changeCommand = "CHANGE_PASSWORD:" + oldPassword + "|" + newPassword;
            sendEncryptedMessage(changeCommand);
        } catch (Exception e) {
            Platform.runLater(() -> gui.onServerResponse("Error changing password: " + e.getMessage()));
        }
    }

    public void requestUserInfo() {
        try {
            sendEncryptedMessage("USER_INFO");
        } catch (Exception e) {
            Platform.runLater(() -> gui.onServerResponse("Error requesting user info: " + e.getMessage()));
        }
    }

    private void listenForMessages() {
        try {
            while (isConnected) {
                Object receivedData = decryptMessage();
                
                if (receivedData instanceof Message) {
                    Message message = (Message) receivedData;
                    gui.onMessageReceived(message);
                } else if (receivedData instanceof String) {
                    String response = (String) receivedData;
                    handleServerResponse(response);
                }
            }
        } catch (Exception e) {
            if (isConnected) {
                Platform.runLater(() -> {
                    gui.onServerResponse("Connection lost: " + e.getMessage());
                    gui.onDisconnected();
                });
                isConnected = false;
            }
        }
    }

    private void handleServerResponse(String response) {
        Platform.runLater(() -> {
            if (response.startsWith("PASSWORD_CHANGED:")) {
                gui.onServerResponse("Password changed: " + response.substring(17));
            } else if (response.startsWith("PASSWORD_ERROR:")) {
                gui.onServerResponse("Password error: " + response.substring(15));
            } else if (response.startsWith("USER_INFO:")) {
                gui.onServerResponse("User info: " + response.substring(10));
            } else if (response.startsWith("UNKNOWN_COMMAND:")) {
                gui.onServerResponse("Unknown command: " + response.substring(16));
            } else {
                gui.onServerResponse(response);
            }
        });
    }

    private void sendEncryptedMessage(Object message) throws Exception {
        byte[] encrypted = AESUtil.encryptObject((Serializable) message, aesKey);
        out.writeObject(encrypted);
    }

    private Object decryptMessage() throws Exception {
        byte[] encrypted = (byte[]) in.readObject();
        return AESUtil.decryptObject(encrypted, aesKey);
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (username != null) {
                sendEncryptedMessage("QUIT");
            }
        } catch (Exception e) {
            // Ignore errors during disconnect
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        synchronized (this) {
            notify(); // Wake up any waiting authentication
        }
    }
}