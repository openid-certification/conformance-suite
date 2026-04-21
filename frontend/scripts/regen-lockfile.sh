#!/usr/bin/env bash
#
# Regenerate frontend/package-lock.json in an isolated Linux environment that
# matches CI. Use after changing frontend/package.json (adding, removing, or
# bumping a dependency).
#
# Why this script exists:
#   `npm install` on macOS produces a platform-biased lockfile: npm records
#   only the OS-specific optional deps that apply to the host platform, and
#   omits the Linux binaries (esbuild, rollup, lightningcss, …) that CI needs.
#   The result installs fine on the contributor's laptop, passes `npm ci`
#   locally on macOS, and then breaks GitLab CI inside node:22-alpine with
#   `npm ci` "Missing: X from lock file" / "Invalid: Y does not satisfy Z"
#   errors -- often for packages like react, @testing-library/dom, valibot
#   that arrive as optional peer deps.
#
#   Regenerating inside the CI image produces a lockfile that works for
#   every platform: `npm ci` succeeds on macOS, Linux, and Windows, because
#   each runtime only reifies the optional packages relevant to its OS.
#
# Usage:
#   cd frontend && ./scripts/regen-lockfile.sh
#
# Requirements:
#   Docker Desktop (or any Docker-compatible runtime). No Node/npm needed on
#   the host -- the npm version pinned in package.json's `packageManager`
#   field is activated inside the container via Corepack, so the lockfile
#   always matches that pin.
#
# After running:
#   1. Run `npm ci --ignore-scripts` to replace the container-written Linux
#      binaries in node_modules/ with ones for your host OS. (The script
#      removes node_modules/ at the end so a stale Linux tree doesn't silently
#      break `npm run …` on macOS.)
#   2. Commit only frontend/package-lock.json (frontend/package.json should
#      already reflect your intended dependency change).

set -euo pipefail

# Mirror the image used by GitLab CI's frontend_lint job. Keep in sync with
# .gitlab-ci.yml's `image: node:22-alpine` entry.
IMAGE="node:22-alpine"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Read the npm version from the single source of truth in package.json's
# `packageManager` field. This way the script automatically follows future
# bumps of the pin without a second edit.
if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: node is required on the host to read the packageManager pin from package.json." >&2
  echo "Install Node (any recent version will do) and retry." >&2
  exit 1
fi
NPM_VERSION="$(node -e "const p=require('${FRONTEND_DIR}/package.json'); const v=(p.packageManager||'').match(/^npm@([^+]+)/); if(!v){process.exit(1)} process.stdout.write(v[1])")"
if [ -z "${NPM_VERSION}" ]; then
  echo "ERROR: could not parse packageManager pin from ${FRONTEND_DIR}/package.json." >&2
  echo "Expected a value like \"npm@11.12.1\"." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is required but not found in PATH." >&2
  echo "Install Docker Desktop or set up a Docker-compatible runtime, then retry." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "ERROR: docker is installed but the daemon is not reachable." >&2
  echo "Start Docker Desktop (or your Docker runtime) and retry." >&2
  exit 1
fi

# Back up the current lockfile so a mid-run failure (registry outage, disk
# full, Ctrl-C) doesn't leave the working tree with no lockfile at all.
LOCKFILE="${FRONTEND_DIR}/package-lock.json"
BACKUP=""
if [ -f "${LOCKFILE}" ]; then
  BACKUP="$(mktemp -t package-lock.bak.XXXXXX)"
  cp "${LOCKFILE}" "${BACKUP}"
  # shellcheck disable=SC2064
  trap "
    status=\$?
    if [ \$status -ne 0 ] && [ -n \"${BACKUP}\" ] && [ -f \"${BACKUP}\" ]; then
      echo \"Restoring original package-lock.json after failure.\" >&2
      cp \"${BACKUP}\" \"${LOCKFILE}\"
    fi
    if [ -n \"${BACKUP}\" ] && [ -f \"${BACKUP}\" ]; then
      rm -f \"${BACKUP}\"
    fi
    exit \$status
  " EXIT
fi

echo "Regenerating frontend/package-lock.json inside ${IMAGE} with npm@${NPM_VERSION}..."

# Bound worst-case hangs on corepack's npm tarball download. `timeout` ships
# with GNU coreutils on Linux; on macOS, `brew install coreutils` provides it
# as `gtimeout`. If neither is present, run without a timeout rather than
# failing the script for a nice-to-have.
if command -v timeout >/dev/null 2>&1; then
  TIMEOUT=(timeout 300)
elif command -v gtimeout >/dev/null 2>&1; then
  TIMEOUT=(gtimeout 300)
else
  echo "Note: neither \`timeout\` nor \`gtimeout\` found; running without a time limit." >&2
  echo "      Install coreutils (\`brew install coreutils\` on macOS) for automatic timeouts." >&2
  TIMEOUT=()
fi

# --user pins the container's write UID to the invoking user so the regenerated
# lockfile (and any leftover files) stay owned by the contributor, not root.
HOST_UID="$(id -u)"
HOST_GID="$(id -g)"
"${TIMEOUT[@]+"${TIMEOUT[@]}"}" docker run --rm \
  --user "${HOST_UID}:${HOST_GID}" \
  -v "${FRONTEND_DIR}:/w" \
  -w /w \
  "${IMAGE}" \
  sh -c "
    set -eu
    # Corepack's cache path defaults to \$HOME, but --user runs us as an
    # arbitrary UID with no $HOME; point it at a writable location.
    export HOME=/tmp
    mkdir -p /tmp/corepack-bin
    corepack enable --install-directory /tmp/corepack-bin npm
    export PATH=/tmp/corepack-bin:\$PATH
    corepack prepare npm@${NPM_VERSION} --activate >/dev/null
    echo \"Using npm \$(npm --version)\"
    rm -rf node_modules package-lock.json
    npm install --ignore-scripts
  "

# Remove the container-written (Linux-native) node_modules on the host so
# subsequent host-side `npm run …` / `npx` don't try to exec Linux binaries on
# macOS/Windows. The contributor restores a host-native tree with `npm ci`.
# Host-side rm avoids bind-mount quirks on Docker Desktop for macOS where
# in-container rm across the mount can fail with "Directory not empty".
rm -rf "${FRONTEND_DIR}/node_modules"

echo ""
echo "Done. Next steps:"
echo "  1. Restore a host-native install:"
echo "       npm ci --ignore-scripts"
echo "  2. Review and commit only the lockfile:"
echo "       git diff --stat frontend/package-lock.json"
echo "       git add frontend/package-lock.json"
echo "       git commit -m 'chore(frontend): regenerate package-lock.json'"
