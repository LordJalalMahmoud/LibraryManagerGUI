#!/usr/bin/env bash
set -e

# ==============================================================================
# LibraryManager - macOS Native Packaging Script (jpackage)
# Generates Apple Disk Image (.dmg), Installer (.pkg), and Standalone .app bundle
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_NAME="LibraryManager"
APP_VERSION="1.1.0"
APP_DESCRIPTION="Modern Personal Library Management Desktop Application"
APP_VENDOR="LibraryManager"
BUNDLE_ID="com.librarymanager.app"
MAIN_CLASS="com.librarymanager.Launcher"
MAIN_JAR="library-manager-${APP_VERSION}.jar"
ICON_PNG="src/main/resources/icons/app-icon.png"
ICON_ICNS="target/LibraryManager.icns"
DEST_DIR="target/dist"
INPUT_DIR="target/package-input"

MODULES="java.base,java.desktop,java.sql,java.scripting,java.logging,java.management,java.naming,jdk.unsupported,jdk.jfr"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  LibraryManager - macOS Native Desktop Packaging     ${NC}"
echo -e "${BLUE}======================================================${NC}"

if ! command -v jpackage &> /dev/null; then
    echo -e "${RED}Error: jpackage tool not found. Please ensure JDK 21+ is installed.${NC}"
    exit 1
fi

MODE="${1:-all}"

if [ ! -f "target/${MAIN_JAR}" ] || [ "$2" == "--rebuild" ]; then
    echo -e "${YELLOW}Step 1: Building executable Fat JAR...${NC}"
    mvn clean package -DskipTests=true -q
else
    echo -e "${GREEN}✓ Using existing JAR: target/${MAIN_JAR}${NC}"
fi

mkdir -p "$INPUT_DIR" "$DEST_DIR"
cp "target/${MAIN_JAR}" "$INPUT_DIR/"

# Generate .icns icon if iconutil is available on macOS
ICON_ARG=""
if command -v iconutil &> /dev/null && command -v sips &> /dev/null; then
    echo -e "${YELLOW}Step 2: Generating macOS .icns icon bundle...${NC}"
    ICONSET_DIR="target/LibraryManager.iconset"
    rm -rf "$ICONSET_DIR"
    mkdir -p "$ICONSET_DIR"
    sips -z 16 16     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16.png" > /dev/null 2>&1
    sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" > /dev/null 2>&1
    sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32.png" > /dev/null 2>&1
    sips -z 64 64     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" > /dev/null 2>&1
    sips -z 128 128   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128.png" > /dev/null 2>&1
    sips -z 256 256   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" > /dev/null 2>&1
    sips -z 256 256   "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256.png" > /dev/null 2>&1
    sips -z 512 512   "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png" > /dev/null 2>&1
    sips -z 512 512   "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512.png" > /dev/null 2>&1
    sips -z 1024 1024 "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512@2x.png" > /dev/null 2>&1
    iconutil -c icns "$ICONSET_DIR" -o "$ICON_ICNS"
    ICON_ARG="--icon $ICON_ICNS"
    echo -e "${GREEN}✓ Generated ${ICON_ICNS}${NC}"
fi

build_app() {
    echo -e "${YELLOW}Creating Standalone macOS Application (.app)...${NC}"
    rm -rf "${DEST_DIR}/${APP_NAME}.app"
    jpackage \
      --type app-image \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$APP_VENDOR" \
      --description "$APP_DESCRIPTION" \
      $ICON_ARG \
      --input "$INPUT_DIR" \
      --main-jar "$MAIN_JAR" \
      --main-class "$MAIN_CLASS" \
      --dest "$DEST_DIR" \
      --mac-package-name "$APP_NAME" \
      --mac-package-identifier "$BUNDLE_ID" \
      --java-options "-Dfile.encoding=UTF-8" \
      --add-modules "$MODULES"

    echo -e "${YELLOW}Compressing .app into .tar.gz bundle...${NC}"
    tar -czf "${DEST_DIR}/${APP_NAME}-${APP_VERSION}-macos.tar.gz" -C "$DEST_DIR" "${APP_NAME}.app"
    echo -e "${GREEN}✓ Created: ${DEST_DIR}/${APP_NAME}.app and ${APP_NAME}-${APP_VERSION}-macos.tar.gz${NC}"
}

build_dmg() {
    echo -e "${YELLOW}Creating Apple Disk Image (.dmg installer)...${NC}"
    jpackage \
      --type dmg \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$APP_VENDOR" \
      --description "$APP_DESCRIPTION" \
      $ICON_ARG \
      --input "$INPUT_DIR" \
      --main-jar "$MAIN_JAR" \
      --main-class "$MAIN_CLASS" \
      --dest "$DEST_DIR" \
      --mac-package-name "$APP_NAME" \
      --mac-package-identifier "$BUNDLE_ID" \
      --java-options "-Dfile.encoding=UTF-8" \
      --add-modules "$MODULES"

    echo -e "${GREEN}✓ Created DMG installer in ${DEST_DIR}/${NC}"
}

build_pkg() {
    echo -e "${YELLOW}Creating Apple Installer Package (.pkg)...${NC}"
    jpackage \
      --type pkg \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$APP_VENDOR" \
      --description "$APP_DESCRIPTION" \
      $ICON_ARG \
      --input "$INPUT_DIR" \
      --main-jar "$MAIN_JAR" \
      --main-class "$MAIN_CLASS" \
      --dest "$DEST_DIR" \
      --mac-package-name "$APP_NAME" \
      --mac-package-identifier "$BUNDLE_ID" \
      --java-options "-Dfile.encoding=UTF-8" \
      --add-modules "$MODULES"

    echo -e "${GREEN}✓ Created PKG installer in ${DEST_DIR}/${NC}"
}

case "$MODE" in
    app)
        build_app
        ;;
    dmg)
        build_dmg
        ;;
    pkg)
        build_pkg
        ;;
    all)
        build_app
        build_dmg
        build_pkg
        ;;
    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        echo "Usage: ./package-native-macos.sh [app | dmg | pkg | all]"
        exit 1
        ;;
esac

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  macOS Packaging Completed Successfully!             ${NC}"
echo -e "${GREEN}======================================================${NC}"
ls -lh "$DEST_DIR"
