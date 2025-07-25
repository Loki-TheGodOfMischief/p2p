#!/bin/bash

echo "=== Secure Chat Application Setup (GUI Version) ==="

# Create necessary directories
echo "Creating directories..."
mkdir -p out
mkdir -p keys
mkdir -p client
mkdir -p resources

# Create resources directory and copy styles
echo "Setting up resources..."
if [ ! -f "resources/styles.css" ]; then
    mkdir -p resources
    cat > resources/styles.css << 'EOF'
/* Main Application Styling */
.root {
    -fx-background-color: #f5f5f5;
    -fx-font-family: "Arial", sans-serif;
}

/* Connection Panel */
.connection-panel {
    -fx-background-color: #ffffff;
    -fx-border-color: #cccccc;
    -fx-border-width: 1px;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
    -fx-padding: 10px;
}

/* Status Panel */
.status-panel {
    -fx-background-color: #e8e8e8;
    -fx-padding: 5px 10px;
    -fx-border-radius: 3px;
    -fx-background-radius: 3px;
}

.status-connected {
    -fx-text-fill: #28a745;
    -fx-font-weight: bold;
}

.status-disconnected {
    -fx-text-fill: #dc3545;
    -fx-font-weight: bold;
}

.username-label {
    -fx-text-fill: #007bff;
    -fx-font-weight: bold;
}

/* Chat Area */
.chat-area {
    -fx-background-color: #ffffff;
    -fx-border-color: #cccccc;
    -fx-border-width: 1px;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
    -fx-padding: 10px;
    -fx-font-family: "Consolas", "Monaco", monospace;
}

/* Buttons */
.button {
    -fx-background-radius: 5px;
    -fx-border-radius: 5px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
}

.connect-button {
    -fx-background-color: #28a745;
    -fx-text-fill: white;
    -fx-border-color: #1e7e34;
    -fx-border-width: 1px;
}

.connect-button:hover {
    -fx-background-color: #218838;
}

.disconnect-button {
    -fx-background-color: #dc3545;
    -fx-text-fill: white;
    -fx-border-color: #bd2130;
    -fx-border-width: 1px;
}

.disconnect-button:hover {
    -fx-background-color: #c82333;
}

.send-button {
    -fx-background-color: #007bff;
    -fx-text-fill: white;
    -fx-border-color: #0056b3;
    -fx-border-width: 1px;
    -fx-min-width: 80px;
}

.send-button:hover {
    -fx-background-color: #0056b3;
}

.clear-button {
    -fx-background-color: #6c757d;
    -fx-text-fill: white;
    -fx-border-color: #545b62;
    -fx-border-width: 1px;
}

.clear-button:hover {
    -fx-background-color: #545b62;
}

.command-button {
    -fx-background-color: #17a2b8;
    -fx-text-fill: white;
    -fx-border-color: #117a8b;
    -fx-border-width: 1px;
}

.command-button:hover {
    -fx-background-color: #117a8b;
}

.button:disabled {
    -fx-background-color: #e9ecef;
    -fx-text-fill: #6c757d;
    -fx-border-color: #dee2e6;
    -fx-cursor: default;
}

/* Text Fields */
.text-field, .password-field {
    -fx-background-color: white;
    -fx-border-color: #ced4da;
    -fx-border-width: 1px;
    -fx-border-radius: 3px;
    -fx-background-radius: 3px;
    -fx-padding: 5px 10px;
}

.text-field:focused, .password-field:focused {
    -fx-border-color: #007bff;
    -fx-border-width: 2px;
}

.text-field:disabled, .password-field:disabled {
    -fx-background-color: #e9ecef;
    -fx-text-fill: #6c757d;
}
EOF
fi

# Compile the project
echo "Compiling Java files..."
if [ -n "$JAVAFX_PATH" ]; then
    echo "Using JavaFX from: $JAVAFX_PATH"
    find src -name "*.java" -print0 | xargs -0 javac --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml -d out
else
    echo "Warning: JAVAFX_PATH not set, compiling without JavaFX modules"
    find src -name "*.java" -print0 | xargs -0 javac -d out
fi

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Copy resources
echo "Copying resources..."
cp -r resources/* out/ 2>/dev/null || true

# Generate RSA key pairs
echo "Generating RSA key pairs..."
java -cp out common.KeyGeneratorUtil

if [ $? -eq 0 ]; then
    echo "✓ Key generation successful"
else
    echo "✗ Key generation failed"
    exit 1
fi

# Make scripts executable
chmod +x *.sh 2>/dev/null || true

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Files created:"
echo "  - out/                 (Compiled Java classes)"
echo "  - keys/                (Server RSA keys)"
echo "  - client/              (Client RSA keys)"
echo "  - resources/           (GUI resources)"
echo "  - users.db             (Will be created on first run)"
echo "  - auth_log.txt         (Will be created on first run)"
echo ""
echo "Available commands:"
echo "  Start server:          make -f Makefile.server"
echo "  Start terminal client: make -f Makefile.client"
echo "  Start GUI client:      ./run_client_gui.sh"
echo "  User administration:   java -cp out server.UserAdminTool"
echo ""
echo "Manual execution:"
echo "  Server:        java -cp out server.ChatServer"
echo "  Terminal Client: java -cp out client.ClientMain"
echo "  GUI Client:    java -cp out client.ChatClientGUI"
echo "  Admin Tool:    java -cp out server.UserAdminTool"
echo ""
echo "JavaFX Setup:"
if [ -z "$JAVAFX_PATH" ]; then
    echo "  ⚠  JAVAFX_PATH not set!"
    echo "  Download JavaFX SDK from: https://openjfx.io/"
    echo "  Then set: export JAVAFX_PATH=/path/to/javafx/lib"
else
    echo "  ✓ JavaFX configured: $JAVAFX_PATH"
fi
echo ""
echo "Security Features:"
echo "  ✓ RSA-2048 key exchange and authentication"
echo "  ✓ AES-256 message encryption"
echo "  ✓ Salted password hashing (SHA-256 + PBKDF2-like)"
echo "  ✓ Strong password requirements"
echo "  ✓ User registration and login system"
echo "  ✓ Authentication logging"
echo "  ✓ Multi-client support"
echo "  ✓ GUI and Terminal interfaces"
echo ""
echo "Default admin user will be created on first server start:"
echo "  Username: admin"
echo "  Password: Admin@123456"
echo "  (Please change this password immediately!)"