#!/bin/bash

echo "=== Starting Secure Chat GUI Client ==="

# Check for JavaFX
if [ -z "$JAVAFX_PATH" ]; then
    echo "Warning: JAVAFX_PATH environment variable not set."
    echo "Please download JavaFX SDK and set JAVAFX_PATH to the lib directory."
    echo "Example: export JAVAFX_PATH=/home/dulshan/Downloads/openjfx-24.0.2_linux-x64_bin-sdk/javafx-sdk-24.0.2/lib"
    echo ""
    echo "Attempting to run without module path (may work with OpenJFX installed)..."
    java -cp out client.ChatClientGUI
else
    echo "Using JavaFX from: $JAVAFX_PATH"
    java --module-path "/home/dulshan/Downloads/openjfx-24.0.2_linux-x64_bin-sdk/javafx-sdk-24.0.2/lib" --add-modules javafx.controls,javafx.fxml -cp out client.ChatClientGUI
fi