#!/bin/sh
# Copy of vempain-cluster/postgres-ssl-entrypoint.sh, used by docker-compose.yaml and docker-compose-site.yaml; keep the two identical.
set -eu

certificate_source=$1
key_source=$2

ssl_directory=/var/lib/postgresql/ssl
certificate_target="${ssl_directory}/server.crt"
key_target="${ssl_directory}/server.key"
hba_target="${ssl_directory}/pg_hba.conf"

mkdir -p "$ssl_directory"
cp "$certificate_source" "$certificate_target"
cp "$key_source" "$key_target"

# Network connections must use TLS: hostnossl is rejected for every client. The Unix socket inside the container stays open for the image's
# initialisation scripts and pg_isready health checks. The file is rewritten on every start, so edits to it inside the volume do not persist.
cat > "$hba_target" <<HBA
# TYPE     DATABASE  USER  ADDRESS  METHOD
local      all       all            trust
hostssl    all       all   all      ${POSTGRES_HOST_AUTH_METHOD:-scram-sha-256}
hostnossl  all       all   all      reject
HBA

chown postgres:postgres "$certificate_target" "$key_target" "$hba_target"
chmod 644 "$certificate_target"
chmod 600 "$key_target" "$hba_target"

shift 2
exec /usr/local/bin/docker-entrypoint.sh postgres \
  -c ssl=on \
  -c ssl_cert_file="$certificate_target" \
  -c ssl_key_file="$key_target" \
  -c hba_file="$hba_target" \
  "$@"
