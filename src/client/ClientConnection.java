package client;

import java.io.*;
import java.net.Socket;
import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import java.util.Scanner;
import common.Message;
import client.AESUtil;
import java.util.HashMap;
import java.util.Map;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Base64;

public class ClientConnection {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SecretKey aesKey;
    private String username;
    private boolean isConnected = false;
    private Map<String, PublicKey> userPublicKeys = new HashMap<>();

    public ClientConnection(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void connect() {
        try {
            // Ensure RSA key pair exists for E2EE
            File privKeyFile = new File("client/private_key.der");
            File pubKeyFile = new File("client/public_key.der");
            if (!privKeyFile.exists() || !pubKeyFile.exists()) {
                System.out.println("Generating new RSA key pair for end-to-end encryption...");
                CryptoUtil.generateAndSaveRSAKeyPair("client/public_key.der", "client/private_key.der");
                System.out.println("RSA key pair generated and saved.");
            }

            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // 1. Perform key exchange
            if (!performKeyExchange()) {
                System.out.println("Key exchange failed!");
                return;
            }

            // 2. Perform RSA authentication (optional)
            if (!performRSAAuthentication()) {
                System.out.println("RSA authentication failed!");
                return;
            }

            // 3. Perform user authentication
            if (!performUserAuthentication()) {
                System.out.println("User authentication failed!");
                return;
            }

            // 4. Send our public key to the server for E2EE
            sendOwnPublicKey();
            // 5. Receive all public keys from the server
            receiveAllPublicKeys();

            System.out.println("Successfully authenticated as: " + username);
            isConnected = true;

            // Start message listener thread
            new Thread(this::listenForMessages).start();

            // Start chat interface
            startChatInterface();

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private boolean performKeyExchange() throws Exception {
        // Receive server's public key
        PublicKey serverPublicKey = (PublicKey) in.readObject();
        System.out.println("Received server's public key");

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
        System.out.println("Key exchange completed successfully");

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
                System.out.println("Client RSA keys not found, generating temporary keys...");
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
            System.out.println("RSA authentication completed");

            return true;
        } catch (Exception e) {
            System.err.println("RSA authentication error: " + e.getMessage());
            return false;
        }
    }

    private boolean performUserAuthentication() throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Wait for authentication request
        String authRequest = (String) decryptMessage();
        if (!"AUTH_REQUEST".equals(authRequest)) {
            System.out.println("Unexpected authentication request: " + authRequest);
            return false;
        }

        while (true) {
            System.out.println("\n=== User Authentication ===");
            System.out.println("1. Login with existing account");
            System.out.println("2. Register new account");
            System.out.print("Choose option (1 or 2): ");

            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                if (handleLogin(scanner)) {
                    return true;
                }
            } else if ("2".equals(choice)) {
                if (handleRegistration(scanner)) {
                    return true;
                }
            } else {
                System.out.println("Invalid choice. Please enter 1 or 2.");
                continue;
            }

            // Check server response
            String response = (String) decryptMessage();
            if (response.startsWith("AUTH_RETRY:")) {
                System.out.println("Authentication failed. " + response.substring(11));
                continue;
            } else if (response.startsWith("AUTH_FAILED:")) {
                System.out.println("Authentication failed: " + response.substring(12));
                return false;
            } else if (response.startsWith("AUTH_ERROR:")) {
                System.out.println("Authentication error: " + response.substring(11));
                continue;
            }
        }
    }

    private boolean handleLogin(Scanner scanner) throws Exception {
        sendEncryptedMessage("LOGIN");

        // Wait for login request
        String loginRequest = (String) decryptMessage();
        if (!"LOGIN_REQUEST".equals(loginRequest)) {
            System.out.println("Unexpected login request: " + loginRequest);
            return false;
        }

        System.out.print("Username: ");
        String inputUsername = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = readPassword();

        // Send credentials
        sendEncryptedMessage(inputUsername);
        sendEncryptedMessage(password);

        // Wait for response
        String response = (String) decryptMessage();
        if ("AUTH_SUCCESS".equals(response)) {
            username = inputUsername;
            System.out.println("Login successful!");
            return true;
        } else if (response.startsWith("AUTH_FAILED:")) {
            System.out.println("Login failed: " + response.substring(12));
            return false;
        }

        return false;
    }

