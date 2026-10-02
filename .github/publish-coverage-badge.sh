#!/bin/sh
# Works out the share of instructions the tests cover across the modules and keeps it, as the endpoint
# shields.io reads, on the badges branch so that no outside service or secret is needed.
set -eu

reports="core/target/site/jacoco/jacoco.csv web/target/site/jacoco/jacoco.csv ui/target/site/jacoco/jacoco.csv"
# Columns four and five of a JaCoCo csv are the instructions missed and covered; the first line is the header.
percent=$(awk -F, 'FNR > 1 { missed += $4; covered += $5 } END { printf "%d", 100 * covered / (missed + covered) }' $reports)

if [ "$percent" -ge 90 ]; then
	color=brightgreen
elif [ "$percent" -ge 75 ]; then
	color=green
elif [ "$percent" -ge 60 ]; then
	color=yellow
else
	color=red
fi

badge="${RUNNER_TEMP:-/tmp}/coverage.json"
printf '{"schemaVersion":1,"label":"coverage","message":"%s%%","color":"%s"}\n' "$percent" "$color" > "$badge"

git config user.name "Janus build"
git config user.email "noreply@users.noreply.github.com"

if git ls-remote --exit-code --heads origin badges > /dev/null; then
	git fetch --depth 1 origin badges
	git switch --force -C badges FETCH_HEAD
else
	git switch --orphan badges
fi

cp "$badge" coverage.json
git add coverage.json

if git diff --cached --quiet; then
	exit 0
fi

git commit --message "Update the coverage badge to ${percent}%"
git push origin badges
