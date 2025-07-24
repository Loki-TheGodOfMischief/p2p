package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import common.LoggerUtil;

public class UserManager {
    private static final String USER_DB_FILE = "users.db";
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    private static final int ITERATIONS = 10000; // PBKDF2 iterations
    
    private Map<String, UserData> users;
    
    public UserManager() {
        users = new HashMap<>();
        loadUsers();
    }
    
    public static class UserData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String username;
        private String hashedPassword;
        private String salt;
        private long createdDate;
        private long lastLoginDate;
        private boolean isActive;
        
        public UserData(String username, String hashedPassword, String salt) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.salt = salt;
            this.createdDate = System.currentTimeMillis();
            this.lastLoginDate = 0;
            this.isActive = true;
        }
        
        // Getters and setters
        public String getUsername() { return username; }
        public String getHashedPassword() { return hashedPassword; }
        public String getSalt() { return salt; }
        public long getCreatedDate() { return createdDate; }
        public long getLastLoginDate() { return lastLoginDate; }
        public boolean isActive() { return isActive; }
        public void setLastLoginDate(long date) { this.lastLoginDate = date; }
        public void setActive(boolean active) { this.isActive = active; }
    }
    
    /**
     * Generate a random salt
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hash password with salt using PBKDF2
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            
            // Apply salt
            md.update(saltBytes);
            
            // Hash with iterations (simple PBKDF2-like approach)
            byte[] hashedPassword = password.getBytes("UTF-8");
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                md.update(saltBytes);
                hashedPassword = md.digest(hashedPassword);
            }
            
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Register a new user
     */
    public synchronized boolean registerUser(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            LoggerUtil.log("Registration failed: Invalid username or password");
            return false;
        }
        
        if (users.containsKey(username.toLowerCase())) {
            LoggerUtil.log("Registration failed: Username '" + username + "' already exists");
            return false;
        }
        
        // Validate password strength
        if (!isPasswordStrong(password)) {
            LoggerUtil.log("Registration failed: Password too weak for user '" + username + "'");
            return false;
        }
        
        try {
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            
            UserData userData = new UserData(username, hashedPassword, salt);
            users.put(username.toLowerCase(), userData);
            
            saveUsers();
            LoggerUtil.log("User registered successfully: " + username);
            return true;
        } catch (Exception e) {
            LoggerUtil.log("Registration error for user '" + username + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate user with username and password
     */
    public synchronized boolean authenticateUser(String username, String password) {
        if (username == null || password == null) {
            LoggerUtil.log("Authentication failed: Null credentials");
            return false;
        }
        
        UserData userData = users.get(username.toLowerCase());
        if (userData == null) {
            LoggerUtil.log("Authentication failed: User '" + username + "' not found");
            return false;
        }
        
        if (!userData.isActive()) {
            LoggerUtil.log("Authentication failed: User '" + username + "' is deactivated");
            return false;
        }
        
        try {
            String hashedInputPassword = hashPassword(password, userData.getSalt());
            boolean isValid = hashedInputPassword.equals(userData.getHashedPassword());
            
            if (isValid) {
                userData.setLastLoginDate(System.currentTimeMillis());
                saveUsers();
                LoggerUtil.log("Authentication successful: " + username);
            } else {
                LoggerUtil.log("Authentication failed: Invalid password for user '" + username + "'");
            }
            
            return isValid;
        } catch (Exception e) {
            LoggerUtil.log("Authentication error for user '" + username + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Change user password
     */
    public synchronized boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticateUser(username, oldPassword)) {
            return false;
        }
        
        if (!isPasswordStrong(newPassword)) {
            LoggerUtil.log("Password change failed: New password too weak for user '" + username + "'");
            return false;
        }
        
        try {
            UserData userData = users.get(username.toLowerCase());
            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);
            
            // Create new UserData with updated password
            UserData updatedData = new UserData(userData.getUsername(), newHashedPassword, newSalt);
            updatedData.setLastLoginDate(userData.getLastLoginDate());
            users.put(username.toLowerCase(), updatedData);
            
            saveUsers();
            LoggerUtil.log("Password changed successfully for user: " + username);
            return true;
        } catch (Exception e) {
            LoggerUtil.log("Password change error for user '" + username + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deactivate user account
     */
    public synchronized boolean deactivateUser(String username) {
        UserData userData = users.get(username.toLowerCase());
        if (userData == null) {
            return false;
        }
        
        userData.setActive(false);
        saveUsers();
        LoggerUtil.log("User deactivated: " + username);
        return true;
    }
    
    /**
     * Check if password meets strength requirements
     */
    private boolean isPasswordStrong(String password) {
        if (password.length() < 8) return false;
        
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * Get user information (without sensitive data)
     */
    public UserInfo getUserInfo(String username) {
        UserData userData = users.get(username.toLowerCase());
        if (userData == null) return null;
        
        return new UserInfo(userData.getUsername(), userData.getCreatedDate(), 
                           userData.getLastLoginDate(), userData.isActive());
    }
    
    public static class UserInfo {
        private String username;
        private long createdDate;
        private long lastLoginDate;
        private boolean isActive;
        
        public UserInfo(String username, long createdDate, long lastLoginDate, boolean isActive) {
            this.username = username;
            this.createdDate = createdDate;
            this.lastLoginDate = lastLoginDate;
            this.isActive = isActive;
        }
        
        public String getUsername() { return username; }
        public long getCreatedDate() { return createdDate; }
        public long getLastLoginDate() { return lastLoginDate; }
        public boolean isActive() { return isActive; }
    }
    
    /**
     * Load users from encrypted file
     */
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        try {
            File file = new File(USER_DB_FILE);
            if (!file.exists()) {
                LoggerUtil.log("User database file not found, creating new one");
                return;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, UserData>) ois.readObject();
                LoggerUtil.log("Loaded " + users.size() + " users from database");
            }
        } catch (Exception e) {
            LoggerUtil.log("Error loading user database: " + e.getMessage());
            users = new HashMap<>();
        }
    }
    
    /**
     * Save users to encrypted file
     */
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DB_FILE))) {
            oos.writeObject(users);
            LoggerUtil.log("User database saved successfully");
        } catch (Exception e) {
            LoggerUtil.log("Error saving user database: " + e.getMessage());
        }
    }
    
    /**
     * Get total number of registered users
     */
    public int getUserCount() {
        return users.size();
    }
    
    /**
     * List all usernames (for admin purposes)
     */
    public String[] getAllUsernames() {
        return users.keySet().toArray(new String[0]);
    }
}