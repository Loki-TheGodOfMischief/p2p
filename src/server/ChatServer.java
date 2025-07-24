package server;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected: " + socket.getInetAddress());
            ClientHandler handler = new ClientHandler(socket, clients);
            clients.add(handler);
            new Thread(handler).start();
        }
    }
}