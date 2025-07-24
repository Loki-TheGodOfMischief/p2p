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

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Key exchange and authentication
            KeyExchangeProtocol serverKeyExchange = new KeyExchangeProtocol();
            serverKeyExchange.performServerHandshake(in, out);
            aesKey = serverKeyExchange.getAESKey();

            // Authentication
            if (!ServerAuth.authenticate(in, out)) {
                System.out.println("Authentication failed.");
                socket.close();
                return;
            }
            username = (String) AESUtil.decrypt((byte[]) in.readObject(), aesKey);
            LoggerUtil.log("User authenticated: " + username);

            // Chat loop
            while (true) {
                byte[] encrypted = (byte[]) in.readObject();
                Message msg = (Message) AESUtil.decrypt(encrypted, aesKey);
                broadcast(msg);
            }

        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            clients.remove(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void broadcast(Message msg) throws Exception {
        for (ClientHandler client : clients) {
            if (client != this) {
                client.sendMessage(msg);
            }
        }
    }

    private void sendMessage(Message msg) throws Exception {
        byte[] encrypted = AESUtil.encrypt(msg, aesKey);
        out.writeObject(encrypted);
    }
}  