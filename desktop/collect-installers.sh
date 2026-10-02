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
	# A copy unpacked from the tar.gz has no package to put it in the menu, so it brings what does that.
	cp "$here/src/main/packaging/linux/Unigrid.desktop" "$dist/Unigrid/lib/Unigrid.desktop.in"
	install -m 755 "$here/src/main/packaging/linux/install-desktop.sh" "$dist/Unigrid/install-desktop.sh"
	tar -C "$dist" -czf "$out/janus-$version-linux-x86_64.tar.gz" Unigrid
	;;
Darwin)
	cp "$dist"/Unigrid-*.dmg "$out/janus-$version-macos-arm64.dmg"
	;;
MINGW* | MSYS* | CYGWIN*)
	cp "$dist"/Unigrid-*.msi "$out/janus-$version-windows-x64.msi"
	;;
esac

ls -l "$out"