    private boolean handleRegistration(Scanner scanner) throws Exception {
        sendEncryptedMessage("REGISTER");

        // Wait for registration request
        String registerRequest = (String) decryptMessage();
        if (!"REGISTER_REQUEST".equals(registerRequest)) {
            System.out.println("Unexpected registration request: " + registerRequest);
            return false;
        }

        System.out.println("\nPassword requirements:");
        System.out.println("- At least 8 characters long");
        System.out.println("- Must contain uppercase and lowercase letters");
        System.out.println("- Must contain at least one digit");
        System.out.println("- Must contain at least one special character");

        System.out.print("Choose username: ");
        String newUsername = scanner.nextLine().trim();

        System.out.print("Choose password: ");
        String newPassword = readPassword();

        System.out.print("Confirm password: ");
        String confirmPassword = readPassword();

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("Passwords do not match!");
            return false;
        }

        // Send credentials
        sendEncryptedMessage(newUsername);
        sendEncryptedMessage(newPassword);

        // Wait for response
        String response = (String) decryptMessage();
        if ("AUTH_SUCCESS".equals(response)) {
            username = newUsername;
            System.out.println("Registration successful!");
            return true;
        } else if (response.startsWith("AUTH_FAILED:")) {
            System.out.println("Registration failed: " + response.substring(12));
            return false;
        }

