#!/usr/bin/env bash
set -e

# ==============================================================================
# LibraryManager - Native Packaging Script (jpackage)
# Generates self-contained native desktop bundles with bundled custom JRE.
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_NAME="LibraryManager"
DEB_NAME="library-manager"
APP_VERSION="${APP_VERSION:-$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')}"
APP_DESCRIPTION="Modern Personal Library Management Desktop Application"
APP_VENDOR="LibraryManager"
MAIN_CLASS="com.librarymanager.Launcher"
MAIN_JAR="library-manager-${APP_VERSION}.jar"
ICON_PATH="src/main/resources/icons/app-icon.png"
DEST_DIR="target/dist"
INPUT_DIR="target/package-input"

MODULES="java.base,java.desktop,java.sql,java.scripting,java.logging,java.management,java.naming,jdk.unsupported,jdk.jfr"

# Colors for terminal output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  LibraryManager v${APP_VERSION} - Native Desktop Packaging   ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Check prerequisites
if ! command -v jpackage &> /dev/null; then
    echo -e "${RED}Error: jpackage tool not found. Please ensure JDK 21+ is installed and on PATH.${NC}"
    exit 1
fi

MODE="${1:-all}"

if [ ! -f "target/${MAIN_JAR}" ] || [ "$2" == "--rebuild" ]; then
    echo -e "${YELLOW}Step 1: Building executable Fat JAR...${NC}"
    mvn clean package -DskipTests=true -q
fi

# Detect actual generated JAR
if [ ! -f "target/${MAIN_JAR}" ]; then
    FOUND_JAR=$(ls target/library-manager-*.jar 2>/dev/null | grep -v 'original' | head -n 1)
    if [ -n "$FOUND_JAR" ] && [ -f "$FOUND_JAR" ]; then
        MAIN_JAR=$(basename "$FOUND_JAR")
        echo -e "${GREEN}✓ Auto-detected JAR: target/${MAIN_JAR}${NC}"
    else
        echo -e "${RED}Error: target/${MAIN_JAR} was not found after build.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Using existing JAR: target/${MAIN_JAR}${NC}"
fi

echo -e "${YELLOW}Step 2: Preparing staging directories...${NC}"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
mkdir -p "$DEST_DIR"
cp "target/${MAIN_JAR}" "$INPUT_DIR/"

build_app_image() {
    echo -e "${YELLOW}Step 3: Creating Standalone Portable App-Image (Linux x64)...${NC}"
    rm -rf "${DEST_DIR}/${APP_NAME}"
    jpackage \
      --type app-image \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$APP_VENDOR" \
      --description "$APP_DESCRIPTION" \
      --icon "$ICON_PATH" \
      --input "$INPUT_DIR" \
      --main-jar "$MAIN_JAR" \
      --main-class "$MAIN_CLASS" \
      --dest "$DEST_DIR" \
      --java-options "-Dfile.encoding=UTF-8" \
      --add-modules "$MODULES"

    echo -e "${GREEN}✓ Standalone App-Image created at: ${DEST_DIR}/${APP_NAME}/${NC}"
    echo -e "${GREEN}  Binary executable: ${DEST_DIR}/${APP_NAME}/bin/${APP_NAME}${NC}"
}

build_tarball() {
    echo -e "${YELLOW}Step 4: Compressing App-Image into portable .tar.gz bundle...${NC}"
    TAR_FILE="${DEST_DIR}/${APP_NAME}-${APP_VERSION}-linux-x64.tar.gz"
    tar -czf "$TAR_FILE" -C "$DEST_DIR" "$APP_NAME"
    echo -e "${GREEN}✓ Portable archive created: ${TAR_FILE}${NC}"
}

build_deb() {
    if command -v dpkg-deb &> /dev/null; then
        echo -e "${YELLOW}Step 5: Building native Debian/Ubuntu installer (.deb)...${NC}"
        jpackage \
          --type deb \
          --name "$DEB_NAME" \
          --app-version "$APP_VERSION" \
          --vendor "$APP_VENDOR" \
          --description "$APP_DESCRIPTION" \
          --icon "$ICON_PATH" \
          --input "$INPUT_DIR" \
          --main-jar "$MAIN_JAR" \
          --main-class "$MAIN_CLASS" \
          --dest "$DEST_DIR" \
          --linux-shortcut \
          --linux-menu-group "Office;Utility;" \
          --linux-app-category "Utility" \
          --linux-deb-maintainer "jalal@librarymanager.local" \
          --java-options "-Dfile.encoding=UTF-8" \
          --add-modules "$MODULES"

        echo -e "${GREEN}✓ Native Debian package created: $(ls ${DEST_DIR}/${DEB_NAME}_*.deb)${NC}"
        cp -f ${DEST_DIR}/${DEB_NAME}_*.deb "${DEST_DIR}/${APP_NAME}-${APP_VERSION}-linux-amd64.deb" 2>/dev/null || true
    else
        echo -e "${YELLOW}Skipping .deb generation (dpkg-deb not installed on this system).${NC}"
    fi
}

case "$MODE" in
    app-image)
        build_app_image
        ;;
    deb)
        build_deb
        ;;
    tar)
        build_app_image
        build_tarball
        ;;
    all)
        build_app_image
        build_tarball
        build_deb
        ;;
    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        echo "Usage: ./package-native.sh [app-image | deb | tar | all]"
        exit 1
        ;;
esac

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  Native Packaging Completed Successfully!           ${NC}"
echo -e "${GREEN}======================================================${NC}"
echo -e "Generated files in ${DEST_DIR}/:"
ls -lh "$DEST_DIR"
