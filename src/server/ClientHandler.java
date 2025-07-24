package server;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import common.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private List<ClientHandler> clients;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private SecretKey aesKey;
    private String username;
    private UserManager userManager;
    private boolean isAuthenticated = false;

    public ClientHandler(Socket socket, List<ClientHandler> clients, UserManager userManager) {
        this.socket = socket;
        this.clients = clients;
        this.userManager = userManager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            LoggerUtil.log("Client connected from: " + socket.getInetAddress());

            // 1. Key exchange
            if (!performKeyExchange()) {
                LoggerUtil.log("Key exchange failed for client: " + socket.getInetAddress());
                return;
            }

            // 2. RSA-based authentication (optional - for additional security)
            if (!performRSAAuthentication()) {
                LoggerUtil.log("RSA authentication failed for client: " + socket.getInetAddress());
                return;
            }

            // 3. Username/Password authentication
            if (!performUserAuthentication()) {
                LoggerUtil.log("User authentication failed for client: " + socket.getInetAddress());
                return;
            }

            LoggerUtil.log("Client fully authenticated: " + username + " from " + socket.getInetAddress());
            isAuthenticated = true;

            // Send welcome message
            sendSystemMessage("Welcome to the secure chat, " + username + "!");
            broadcastSystemMessage(username + " has joined the chat.");

            // 4. Chat loop
            chatLoop();

        } catch (Exception e) {
            LoggerUtil.log("Client handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean performKeyExchange() throws Exception {
        // Generate RSA key pair for this session
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // Send public key to client
        out.writeObject(kp.getPublic());

        // Receive AES key encrypted with our public key
        byte[] encryptedAESKey = (byte[]) in.readObject();
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, kp.getPrivate());
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);

        aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        LoggerUtil.log("Key exchange completed successfully");
        return true;
    }

    private boolean performRSAAuthentication() throws Exception {
        // Optional RSA authentication step (can be skipped if only using username/password)
        try {
            PublicKey clientPublicKey = (PublicKey) in.readObject();

            // Send challenge
            byte[] challenge = new byte[32];
            new SecureRandom().nextBytes(challenge);
            out.writeObject(challenge);

            // Receive signature
            byte[] signature = (byte[]) in.readObject();

            // Verify signature
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clientPublicKey);
            sig.update(challenge);
            
            boolean rsaValid = sig.verify(signature);
            LoggerUtil.log("RSA authentication result: " + rsaValid);
            return rsaValid;
        } catch (Exception e) {
            LoggerUtil.log("RSA authentication error: " + e.getMessage());
            return false;
        }
    }

    private boolean performUserAuthentication() throws Exception {
        // Send authentication request
        sendEncryptedMessage("AUTH_REQUEST");

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Receive authentication type
                String authType = (String) decryptMessage();
                
                if ("LOGIN".equals(authType)) {
                    if (handleLogin()) {
                        return true;
                    }
                } else if ("REGISTER".equals(authType)) {
                    if (handleRegistration()) {
                        return true;
                    }
                } else {
                    sendEncryptedMessage("AUTH_ERROR:Invalid authentication type");
                    continue;
                }

                if (attempt < maxAttempts) {
                    sendEncryptedMessage("AUTH_RETRY:Attempt " + (attempt + 1) + " of " + maxAttempts);
                } else {
                    sendEncryptedMessage("AUTH_FAILED:Maximum attempts exceeded");
                }

            } catch (Exception e) {
                LoggerUtil.log("Authentication attempt " + attempt + " error: " + e.getMessage());
                sendEncryptedMessage("AUTH_ERROR:Authentication error");
            }
        }

        return false;
    }

    private boolean handleLogin() throws Exception {
        sendEncryptedMessage("LOGIN_REQUEST");
        
        // Receive username
        String receivedUsername = (String) decryptMessage();
        
        // Receive password
        String password = (String) decryptMessage();

        if (userManager.authenticateUser(receivedUsername, password)) {
            username = receivedUsername;
            sendEncryptedMessage("AUTH_SUCCESS");
            LoggerUtil.log("User login successful: " + username);
            return true;
        } else {
            sendEncryptedMessage("AUTH_FAILED:Invalid credentials");
            LoggerUtil.log("Login failed for username: " + receivedUsername);
            return false;
        }
    }

    private boolean handleRegistration() throws Exception {
        sendEncryptedMessage("REGISTER_REQUEST");
        
        // Receive username
        String newUsername = (String) decryptMessage();
        
        // Receive password
        String newPassword = (String) decryptMessage();

        if (userManager.registerUser(newUsername, newPassword)) {
            username = newUsername;
            sendEncryptedMessage("AUTH_SUCCESS");
            LoggerUtil.log("User registration successful: " + username);
            return true;
        } else {
            sendEncryptedMessage("AUTH_FAILED:Registration failed - username may already exist or password too weak");
            LoggerUtil.log("Registration failed for username: " + newUsername);
            return false;
        }
    }

    private void chatLoop() throws Exception {
        while (true) {
            Object receivedData = decryptMessage();
            
            if (receivedData instanceof Message) {
                Message msg = (Message) receivedData;
                LoggerUtil.log("Message from " + username + ": " + msg.getContent());
                broadcast(msg);
            } else if (receivedData instanceof String) {
                String command = (String) receivedData;
                handleCommand(command);
            }
        }
    }

    private void handleCommand(String command) throws Exception {
        String[] parts = command.split(":", 2);
        String cmd = parts[0];

        switch (cmd) {
            case "CHANGE_PASSWORD":
                if (parts.length == 2) {
                    String[] passwords = parts[1].split("\\|", 2);
                    if (passwords.length == 2) {
                        String oldPassword = passwords[0];
                        String newPassword = passwords[1];
                        
                        if (userManager.changePassword(username, oldPassword, newPassword)) {
                            sendEncryptedMessage("PASSWORD_CHANGED:Password changed successfully");
                            LoggerUtil.log("Password changed for user: " + username);
                        } else {
                            sendEncryptedMessage("PASSWORD_ERROR:Failed to change password");
                        }
                    }
                }
                break;
                
            case "USER_INFO":
                UserManager.UserInfo userInfo = userManager.getUserInfo(username);
                if (userInfo != null) {
                    String info = String.format("USER_INFO:Username: %s, Created: %s, Last Login: %s, Active: %s",
                            userInfo.getUsername(),
                            new Date(userInfo.getCreatedDate()),
                            userInfo.getLastLoginDate() > 0 ? new Date(userInfo.getLastLoginDate()) : "Never",
                            userInfo.isActive() ? "Yes" : "No");
                    sendEncryptedMessage(info);
                }
                break;

            case "QUIT":
                LoggerUtil.log("User " + username + " disconnected gracefully");
                return;

            default:
                sendEncryptedMessage("UNKNOWN_COMMAND:Command not recognized");
        }
    }

    private void sendEncryptedMessage(String message) throws Exception {
        byte[] encrypted = AESUtil.encryptObject((Serializable) message, aesKey);
        out.writeObject(encrypted);
    }

    private Object decryptMessage() throws Exception {
        byte[] encrypted = (byte[]) in.readObject();
        return AESUtil.decryptObject(encrypted, aesKey);
    }

    private void broadcast(Message msg) throws Exception {
        for (ClientHandler client : clients) {
            if (client != this && client.isAuthenticated) {
                client.sendMessage(msg);
            }
        }
    }

    private void broadcastSystemMessage(String message) throws Exception {
        Message systemMsg = new Message("SYSTEM", message);
        for (ClientHandler client : clients) {
            if (client.isAuthenticated) {
                client.sendMessage(systemMsg);
            }
        }
    }

    private void sendSystemMessage(String message) throws Exception {
        Message systemMsg = new Message("SYSTEM", message);
        sendMessage(systemMsg);
    }

    private void sendMessage(Message msg) throws Exception {
        byte[] encrypted = AESUtil.encryptObject((Serializable) msg, aesKey);
        out.writeObject(encrypted);
    }

    private void cleanup() {
        clients.remove(this);
        if (username != null && isAuthenticated) {
            try {
                broadcastSystemMessage(username + " has left the chat.");
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            LoggerUtil.log("User " + username + " disconnected");
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }
}