#!/bin/bash
# Double-click launcher for the Legacy Banking application (macOS; also works on Linux terminals).
# Requires Java 8 or newer. Builds the jar first if it does not exist (needs Maven).
cd "$(dirname "$0")" || exit 1

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found. Install JDK or JRE 8 or newer."
    read -n 1 -s -r -p "Press any key to close..."
    exit 1
fi

if [ ! -f target/legacy-banking-app.jar ]; then
    echo "Building the application, please wait..."
    if ! mvn -q clean package -DskipTests; then
        echo "Build failed. Make sure Maven is installed."
        read -n 1 -s -r -p "Press any key to close..."
        exit 1
    fi
fi

# legacybank.db is created in this folder on first start.
nohup java -jar target/legacy-banking-app.jar > legacybank.log 2>&1 &
sleep 1
# close the Terminal window that Finder opened (the app keeps running)
osascript -e 'tell application "Terminal" to close (every window whose name contains "run.command")' >/dev/null 2>&1 &
exit 0