        return false;
    }

    private String readPassword() {
        // Try to use console for hidden password input
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword();
            return new String(passwordChars);
        } else {
            // Fallback to regular input if console not available
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine();
        }
    }

    private void startChatInterface() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== Chat Interface ===");
        System.out.println("You can start chatting now!");
        System.out.println("Commands:");
        System.out.println("  /msg <username> <message> - Send a private message");
        System.out.println("  /password - Change your password");
        System.out.println("  /info - Show your account information");
        System.out.println("  /quit - Exit the chat");
        System.out.println("========================");

        try {
            String input;
            while (isConnected && (input = scanner.nextLine()) != null) {
                if (input.trim().isEmpty()) {
                    continue;
                }

                if (input.startsWith("/")) {
                    handleClientCommand(input, scanner);
                } else {
                    // Send regular group chat message (not E2EE for simplicity)
                    Message chatMessage = new Message(username, null, input);
                    sendEncryptedMessage(chatMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Chat error: " + e.getMessage());
        }
    }

    private void handleClientCommand(String command, Scanner scanner) throws Exception {
        if (command.toLowerCase().startsWith("/msg ")) {
            // Private message: /msg <username> <message>
            String[] parts = command.split(" ", 3);
            if (parts.length < 3) {
                System.out.println("Usage: /msg <username> <message>");
                return;
            }
            String toUser = parts[1];
            String msg = parts[2];
            PublicKey recipientKey = userPublicKeys.get(toUser);
            if (recipientKey == null) {
                System.out.println("[ERROR] No public key for user: " + toUser);
                return;
            }
            String encryptedMsg = encryptWithPublicKey(msg, recipientKey);
            Message privateMsg = new Message(username, toUser, encryptedMsg);
            sendEncryptedMessage(privateMsg);
            System.out.println("[To " + toUser + "] " + msg);
            return;
        }
        String cmd = command.toLowerCase().trim();

        switch (cmd) {
            case "/quit":
                System.out.println("Goodbye!");
                sendEncryptedMessage("QUIT");
                isConnected = false;
                break;

            case "/password":
                changePassword(scanner);
                break;

            case "/info":
                sendEncryptedMessage("USER_INFO");
                break;

            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Available commands: /password, /info, /quit");
        }
    }

    private void changePassword(Scanner scanner) throws Exception {
        System.out.print("Enter current password: ");
        String oldPassword = readPassword();

        System.out.print("Enter new password: ");
        String newPassword = readPassword();

        System.out.print("Confirm new password: ");
        String confirmPassword = readPassword();

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("New passwords do not match!");
            return;
        }

        String changeCommand = "CHANGE_PASSWORD:" + oldPassword + "|" + newPassword;
        sendEncryptedMessage(changeCommand);
    }

    private void listenForMessages() {
        try {
            while (isConnected) {
                Object receivedData = decryptMessage();

                // Handle public key map update (single or all)
                if (receivedData instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) receivedData;
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String user = (String) entry.getKey();
                        byte[] pubKeyBytes = (byte[]) entry.getValue();
                        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
                        userPublicKeys.put(user, pubKey);
                        System.out.println("[INFO] Received/updated public key for user: " + user);
                    }
                    continue;
                }

                if (receivedData instanceof Message) {
                    Message message = (Message) receivedData;
                    if ("SYSTEM".equals(message.getFrom())) {
                        System.out.println("[SYSTEM] " + message.getContent());
                    } else if (message.getTo() != null && !message.getTo().trim().isEmpty() && message.getTo().equalsIgnoreCase(username)) {
                        // Received a private message: decrypt with our private key
                        String decrypted = decryptWithOwnPrivateKey(message.getContent());
                        System.out.println("[PRIVATE] " + message.getFrom() + ": " + decrypted);
                    } else {
                        System.out.println("[" + message.getTimestamp().toString().substring(11, 19) + "] " + message.getFrom() + ": " + message.getContent());
                    }
                } else if (receivedData instanceof String) {
                    String response = (String) receivedData;
                    handleServerResponse(response);
                }
            }
        } catch (Exception e) {
            if (isConnected) {
                System.err.println("Connection lost: " + e.getMessage());
                isConnected = false;
            }
        }
    }

    private String decryptWithOwnPrivateKey(String encryptedBase64) {
        try {
            PrivateKey privKey = CryptoUtil.loadPrivateKey("client/private_key.der");
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[E2EE DECRYPTION ERROR]";
        }
    }

    private void handleServerResponse(String response) {
        if (response.startsWith("PASSWORD_CHANGED:")) {
            System.out.println("[SYSTEM] " + response.substring(17));
        } else if (response.startsWith("PASSWORD_ERROR:")) {
            System.out.println("[ERROR] " + response.substring(15));
        } else if (response.startsWith("USER_INFO:")) {
            System.out.println("[INFO] " + response.substring(10));
        } else if (response.startsWith("UNKNOWN_COMMAND:")) {
            System.out.println("[ERROR] " + response.substring(16));
        } else {
            System.out.println("[SERVER] " + response);
        }
    }

    private void sendEncryptedMessage(Object message) throws Exception {
        byte[] encrypted = AESUtil.encryptObject((Serializable) message, aesKey);
        out.writeObject(encrypted);
    }

    private Object decryptMessage() throws Exception {
        byte[] encrypted = (byte[]) in.readObject();
        return AESUtil.decryptObject(encrypted, aesKey);
    }

    private void sendOwnPublicKey() throws Exception {
        PublicKey pubKey = CryptoUtil.loadPublicKey("client/public_key.der");
        sendEncryptedMessage(pubKey.getEncoded());
    }

    private void receiveAllPublicKeys() throws Exception {
        Object obj = decryptMessage();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String user = (String) entry.getKey();
                byte[] pubKeyBytes = (byte[]) entry.getValue();
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
                userPublicKeys.put(user, pubKey);
            }
        }
    }

    private void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Disconnected from server");
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    private String encryptWithPublicKey(String message, PublicKey pubKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return "[E2EE ENCRYPTION ERROR]";
        }
    }
}