#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${MONGODB_URI:-}" ]]; then
  echo "MONGODB_URI is required" >&2
  exit 1
fi

backup_directory="${BACKUP_DIRECTORY:-./backups}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$backup_directory"
archive="$backup_directory/discover-vancouver-$timestamp.archive.gz"

mongodump --uri="$MONGODB_URI" --archive="$archive" --gzip
echo "Backup written to $archive"
