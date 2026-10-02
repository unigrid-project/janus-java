#!/bin/sh
# Prepares a release on master: one commit that sets the release version, a tag on it, and one commit that
# moves master on to the next snapshot. Nothing is pushed. Pushing the tag is what starts the release workflow,
# which then leaves a draft release for review.
set -eu

root=$(cd "$(dirname "$0")" && pwd)
cd "$root"

current=$(mvn -B -q -DforceStdout help:evaluate -Dexpression=project.version)
snapshot=${current%-SNAPSHOT}
[ "$snapshot" != "$current" ] || { echo "Master is at $current, which is not a snapshot" >&2; exit 1; }

version=${1:-$snapshot}
echo "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$' || { echo "The version must look like 2.0.0, not '$version'" >&2; exit 1; }

# By default the patch number moves on, which is what a release of a snapshot most often leads to.
next=${2:-$(echo "$version" | awk -F. '{ printf "%d.%d.%d-SNAPSHOT", $1, $2, $3 + 1 }')}
echo "$next" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$' || { echo "The next version must look like 2.0.1-SNAPSHOT, not '$next'" >&2; exit 1; }

tag="v$version"

[ "$(git rev-parse --abbrev-ref HEAD)" = master ] || { echo "Releases are made from master" >&2; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "The working tree has changes; commit or stash them first" >&2; exit 1; }

git fetch --quiet origin master --tags
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/master)" ] || { echo "Master is not the same as origin/master" >&2; exit 1; }
! git rev-parse --quiet --verify "refs/tags/$tag" > /dev/null || { echo "$tag already exists" >&2; exit 1; }

# The release is made of what the build checks passed on, which is the commit this starts from.
if passed=$(gh run list --workflow maven.yml --commit "$(git rev-parse HEAD)" --status success --json databaseId --jq length 2> /dev/null); then
	[ "$passed" -gt 0 ] || { echo "The build checks have not passed for $(git rev-parse --short HEAD)" >&2; exit 1; }
else
	echo "Could not ask GitHub whether the build checks passed; the release workflow checks it again." >&2
fi

start=$(git rev-parse HEAD)
finished=no

# Whatever fails below leaves master where it was, as nothing has been pushed.
undo() {
	if [ "$finished" = no ]; then
		git reset --quiet --hard "$start"
		git tag --delete "$tag" > /dev/null 2>&1 || true
		echo "Nothing was released; master is back at $(git rev-parse --short "$start")" >&2
	fi
}
trap undo EXIT

set_version() {
	mvn -B -q versions:set -DnewVersion="$1" -DgenerateBackupPoms=false
	git commit --quiet --all --message "$2"
}

set_version "$version" "Release $version"
git tag --annotate "$tag" --message "Janus $version"
set_version "$next" "Begin work on $next"
finished=yes

git log --oneline --decorate -3
echo
echo "Check the two commits, then start the release with:"
echo "  git push --atomic origin master $tag"
