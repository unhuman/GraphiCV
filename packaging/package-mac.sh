#!/usr/bin/env bash
#
# Builds a self-contained macOS application for GraphiCV using jpackage.
#
# Produces target/dist/GraphiCV.app -- a native .app bundled with a trimmed Java
# runtime, so end users do not need Java installed.
#
# Usage:
#   ./packaging/package-mac.sh              # build GraphiCV.app
#   ./packaging/package-mac.sh --installer  # also build a .dmg
#   ./packaging/package-mac.sh --installer --type pkg
#
# --skip-build packages whatever jar is already in target/ instead of running Maven
# first. That is how Maven itself invokes this script during the package phase.
#
# Requires a JDK 17 or newer (for jpackage) and Maven. Building a .dmg/.pkg needs
# only Xcode command line tools, which macOS prompts to install if missing.
#
# The resulting app is unsigned. To ship it to other machines, sign and notarize
# it, or tell users to right-click -> Open the first time. To sign during the
# build, set MAC_SIGNING_IDENTITY to a "Developer ID Application: ..." identity.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="GraphiCV"
APP_VERSION="1.0"
MAIN_CLASS="com.unhuman.graphicv.GraphiCV"
JAR_NAME="${APP_NAME}-${APP_VERSION}.jar"

STAGE_DIR="${PROJECT_ROOT}/target/jpackage-input"
DIST_DIR="${PROJECT_ROOT}/target/dist"

INSTALLER=false
SKIP_BUILD=false
TYPE="dmg"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --installer)  INSTALLER=true; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    --type)       TYPE="$2"; shift 2 ;;
    -h|--help)    sed -n '2,28p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *)            echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

# --- 1. Build the runnable jar -------------------------------------------------
# -Djpackage.skip=true stops Maven from calling this script back (see pom.xml).
if [[ "${SKIP_BUILD}" != true ]]; then
  echo "==> Building ${JAR_NAME} with Maven..."
  mvn -f "${PROJECT_ROOT}/pom.xml" -q -Djpackage.skip=true clean package
fi

JAR_PATH="${PROJECT_ROOT}/target/${JAR_NAME}"
[[ -f "${JAR_PATH}" ]] || { echo "Expected jar not found: ${JAR_PATH}" >&2; exit 1; }

# --- 2. Stage just the jar for jpackage ---------------------------------------
rm -rf "${STAGE_DIR}" "${DIST_DIR}"
mkdir -p "${STAGE_DIR}" "${DIST_DIR}"
cp "${JAR_PATH}" "${STAGE_DIR}/"

# --- 3. Run jpackage -----------------------------------------------------------
command -v jpackage >/dev/null 2>&1 || {
  echo "jpackage was not found on the PATH. Install a JDK 17 or newer and put its bin directory on the PATH." >&2
  exit 1
}

ARGS=(
  --name        "${APP_NAME}"
  --app-version "${APP_VERSION}"
  --input       "${STAGE_DIR}"
  --main-jar    "${JAR_NAME}"
  --main-class  "${MAIN_CLASS}"
  --dest        "${DIST_DIR}"
  --vendor      "unHUman / Howard Uman"
  --description "Graphics / Sprite Editor for ColecoVision and TI-99/4A"
  --mac-package-identifier "com.unhuman.graphicv"
)

if [[ "${INSTALLER}" == true ]]; then
  ARGS+=(--type "${TYPE}")
  echo "==> Running jpackage (installer: ${TYPE})..."
else
  ARGS+=(--type app-image)
  echo "==> Running jpackage (app-image)..."
fi

ICON_PATH="${PROJECT_ROOT}/packaging/GraphiCV.icns"
[[ -f "${ICON_PATH}" ]] && ARGS+=(--icon "${ICON_PATH}")

if [[ -n "${MAC_SIGNING_IDENTITY:-}" ]]; then
  ARGS+=(--mac-sign --mac-signing-key-user-name "${MAC_SIGNING_IDENTITY}")
fi

jpackage "${ARGS[@]}"

echo
echo "Done. Output in: ${DIST_DIR}"
ls -1 "${DIST_DIR}"
if [[ "${INSTALLER}" != true ]]; then
  echo
  echo "Run it with: open \"${DIST_DIR}/${APP_NAME}.app\""
fi
