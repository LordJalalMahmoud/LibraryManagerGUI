@echo off
REM ==============================================================================
REM LibraryManager - Windows Native Packaging Script (jpackage)
REM Generates native Windows Installer (.msi) or Standalone Image (.zip)
REM ==============================================================================

setlocal enabledelayedexpansion

set APP_NAME=LibraryManager
set APP_VERSION=1.1.0
set APP_DESCRIPTION=Modern Personal Library Management Desktop Application
set APP_VENDOR=LibraryManager
set MAIN_CLASS=com.librarymanager.Launcher
set MAIN_JAR=library-manager-%APP_VERSION%.jar
set ICON_PATH=src\main\resources\icons\app-icon.ico
set DEST_DIR=target\dist
set INPUT_DIR=target\package-input

set MODULES=java.base,java.desktop,java.sql,java.scripting,java.logging,java.management,java.naming,jdk.unsupported,jdk.jfr

echo ======================================================
echo   LibraryManager - Windows Native Desktop Packaging
echo ======================================================

where jpackage >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] jpackage was not found. Please ensure JDK 21+ is installed and configured in PATH.
    exit /b 1
)

echo [1/3] Building executable Fat JAR...
call mvn clean package -DskipTests=true -q
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed.
    exit /b 1
)

echo [2/3] Preparing staging directories...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
if not exist "%DEST_DIR%" mkdir "%DEST_DIR%"
copy "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul

echo [3/3] Building Native Windows Installer (.msi)...
jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%APP_VENDOR%" ^
  --description "%APP_DESCRIPTION%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest "%DEST_DIR%" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --add-modules "%MODULES%"

if %errorlevel% equ 0 (
    echo ======================================================
    echo   Windows Native Packaging Completed Successfully!
    echo   Output located in %DEST_DIR%\
    echo ======================================================
) else (
    echo [ERROR] jpackage failed to generate Windows installer.
)

endlocal
