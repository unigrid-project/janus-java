#!/usr/bin/env bash
# Gives what jpackage wrote the same predictable names on every platform, which the updater
# and the release checksums rely on.
set -euo pipefail

version=${1:?usage: collect-installers.sh <version>}
here=$(cd "$(dirname "$0")" && pwd)
dist=$here/target/dist
out=$here/target/release

rm -rf "$out"
mkdir -p "$out"

case "$(uname -s)" in
Linux)
	cp "$dist"/unigrid_*.deb "$out/janus-$version-linux-x86_64.deb"
	cp "$dist"/unigrid-*.rpm "$out/janus-$version-linux-x86_64.rpm"
	tar -C "$dist" -czf "$out/janus-$version-linux-x86_64.tar.gz" Unigrid
	;;
esac

ls -l "$out"
