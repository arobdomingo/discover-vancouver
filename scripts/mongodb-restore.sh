#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: CONFIRM_RESTORE=YES MONGODB_URI=... $0 backup.archive.gz" >&2
  exit 1
fi
if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "Set CONFIRM_RESTORE=YES because restore uses --drop" >&2
  exit 1
fi
if [[ -z "${MONGODB_URI:-}" ]]; then
  echo "MONGODB_URI is required" >&2
  exit 1
fi

mongorestore \
  --uri="$MONGODB_URI" \
  --archive="$1" \
  --gzip \
  --drop
echo "Restore completed"
