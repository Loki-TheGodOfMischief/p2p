#!/bin/bash

echo "=== Secure Chat Application Setup ==="

# Create necessary directories
echo "Creating directories..."
mkdir -p out
mkdir -p keys
mkdir -p client

# Compile the project
echo "Compiling Java files..."
find src -name "*.java" -print0 | xargs -0 javac -d out

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

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
echo "  - users.db             (Will be created on first run)"
echo "  - auth_log.txt         (Will be created on first run)"
echo ""
echo "Available commands:"
echo "  Start server:          make -f Makefile.server"
echo "  Start client:          make -f Makefile.client"
echo "  User administration:   java -cp out server.UserAdminTool"
echo ""
echo "Manual execution:"
echo "  Server:     java -cp out server.ChatServer"
echo "  Client:     java -cp out client.ClientMain"
echo "  Admin Tool: java -cp out server.UserAdminTool"
echo ""
echo "Security Features:"
echo "  ✓ RSA-2048 key exchange and authentication"
echo "  ✓ AES-256 message encryption"
echo "  ✓ Salted password hashing (SHA-256 + PBKDF2-like)"
echo "  ✓ Strong password requirements"
echo "  ✓ User registration and login system"
echo "  ✓ Authentication logging"
echo "  ✓ Multi-client support"
echo ""
echo "Default admin user will be created on first server start:"
echo "  Username: admin"
echo "  Password: Admin@123456"
echo "  (Please change this password immediately!)"