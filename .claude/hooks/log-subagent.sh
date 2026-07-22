#!/usr/bin/env bash
set -uo pipefail

INPUT=$(cat)

if [ -n "${CLAUDE_PROJECT_DIR:-}" ]; then
  cd "$CLAUDE_PROJECT_DIR" || exit 0
fi

mkdir -p .claude/logs

AGENT=$(printf '%s' "$INPUT" | node -e '
let data = "";
process.stdin.on("data", chunk => data += chunk);
process.stdin.on("end", () => {
  try {
    const parsed = JSON.parse(data);
    process.stdout.write(parsed.agent_type || parsed.subagent_type || "?");
  } catch {
    process.stdout.write("?");
  }
});
' 2>/dev/null)

echo "$(date -Iseconds) ${AGENT:-?}" >> .claude/logs/subagents.log
exit 0
