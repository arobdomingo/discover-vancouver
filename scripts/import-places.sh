#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

exec mvn spring-boot:run \
  -Dspring-boot.run.main-class=com.discovervancouver.PlaceIngestionApplication
