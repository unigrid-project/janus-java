#!/bin/sh
# Pins the Hedgehog release Janus runs against. The checksums come from the release's own SHA256SUMS, which is
# trusted only once its signature has been checked against the bundled release key.
set -eu

version=${1:?usage: ./bump-hedgehog.sh <Hedgehog version, for example 0.0.8>}
root=$(cd "$(dirname "$0")" && pwd)
release="https://github.com/unigrid-project/hedgehog/releases/download/v$version"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# A keyring of its own, so that nothing is added to the one of whoever runs this.
export GNUPGHOME="$work/gnupg"
mkdir -m 700 "$GNUPGHOME"
gpg --batch --quiet --import "$root/release-key.asc"

curl -fsSL -o "$work/SHA256SUMS" "$release/SHA256SUMS"
curl -fsSL -o "$work/SHA256SUMS.asc" "$release/SHA256SUMS.asc"
gpg --batch --verify "$work/SHA256SUMS.asc" "$work/SHA256SUMS"

pin() {
	checksum=$(awk -v asset="hedgehog-$version-$2" '$2 == asset { print $1 }' "$work/SHA256SUMS")
	[ -n "$checksum" ] || { echo "SHA256SUMS lists no hedgehog-$version-$2" >&2; exit 1; }
	sed -i "s|<hedgehog.sha256.$1>.*</hedgehog.sha256.$1>|<hedgehog.sha256.$1>$checksum</hedgehog.sha256.$1>|" "$root/pom.xml"
}

pin linux x86_64-linux-gnu.bin
pin macos osx-arm64.bin
pin windows win64.exe
sed -i "s|<hedgehog.version>.*</hedgehog.version>|<hedgehog.version>$version</hedgehog.version>|" "$root/pom.xml"
echo "Pinned Hedgehog $version"
