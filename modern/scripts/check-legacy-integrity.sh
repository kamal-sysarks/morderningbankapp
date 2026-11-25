#!/usr/bin/env bash
# =============================================================================
# modern/scripts/check-legacy-integrity.sh
# Verifies that legacy source files have not been modified.
#
# Usage:
#   sh check-legacy-integrity.sh            # verify against baseline
#   sh check-legacy-integrity.sh --generate # create/update baseline
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BASELINE="$SCRIPT_DIR/legacy-checksums.sha256"

# Detect shasum command (macOS uses shasum, Linux uses sha256sum)
if command -v sha256sum >/dev/null 2>&1; then
  SHA_CMD="sha256sum"
elif command -v shasum >/dev/null 2>&1; then
  SHA_CMD="shasum -a 256"
else
  echo "ERROR: Neither sha256sum nor shasum found." >&2
  exit 1
fi

# Collect files to hash — using a temp file for POSIX sh compatibility
_tmplist=$(mktemp)
find "$REPO_ROOT/src" -type f 2>/dev/null | sort > "$_tmplist"
FILES_TO_CHECK=()
while IFS= read -r f; do
  FILES_TO_CHECK+=("$f")
done < "$_tmplist"
rm -f "$_tmplist"

FILES_TO_CHECK+=("$REPO_ROOT/pom.xml")

if [ -f "$REPO_ROOT/legacybank.db" ]; then
  FILES_TO_CHECK+=("$REPO_ROOT/legacybank.db")
fi

if [ "$1" = "--generate" ]; then
  echo "==> Generating baseline checksums..."
  > "$BASELINE"
  for f in "${FILES_TO_CHECK[@]}"; do
    rel="${f#$REPO_ROOT/}"
    hash=$($SHA_CMD "$f" | awk '{print $1}')
    echo "$hash  $rel" >> "$BASELINE"
    echo "  $rel"
  done
  echo "==> Baseline saved to: $BASELINE"
  exit 0
fi

if [ ! -f "$BASELINE" ]; then
  echo "ERROR: Baseline not found at $BASELINE" >&2
  echo "Run with --generate first." >&2
  exit 1
fi

echo "==> Checking legacy file integrity..."
FAILED=0

while IFS= read -r line; do
  expected_hash=$(echo "$line" | awk '{print $1}')
  rel_path=$(echo "$line" | awk '{print $NF}')
  abs_path="$REPO_ROOT/$rel_path"

  if [ ! -f "$abs_path" ]; then
    echo "MISSING: $rel_path"
    FAILED=$((FAILED + 1))
    continue
  fi

  actual_hash=$($SHA_CMD "$abs_path" | awk '{print $1}')
  if [ "$actual_hash" != "$expected_hash" ]; then
    echo "CHANGED: $rel_path"
    FAILED=$((FAILED + 1))
  fi
done < "$BASELINE"

if [ "$FAILED" -eq 0 ]; then
  echo "==> All legacy files are unchanged. ✓"
  exit 0
else
  echo "==> $FAILED file(s) have been modified!" >&2
  exit 1
fi
