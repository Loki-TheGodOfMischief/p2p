package server;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import common.LoggerUtil;

public class ChatServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static UserManager userManager;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Secure Chat Server ===");
        System.out.println("Initializing server...");

        // Initialize user manager
        userManager = new UserManager();
        System.out.println("User manager initialized. Registered users: " + userManager.getUserCount());

        // Create admin user if none exists
        if (userManager.getUserCount() == 0) {
            String adminPassword = "Admin@123456"; // Strong default password
            if (userManager.registerUser("admin", adminPassword)) {
                System.out.println("Default admin user created:");
                System.out.println("Username: admin");
                System.out.println("Password: " + adminPassword);
                System.out.println("Please change this password after first login!");
            }
        }

        LoggerUtil.log("Chat server starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for client connections...");

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                LoggerUtil.log("Server shutdown initiated");
                
                // Notify all connected clients
                for (ClientHandler client : clients) {
                    try {
                        if (client.isAuthenticated()) {
                            // Send shutdown notice
                            System.out.println("Notifying client: " + client.getUsername());
                        }
                    } catch (Exception e) {
                        // Ignore errors during shutdown
                    }
                }
                
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
                
                LoggerUtil.log("Server shutdown completed");
                System.out.println("Server shutdown completed");
            }));

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    System.out.println("New client connection from: " + clientAddress);
                    LoggerUtil.log("Client connection accepted from: " + clientAddress);

                    ClientHandler handler = new ClientHandler(socket, clients, userManager);
                    clients.add(handler);
                    
                    // Start client handler in new thread
                    Thread clientThread = new Thread(handler);
                    clientThread.setName("ClientHandler-" + clientAddress);
                    clientThread.start();

                    // Log current statistics
                    System.out.println("Active connections: " + clients.size());

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                        LoggerUtil.log("Error accepting client connection: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            LoggerUtil.log("Server error: " + e.getMessage());
        }
    }

    /**
     * Get current server statistics
     */
    public static ServerStats getServerStats() {
        int totalConnections = clients.size();
        int authenticatedUsers = 0;
        List<String> activeUsers = new ArrayList<>();

        for (ClientHandler client : clients) {
            if (client.isAuthenticated()) {
                authenticatedUsers++;
                activeUsers.add(client.getUsername());
            }
        }

        return new ServerStats(totalConnections, authenticatedUsers, activeUsers, userManager.getUserCount());
    }

    public static class ServerStats {
        private int totalConnections;
        private int authenticatedUsers;
        private List<String> activeUsers;
        private int registeredUsers;

        public ServerStats(int totalConnections, int authenticatedUsers, List<String> activeUsers, int registeredUsers) {
            this.totalConnections = totalConnections;
            this.authenticatedUsers = authenticatedUsers;
            this.activeUsers = new ArrayList<>(activeUsers);
            this.registeredUsers = registeredUsers;
        }

        public int getTotalConnections() { return totalConnections; }
        public int getAuthenticatedUsers() { return authenticatedUsers; }
        public List<String> getActiveUsers() { return activeUsers; }
        public int getRegisteredUsers() { return registeredUsers; }

        @Override
        public String toString() {
            return String.format("Server Stats - Total Connections: %d, Authenticated: %d, Registered Users: %d, Active Users: %s",
                    totalConnections, authenticatedUsers, registeredUsers, activeUsers);
        }
    }
}