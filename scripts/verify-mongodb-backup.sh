#!/usr/bin/env bash
set -euo pipefail

container="${MONGO_CONTAINER:-discover-vancouver-mongo}"
database="discover_vancouver_backup_verification"
archive="/tmp/$database.archive.gz"

cleanup() {
  docker exec "$container" mongosh --quiet --eval \
    "db.getSiblingDB('$database').dropDatabase()" >/dev/null || true
  docker exec "$container" rm -f "$archive" >/dev/null || true
}
trap cleanup EXIT

docker exec "$container" mongosh --quiet --eval \
  "db.getSiblingDB('$database').places.insertOne({_id:'backup-check',name:'Backup Check'})" >/dev/null
docker exec "$container" mongodump \
  --db="$database" --archive="$archive" --gzip >/dev/null
docker exec "$container" mongosh --quiet --eval \
  "db.getSiblingDB('$database').dropDatabase()" >/dev/null
docker exec "$container" mongorestore \
  --archive="$archive" --gzip >/dev/null

restored="$(docker exec "$container" mongosh --quiet --eval \
  "db.getSiblingDB('$database').places.countDocuments({_id:'backup-check'})")"
if [[ "$restored" != "1" ]]; then
  echo "Backup verification failed: expected one restored document" >&2
  exit 1
fi
echo "MongoDB backup and restore verification passed"
