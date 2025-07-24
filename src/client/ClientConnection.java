package client;

import java.io.*;
import java.net.Socket;
import java.security.*;
import javax.crypto.SecretKey;

public class ClientConnection {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SecretKey aesKey;

    public ClientConnection(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server.");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // --- 1. Receive Challenge from Server
            String challenge = in.readLine();
            System.out.println("Received Challenge: " + challenge);

            // --- 2. Sign Challenge with Client Private Key
            byte[] signature = AuthenticationUtil.signChallenge(challenge);

            // --- 3. Send Signature to Server
            out.println(AuthenticationUtil.encodeBase64(signature));

            // --- 4. Wait for authentication result
            String authResponse = in.readLine();
            if ("AUTH_SUCCESS".equals(authResponse)) {
                System.out.println("Authenticated successfully.");

                // --- 5. Receive AES Key (encrypted with client public key)
                String encryptedAESKeyBase64 = in.readLine();
                aesKey = CryptoUtil.decryptAESKey(encryptedAESKeyBase64);

                // --- 6. Start Chat
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                String message;

                while ((message = userInput.readLine()) != null) {
                    String encryptedMsg = CryptoUtil.encryptMessage(message, aesKey);
                    out.println(encryptedMsg);
                }
            } else {
                System.out.println("Authentication failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
