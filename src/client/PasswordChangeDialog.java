package client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PasswordChangeDialog extends Dialog<PasswordChangeDialog.PasswordData> {
    
    public static class PasswordData {
        public final String oldPassword;
        public final String newPassword;
        
        public PasswordData(String oldPassword, String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    public PasswordChangeDialog() {
        setTitle("Change Password");
        setHeaderText("Enter your current password and new password");

        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        getDialogPane().getButtonTypes().addAll(changeButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("Current Password");
        oldPassword.setPrefWidth(200);
        
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New Password");
        newPassword.setPrefWidth(200);
        
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm New Password");
        confirmPassword.setPrefWidth(200);

        // Password requirements info
        VBox passwordInfo = new VBox(5);
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

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(oldPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);
        grid.add(new Label("Confirm New Password:"), 0, 2);
        grid.add(confirmPassword, 1, 2);
        grid.add(passwordInfo, 0, 3, 2, 1);

        Node changeButton = getDialogPane().lookupButton(changeButtonType);
        changeButton.setDisable(true);

        // Validation
        Runnable validation = () -> {
            boolean valid = !oldPassword.getText().trim().isEmpty() && 
                           !newPassword.getText().trim().isEmpty() &&
                           !confirmPassword.getText().trim().isEmpty() &&
                           newPassword.getText().equals(confirmPassword.getText());
            changeButton.setDisable(!valid);
        };

        oldPassword.textProperty().addListener((observable, oldValue, newValue) -> validation.run());
        newPassword.textProperty().addListener((observable, oldValue, newValue) -> validation.run());
        confirmPassword.textProperty().addListener((observable, oldValue, newValue) -> validation.run());

        getDialogPane().setContent(grid);

        Platform.runLater(() -> oldPassword.requestFocus());

        setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                if (!newPassword.getText().equals(confirmPassword.getText())) {
                    showAlert("Error", "New passwords do not match!");
                    return null;
                }
                return new PasswordData(oldPassword.getText(), newPassword.getText());
            }
            return null;
        });
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}