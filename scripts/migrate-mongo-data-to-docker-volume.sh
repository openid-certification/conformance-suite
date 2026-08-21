#!/usr/bin/env bash
#
# One-time migration of the MongoDB dev data from the ./mongo/data bind mount
# into the named Docker volume (mongodb-data) now used by the Mac compose files.
#
# Background: Docker Desktop for Mac removed the legacy gRPC-FUSE file-sharing
# backend. Its successor VirtioFS presents bind-mounted files as root-owned and
# ignores chown, so mongod (running as uid 999) dies at startup with
#   WiredTiger ... handle-open: open: Operation not permitted
# (EPERM from WiredTiger's O_NOATIME open, which the kernel only permits for the
# file owner). The Mac compose files therefore switched /data/db to a named
# volume; this script copies your existing ./mongo/data into it so you keep
# your test history.
#
# Usage:
#   scripts/migrate-mongo-data-to-docker-volume.sh [compose-file] [--force]
#
#   compose-file  defaults to docker-compose-dev-mac-nodocker.yml
#                 (pass docker-compose-dev-mac.yml if that is what you use)
#   --force       overwrite data already present in the named volume
#
# Afterwards, start the stack as usual; once you are happy everything is there,
# reclaim the disk space with: rm -rf mongo/data

set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE_FILE="docker-compose-dev-mac-nodocker.yml"
FORCE=0
for arg in "$@"; do
	case "$arg" in
		--force) FORCE=1 ;;
		-h|--help) sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
		*) COMPOSE_FILE="$arg" ;;
	esac
done

MONGO_IMAGE="mongo:6.0.13"
VOLUME_KEY="mongodb-data"

die() { echo "ERROR: $*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || die "docker not found on PATH"
[ -f "$COMPOSE_FILE" ] || die "compose file '$COMPOSE_FILE' not found (run from the repo, or pass the file as argument)"

# Nothing to do on a fresh checkout
if [ ! -s mongo/data/WiredTiger.wt ]; then
	echo "No existing MongoDB data found at ./mongo/data - nothing to migrate."
	echo "Just start the stack normally; a fresh empty volume will be created."
	exit 0
fi

# The compose file must already contain the named-volume change
docker compose -f "$COMPOSE_FILE" config --volumes 2>/dev/null | grep -qx "$VOLUME_KEY" \
	|| die "'$COMPOSE_FILE' does not declare the '$VOLUME_KEY' volume - pull the branch with the named-volume change first"

# Resolve the volume name the same way compose does (honours COMPOSE_PROJECT_NAME)
project=$(docker compose -f "$COMPOSE_FILE" config 2>/dev/null | sed -n 's/^name: //p' | head -1)
[ -n "$project" ] || die "could not determine the compose project name"
volume="${project}_${VOLUME_KEY}"

# Let compose create the volume (with its labels) rather than 'docker run' doing it
docker compose -f "$COMPOSE_FILE" stop mongodb >/dev/null 2>&1 || true
docker compose -f "$COMPOSE_FILE" create mongodb >/dev/null

# Refuse to clobber a volume that already holds a database
if docker run --rm -v "$volume":/new "$MONGO_IMAGE" test -e /new/WiredTiger.wt; then
	if [ "$FORCE" -ne 1 ]; then
		die "volume '$volume' already contains MongoDB data - re-run with --force to overwrite it"
	fi
	echo "--force given: overwriting existing data in '$volume'"
	docker run --rm -v "$volume":/new "$MONGO_IMAGE" bash -c 'rm -rf /new/* /new/.[!.]* 2>/dev/null || true'
fi

size=$(du -sh mongo/data | cut -f1)
echo "Copying $size from ./mongo/data into volume '$volume' (this may take a few minutes)..."
docker run --rm -v ./mongo/data:/old:ro -v "$volume":/new "$MONGO_IMAGE" \
	bash -c 'cp -a /old/. /new/ && chown -R mongodb:mongodb /new'

# Sanity check: files present and owned by the mongodb user (uid 999)
docker run --rm -v "$volume":/new "$MONGO_IMAGE" \
	bash -c '[ -s /new/WiredTiger.wt ] && [ "$(stat -c %u /new/WiredTiger.wt)" = "999" ]' \
	|| die "verification failed - the copied data does not look right"

echo
echo "Done. Your data now lives in the Docker volume '$volume'."
echo "Start the stack as usual, e.g.:"
echo "  docker compose -f $COMPOSE_FILE up --remove-orphans --build"
echo
echo "./mongo/data was left untouched as a backup; once everything works,"
echo "reclaim the space with: rm -rf mongo/data"
