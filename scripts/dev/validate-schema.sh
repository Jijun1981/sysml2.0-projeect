#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

SCHEMA_RUNTIME="$ROOT_DIR/server/build/generated/schema.graphql"
SCHEMA_SNAPSHOT="$ROOT_DIR/server/src/test/resources/schema.snapshot.graphql"

if [[ ! -f "$SCHEMA_RUNTIME" ]]; then
  echo "Runtime schema not found: $SCHEMA_RUNTIME" >&2
  exit 2
fi
if [[ ! -f "$SCHEMA_SNAPSHOT" ]]; then
  echo "Snapshot missing, creating from runtime..."
  mkdir -p "$(dirname "$SCHEMA_SNAPSHOT")"
  cp "$SCHEMA_RUNTIME" "$SCHEMA_SNAPSHOT"
  echo "Snapshot created at $SCHEMA_SNAPSHOT"
  exit 0
fi

if diff -u "$SCHEMA_SNAPSHOT" "$SCHEMA_RUNTIME"; then
  echo "Schema matches snapshot"
else
  echo "Schema differs from snapshot" >&2
  exit 3
fi



