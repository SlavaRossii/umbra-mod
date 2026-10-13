#!/bin/bash
# Force-pushes the given files to an orphan branch so the session can read CI output with git.
# usage: publish.sh <branch> <files...>
set -u
BRANCH=$1; shift
OUT=$(mktemp -d)
for f in "$@"; do [ -f "$f" ] && cp "$f" "$OUT/"; done
echo "run $GITHUB_RUN_ID sha $GITHUB_SHA job $GITHUB_JOB" > "$OUT/RUN.txt"
cd "$OUT"
git init -q
git checkout -q -b "$BRANCH"
git add -A
git -c user.name=ci -c user.email=ci@localhost commit -qm "CI output for run $GITHUB_RUN_ID"
git push -q -f "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}" "$BRANCH"
