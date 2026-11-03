#!/bin/sh
set -eu
mkdir -p docs/database
docker compose exec -T user-db pg_dump -U postgres --no-owner --no-privileges user_db > docs/database/user_db.sql
docker compose exec -T device-db pg_dump -U postgres --no-owner --no-privileges device_db > docs/database/device_db.sql
docker compose exec -T credential-db pg_dump -U postgres --no-owner --no-privileges credential_db > docs/database/credential_db.sql
printf 'Saved user_db.sql, device_db.sql and credential_db.sql\n'
