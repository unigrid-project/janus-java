#!/usr/bin/env python3
#
#    The Janus Wallet
#    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation
#
#    This program is free software: you can redistribute it and/or modify it under the terms of the
#    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
#    of the License (see COPYING and COPYING.addendum).
#
#    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#    GNU Affero General Public License for more details.
#
#    You should have received an addended copy of the GNU Affero General Public License with this program.
#    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.

"""Builds the wallet dump the dump reader is tested against, and the addresses its keys give.

The dump has the layout the legacy daemon's dumpwallet writes (rpcdump.cpp): a header, then one line
per key with the private key, its birth time and a label, reserve or change mark, then the address in
a comment. The keys are made up and never held coins. Only the standard library is needed, so the
public keys come from a small secp256k1 multiplication done here rather than by the reader under test.
"""

import hashlib
from pathlib import Path

ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
P = 2**256 - 2**32 - 977
N = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141
G = (0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798,
	0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8)
SECRET_VERSION = 153
ADDRESS_VERSION = 40
HEADER = """# Wallet dump created by UNIGRID 2.9.17 (2019-03-14 09:26:53 +0100)
# * Created on 2019-03-14T08:26:53Z
# * Best block at time of backup was 420000 (0000000000000000000000000000000000000000000000000000000000000000),
#   mined on 2019-03-14T08:20:00Z
"""


def add(a, b):
	if a is None:
		return b
	if b is None:
		return a
	if a[0] == b[0] and (a[1] + b[1]) % P == 0:
		return None
	slope = 3 * a[0] * a[0] * pow(2 * a[1], -1, P) if a == b else (b[1] - a[1]) * pow(b[0] - a[0], -1, P)
	x = (slope * slope - a[0] - b[0]) % P
	return (x, (slope * (a[0] - x) - a[1]) % P)


def multiply(secret):
	result, point = None, G
	while secret:
		if secret & 1:
			result = add(result, point)
		point, secret = add(point, point), secret >> 1
	return result


def public_key(secret, compressed):
	x, y = multiply(secret)
	if compressed:
		return bytes([2 + (y & 1)]) + x.to_bytes(32, "big")
	return b"\x04" + x.to_bytes(32, "big") + y.to_bytes(32, "big")


def base58check(version, payload):
	data = bytes([version]) + payload
	number = int.from_bytes(data + hashlib.sha256(hashlib.sha256(data).digest()).digest()[:4], "big")
	text = ""
	while number:
		number, rest = divmod(number, 58)
		text = ALPHABET[rest] + text
	return text


def address(secret, compressed):
	hashed = hashlib.new("ripemd160", hashlib.sha256(public_key(secret, compressed)).digest()).digest()
	return base58check(ADDRESS_VERSION, hashed)


def wif(secret, compressed):
	return base58check(SECRET_VERSION, secret.to_bytes(32, "big") + (b"\x01" if compressed else b""))


def own_secret(n):
	return int.from_bytes(hashlib.sha256(f"janus dump fixture {n}".encode()).digest(), "big") % N


def percent_encode(label):
	return "".join(f"%{ord(c):02x}" if ord(c) <= 32 or ord(c) >= 128 or c == "%" else c for c in label)


KEYS = [
	(1, True, "2018-01-02T10:00:00Z", "label=" + percent_encode("Mining rewards")),
	(2, True, "2018-01-02T10:00:01Z", "label="),
	(3, False, "2018-02-11T07:15:30Z", "label=" + percent_encode("100% déjà vu")),
	(4, True, "2018-03-30T21:45:12Z", "reserve=1"),
	(5, False, "2018-03-30T21:45:13Z", "reserve=1"),
	(6, True, "2018-04-01T00:00:00Z", "change=1"),
]


def main():
	folder = Path(__file__).parent
	lines = [HEADER]
	addresses = []

	for n, compressed, born, mark in KEYS:
		secret = own_secret(n)
		addresses.append(address(secret, compressed))
		lines.append(f"{wif(secret, compressed)} {born} {mark} # addr={addresses[-1]}\n")

	lines.append("\n# End of dump\n")
	(folder / "wallet.dump").write_text("".join(lines), encoding="latin-1", newline="\n")
	(folder / "wallet.dump.addresses").write_text("\n".join(sorted(addresses)) + "\n", encoding="ascii")


if __name__ == "__main__":
	main()
