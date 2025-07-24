package client;

public class ClientMain {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 12345;

        ClientConnection client = new ClientConnection(serverAddress, serverPort);
        client.connect();
    }
}
