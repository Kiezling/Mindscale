#!/usr/bin/env bash
set -uo pipefail

# Android commit oracle. Claude hooks run under Git Bash on Windows while the
# Gradle wrapper is a Windows batch file, so each step is invoked via cmd.exe.

if [ -n "${CLAUDE_PROJECT_DIR:-}" ]; then
  cd "$CLAUDE_PROJECT_DIR" || exit 2
fi

mkdir -p .claude/logs
LOG=".claude/logs/oracle.log"

fail() {
  echo "ORACLE FAILED: $1" >&2
  echo "$(date -Iseconds) FAIL $1" >> "$LOG"
  exit 2
}

run_gradle() {
  local label="$1"
  local task="$2"
  # Double-slash cmd.exe switches prevent Git Bash/MSYS from rewriting /c as
  # a filesystem path. A single-slash /c can open an interactive prompt and
  # falsely return success without running Gradle.
  # The leading ".\" is required: on machines with NoDefaultCurrentDirectoryInExePath=1
  # set, cmd.exe does not search the current directory for a bare "gradlew.bat",
  # and fails with "not recognized" even though the file is right there.
  cmd.exe //d //s //c ".\\gradlew.bat $task" || fail "$label"
}

run_gradle "unit-tests" "test"
run_gradle "lint" "lint"
run_gradle "debug-assembly" "assembleDebug"

echo "$(date -Iseconds) PASS test lint assembleDebug" >> "$LOG"
exit 0
