package client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Pair;

public class AuthenticationDialog extends Dialog<AuthenticationDialog.AuthData> {
    
    public static class AuthData {
        public final String username;
        public final String password;
        public final boolean isLogin;
        
        public AuthData(String username, String password, boolean isLogin) {
            this.username = username;
            this.password = password;
            this.isLogin = isLogin;
        }
    }

    public AuthenticationDialog() {
        setTitle("User Authentication");
        setHeaderText("Please login or register to continue");

        // Create the custom dialog
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, cancelButtonType);

        // Create the username and password labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setPrefWidth(200);
        
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setPrefWidth(200);
        
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm Password");
        confirmPassword.setPrefWidth(200);
        confirmPassword.setVisible(false);
        confirmPassword.setManaged(false);

        // Password requirements info
        VBox passwordInfo = new VBox(5);
        passwordInfo.setVisible(false);
        passwordInfo.setManaged(false);
        
        Label requirementsLabel = new Label("Password Requirements:");
        requirementsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Label req1 = new Label("• At least 8 characters long");
        Label req2 = new Label("• Must contain uppercase and lowercase letters");
        Label req3 = new Label("• Must contain at least one digit");
        Label req4 = new Label("• Must contain at least one special character");
        
        req1.setFont(Font.font("Arial", 10));
        req2.setFont(Font.font("Arial", 10));
        req3.setFont(Font.font("Arial", 10));
        req4.setFont(Font.font("Arial", 10));
        
        passwordInfo.getChildren().addAll(requirementsLabel, req1, req2, req3, req4);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPassword, 1, 2);
        grid.add(passwordInfo, 0, 3, 2, 1);

        // Enable/Disable login button depending on whether a username was entered
        Node loginButton = getDialogPane().lookupButton(loginButtonType);
        Node registerButton = getDialogPane().lookupButton(registerButtonType);
        
        loginButton.setDisable(true);
        registerButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax)
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || password.getText().trim().isEmpty());
            updateRegisterButton(registerButton, username, password, confirmPassword);
        });
        
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || username.getText().trim().isEmpty());
            updateRegisterButton(registerButton, username, password, confirmPassword);
        });
        
        confirmPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            updateRegisterButton(registerButton, username, password, confirmPassword);
        });

        getDialogPane().setContent(grid);

        // Request focus on the username field by default
        Platform.runLater(() -> username.requestFocus());

        // Handle register button click to show additional fields
        registerButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!confirmPassword.isVisible()) {
                confirmPassword.setVisible(true);
                confirmPassword.setManaged(true);
                passwordInfo.setVisible(true);
                passwordInfo.setManaged(true);
                
                // Resize dialog
                getDialogPane().getScene().getWindow().sizeToScene();
                
                event.consume(); // Don't close the dialog yet
                return;
            }
            // If confirm password is visible, proceed with registration
        });

        // Convert the result to a username-password-pair when the login button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new AuthData(username.getText(), password.getText(), true);
            } else if (dialogButton == registerButtonType) {
                if (confirmPassword.isVisible()) {
                    // Validate passwords match
                    if (!password.getText().equals(confirmPassword.getText())) {
                        showAlert("Error", "Passwords do not match!");
                        return null;
                    }
                    return new AuthData(username.getText(), password.getText(), false);
                }
            }
            return null;
        });
    }
    
    private void updateRegisterButton(Node registerButton, TextField username, 
                                    PasswordField password, PasswordField confirmPassword) {
        if (confirmPassword.isVisible()) {
            // Registration mode
            boolean valid = !username.getText().trim().isEmpty() && 
                           !password.getText().trim().isEmpty() &&
                           !confirmPassword.getText().trim().isEmpty() &&
                           password.getText().equals(confirmPassword.getText());
            registerButton.setDisable(!valid);
        } else {
            // Just showing register button
            registerButton.setDisable(username.getText().trim().isEmpty() || 
                                    password.getText().trim().isEmpty());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}