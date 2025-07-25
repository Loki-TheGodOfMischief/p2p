package server;

import java.io.*;
import java.util.*;
import java.util.Base64;

public class PrintAllUsers {
    public static void main(String[] args) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("users.db"))) {
            Map<String, UserManager.UserData> users = (Map<String, UserManager.UserData>) ois.readObject();
            System.out.println("Total users: " + users.size());
            for (Map.Entry<String, UserManager.UserData> entry : users.entrySet()) {
                UserManager.UserData user = entry.getValue();
                System.out.println("Username: " + user.getUsername());
                System.out.println("Active: " + user.isActive());
                System.out.println("Created: " + user.getCreatedDate());
                System.out.println("Last Login: " + user.getLastLoginDate());

                String saltBase64 = user.getSalt();
                System.out.println("Salt (Base64): " + saltBase64);

                // Decode Base64 to byte array
                byte[] decodedSalt = Base64.getDecoder().decode(saltBase64);

                // Convert bytes to hex string
                StringBuilder hex = new StringBuilder();
                for (byte b : decodedSalt) {
                    hex.append(String.format("%02x ", b));
                }

                System.out.println("Salt (Decoded Bytes): " + hex.toString().trim());

                System.out.println("Hashed Password: " + user.getHashedPassword());
                System.out.println("-----");
            }
        } catch (Exception e) {
            System.out.println("Error reading users.db: " + e.getMessage());
        }
    }
}
