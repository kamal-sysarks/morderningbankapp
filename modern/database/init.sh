#!/usr/bin/env bash
# =============================================================================
# modern/database/init.sh
# Initializes the SQLite database for the modernized banking application.
#
# DEMO LOGIN:  username = admin   password = admin123
#
# Usage:
#   sh init.sh                  # creates ./legacybank.db in repo root
#   DB_PATH=/custom/path.db sh init.sh
#
# Idempotent: drops and recreates tables on every run (seed data included).
# Original legacy SQL files are read-only; this script does not modify them.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DB_PATH="${DB_PATH:-$REPO_ROOT/legacybank.db}"

SCHEMA_SQL="$REPO_ROOT/database/sqlite-schema.sql"
SEED_SQL="$REPO_ROOT/database/seed-data.sql"

echo "==> Database path : $DB_PATH"
echo "==> Schema        : $SCHEMA_SQL"
echo "==> Seed data     : $SEED_SQL"

# Verify source files exist (read-only legacy files)
if [ ! -f "$SCHEMA_SQL" ]; then
  echo "ERROR: schema file not found: $SCHEMA_SQL" >&2
  exit 1
fi
if [ ! -f "$SEED_SQL" ]; then
  echo "ERROR: seed file not found: $SEED_SQL" >&2
  exit 1
fi

# Check sqlite3 is available
if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "ERROR: sqlite3 is not installed. Install it and try again." >&2
  exit 1
fi

# Apply schema (drops and recreates tables)
echo "==> Applying schema..."
sqlite3 "$DB_PATH" < "$SCHEMA_SQL"

# Apply seed data
echo "==> Applying seed data..."
sqlite3 "$DB_PATH" < "$SEED_SQL"

# Migrate employee passwords from plaintext to BCrypt
# BCrypt hash of "admin123" (strength 10) — generated offline
echo "==> Migrating passwords to BCrypt..."
sqlite3 "$DB_PATH" <<'SQL'
UPDATE EMPLOYEES
SET PASSWORD = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE USERNAME = 'admin' AND LENGTH(PASSWORD) < 60;
SQL

echo "==> Done. Database ready at: $DB_PATH"
echo "==> Demo credentials: admin / admin123"
