#!/usr/bin/env bash
set -uo pipefail

# Supplemental guardrail: reject broadly destructive commands and require a
# passing Android oracle before non-documentation commits.

INPUT=$(cat)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CMD=$(printf '%s' "$INPUT" | node -e '
let data = "";
process.stdin.on("data", chunk => data += chunk);
process.stdin.on("end", () => {
  try {
    const parsed = JSON.parse(data);
    process.stdout.write((parsed.tool_input && parsed.tool_input.command) || "");
  } catch {
    process.stdout.write("");
  }
});
' 2>/dev/null)

if [ -z "$CMD" ]; then
  CMD="$INPUT"
fi

is_destructive() {
  printf '%s' "$CMD" | grep -qiE 'rm[[:space:]]+-rf[[:space:]]+(/|\.\.)' && return 0
  printf '%s' "$CMD" | grep -qiE 'git[[:space:]]+reset[[:space:]]+--hard' && return 0
  printf '%s' "$CMD" | grep -qiE 'git[[:space:]]+clean[[:space:]]+[^|;&]*-[a-zA-Z]*f' && return 0
  printf '%s' "$CMD" | grep -qiE 'git[[:space:]]+push[[:space:]]+([^|;&]*[[:space:]])?(-f|--force)' && return 0
  printf '%s' "$CMD" | grep -qiE 'Remove-Item[^|;&]*-Recurse[^|;&]*-Force' && return 0
  printf '%s' "$CMD" | grep -qiE 'Remove-Item[^|;&]*-Force[^|;&]*-Recurse' && return 0
  printf '%s' "$CMD" | grep -qiE '(^|[[:space:]])diskpart([[:space:]]|$)' && return 0
  printf '%s' "$CMD" | grep -qiE '(^|[[:space:]])format([[:space:]]+[A-Za-z]:|[[:space:]]+/)' && return 0
  return 1
}

if is_destructive; then
  echo "Blocked broadly destructive command. Obtain explicit human approval and use a narrowly scoped manual action." >&2
  exit 2
fi

if printf '%s' "$CMD" | grep -qE '(^|[;&|[:space:]])git[[:space:]]+commit([[:space:]]|$)'; then
  STATUS="$(git status --porcelain --untracked-files=all 2>/dev/null)"

  if [ -z "$STATUS" ]; then
    exec "$SCRIPT_DIR/oracle.sh"
  fi

  DOCS_ONLY=1
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    path="${line:3}"
    if printf '%s' "$path" | grep -q ' -> '; then
      path="${path#*-> }"
    fi
    case "$path" in
      *.md|*.txt|.claude/*|.gitattributes|.gitignore) ;;
      *) DOCS_ONLY=0 ;;
    esac
  done <<< "$STATUS"

  if [ "$DOCS_ONLY" -eq 1 ]; then
    exit 0
  fi

  exec "$SCRIPT_DIR/oracle.sh"
fi

exit 0
