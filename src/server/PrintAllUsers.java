package server;

import java.io.*;
import java.util.*;

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
                System.out.println("Salt: " + user.getSalt());
                System.out.println("Hashed Password: " + user.getHashedPassword());
                System.out.println("-----");
            }
        } catch (Exception e) {
            System.out.println("Error reading users.db: " + e.getMessage());
        }
    }
}