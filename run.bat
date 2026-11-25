@echo off
REM Double-click launcher for the Legacy Banking application (Windows).
REM Requires Java 8 or newer on the PATH. Builds the jar first if it does not exist (needs Maven).
cd /d "%~dp0"

where javaw >nul 2>&1
if errorlevel 1 (
    echo Java was not found. Install JDK or JRE 8 or newer and make sure it is on the PATH.
    pause
    exit /b 1
)

if not exist "target\legacy-banking-app.jar" (
    echo Building the application, please wait...
    call mvn -q clean package -DskipTests
    if errorlevel 1 (
        echo Build failed. Make sure Maven is installed and on the PATH.
        pause
        exit /b 1
    )
)

REM javaw starts the Swing app without a console window; legacybank.db is created in this folder.
start "" javaw -jar "target\legacy-banking-app.jar"
