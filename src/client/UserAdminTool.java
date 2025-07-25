package client;

import java.util.Scanner;
import common.LoggerUtil;

/**
 * Command-line tool for user administration
 */
public class UserAdminTool {
    private UserManager userManager;
    private Scanner scanner;

    public UserAdminTool() {
        userManager = new UserManager();
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        UserAdminTool tool = new UserAdminTool();
        tool.run();
    }

    public void run() {
        System.out.println("=== User Administration Tool ===");
        System.out.println("Total registered users: " + userManager.getUserCount());

        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    listUsers();
                    break;
                case "2":
                    createUser();
                    break;
                case "3":
                    resetPassword();
                    break;
                case "4":
                    deactivateUser();
                    break;
                case "5":
                    showUserDetails();
                    break;
                case "6":
                    showSystemStats();
                    break;
                case "7":
                    System.out.println("Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void showMenu() {
        System.out.println("\n=== Admin Menu ===");
        System.out.println("1. List all users");
        System.out.println("2. Create new user");
        System.out.println("3. Reset user password");
        System.out.println("4. Deactivate user");
        System.out.println("5. Show user details");
        System.out.println("6. Show system statistics");
        System.out.println("7. Exit");
        System.out.print("Choose option: ");
    }

    private void listUsers() {
        String[] usernames = userManager.getAllUsernames();
        if (usernames.length == 0) {
            System.out.println("No users found.");
            return;
        }

        System.out.println("\n=== All Users ===");
        System.out.printf("%-20s %-10s %-20s %-20s%n", "Username", "Status", "Created", "Last Login");
        System.out.println("-".repeat(70));

        for (String username : usernames) {
            UserManager.UserInfo info = userManager.getUserInfo(username);
            if (info != null) {
                String status = info.isActive() ? "Active" : "Inactive";
                String created = new java.util.Date(info.getCreatedDate()).toString().substring(0, 19);
                String lastLogin = info.getLastLoginDate() > 0
                        ? new java.util.Date(info.getLastLoginDate()).toString().substring(0, 19)
                        : "Never";

                System.out.printf("%-20s %-10s %-20s %-20s%n",
                        info.getUsername(), status, created, lastLogin);
            }
        }
    }

    private void createUser() {
        System.out.println("\n=== Create New User ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        System.out.println("Password requirements:");
        System.out.println("- At least 8 characters long");
        System.out.println("- Must contain uppercase and lowercase letters");
        System.out.println("- Must contain at least one digit");
        System.out.println("- Must contain at least one special character");

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine();

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match!");
            return;
        }

        if (userManager.registerUser(username, password)) {
            System.out.println("User '" + username + "' created successfully!");
        } else {
            System.out.println("Failed to create user. Username may already exist or password is too weak.");
        }
    }

    private void resetPassword() {
        System.out.println("\n=== Reset User Password ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        UserManager.UserInfo info = userManager.getUserInfo(username);
        if (info == null) {
            System.out.println("User not found: " + username);
            return;
        }

        System.out.println("Resetting password for user: " + info.getUsername());
        System.out.print("New password: ");
        String newPassword = scanner.nextLine();

        System.out.print("Confirm new password: ");
        String confirmPassword = scanner.nextLine();

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("Passwords do not match!");
            return;
        }

        // For admin reset, we need to directly modify the user data
        // This is a simplified approach - in production, you might want additional
        // verification
        System.out.println("Warning: This will reset the password without verifying the old password.");
        System.out.print("Continue? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if ("yes".equals(confirm)) {
            // Create a temporary admin user to perform the reset
            if (forcePasswordReset(username, newPassword)) {
                System.out.println("Password reset successfully for user: " + username);
                LoggerUtil.log("Admin password reset for user: " + username);
            } else {
                System.out.println("Failed to reset password. Password may be too weak.");
            }
        } else {
            System.out.println("Password reset cancelled.");
        }
    }

    private boolean forcePasswordReset(String username, String newPassword) {
        // This is a simple implementation - create a new user with same username
        // In a real system, you'd modify the existing user data
        UserManager.UserInfo existingUser = userManager.getUserInfo(username);
        if (existingUser == null) {
            return false;
        }

        // Deactivate old user and create new one with same username
        userManager.deactivateUser(username);
        return userManager.registerUser(username, newPassword);
    }

    private void deactivateUser() {
        System.out.println("\n=== Deactivate User ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        UserManager.UserInfo info = userManager.getUserInfo(username);
        if (info == null) {
            System.out.println("User not found: " + username);
            return;
        }

        if (!info.isActive()) {
            System.out.println("User is already deactivated: " + username);
            return;
        }

        System.out.println("User details:");
        showUserInfo(info);

        System.out.print("Are you sure you want to deactivate this user? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if ("yes".equals(confirm)) {
            if (userManager.deactivateUser(username)) {
                System.out.println("User deactivated successfully: " + username);
            } else {
                System.out.println("Failed to deactivate user: " + username);
            }
        } else {
            System.out.println("Deactivation cancelled.");
        }
    }

    private void showUserDetails() {
        System.out.println("\n=== User Details ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        UserManager.UserInfo info = userManager.getUserInfo(username);
        if (info == null) {
            System.out.println("User not found: " + username);
            return;
        }

        showUserInfo(info);
    }

    private void showUserInfo(UserManager.UserInfo info) {
        System.out.println("\n--- User Information ---");
        System.out.println("Username: " + info.getUsername());
        System.out.println("Status: " + (info.isActive() ? "Active" : "Inactive"));
        System.out.println("Created: " + new java.util.Date(info.getCreatedDate()));
        System.out.println(
                "Last Login: " + (info.getLastLoginDate() > 0 ? new java.util.Date(info.getLastLoginDate()) : "Never"));
    }

    private void showSystemStats() {
        System.out.println("\n=== System Statistics ===");
        System.out.println("Total registered users: " + userManager.getUserCount());

        String[] usernames = userManager.getAllUsernames();
        int activeUsers = 0;
        int inactiveUsers = 0;

        for (String username : usernames) {
            UserManager.UserInfo info = userManager.getUserInfo(username);
            if (info != null) {
                if (info.isActive()) {
                    activeUsers++;
                } else {
                    inactiveUsers++;
                }
            }
        }

        System.out.println("Active users: " + activeUsers);
        System.out.println("Inactive users: " + inactiveUsers);
        System.out.println("Database file: users.db");
        System.out.println("Log file: auth_log.txt");

        // Show recent log entries (last 10)
        System.out.println("\nRecent activity (check auth_log.txt for full log):");
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get("auth_log.txt");
            if (java.nio.file.Files.exists(logPath)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(logPath);
                int startIndex = Math.max(0, lines.size() - 10);
                for (int i = startIndex; i < lines.size(); i++) {
                    System.out.println("  " + lines.get(i));
                }
            } else {
                System.out.println("  No log file found.");
            }
        } catch (Exception e) {
            System.out.println("  Error reading log file: " + e.getMessage());
        }
    }
}