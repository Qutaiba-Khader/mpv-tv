#!/bin/bash
set -euo pipefail

# Sync mpv-tv with upstream mpv-android
# Run from repo root: ./scripts/sync-upstream.sh

UPSTREAM_REMOTE="upstream"
UPSTREAM_BRANCH="master"

echo "Fetching upstream..."
git fetch "$UPSTREAM_REMOTE"

BEHIND=$(git rev-list --count master.."$UPSTREAM_REMOTE/$UPSTREAM_BRANCH")
if [ "$BEHIND" -eq 0 ]; then
    echo "Already up to date with upstream."
    exit 0
fi

echo "$BEHIND new commits from upstream."

echo "Merging upstream into master..."
git checkout master
git merge "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH" --no-edit

echo "Merging master into dev..."
git checkout dev
git merge master --no-edit

echo "Done. Review any conflicts, then push:"
echo "  git push origin dev master"
