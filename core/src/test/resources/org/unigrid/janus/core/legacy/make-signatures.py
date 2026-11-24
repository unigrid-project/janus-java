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

"""Signs a verification text with some of plain-wallet's keys the way the legacy wallet's signmessage does.

The signatures the Java signer makes are checked against these byte for byte, so this is written apart from it:
plain Python, the curve and RFC 6979 worked out here, with no library shared with Janus. The wallet hashes its
magic and the text, each led by its length, twice with SHA-256, and signs the hash with deterministic ECDSA and a
low s. The 65 bytes start with a header: 27, plus the recovery id, plus 4 for a compressed key.
"""

import base64
import hashlib
import hmac
import sys
from pathlib import Path

HERE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent

P = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F
N = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141
G = (0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798,
	0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8)
MAGIC = b"DarkNet Signed Message:\n"
TEXT = "verify 00112233445566778899aabbccddeeff PERSON 1763500000"
EACH = 3


def add(a, b):
	if a is None:
		return b
	if b is None:
		return a
	if a[0] == b[0] and (a[1] + b[1]) % P == 0:
		return None
	if a == b:
		slope = 3 * a[0] * a[0] * pow(2 * a[1], -1, P) % P
	else:
		slope = (b[1] - a[1]) * pow(b[0] - a[0], -1, P) % P
	x = (slope * slope - a[0] - b[0]) % P
	return x, (slope * (a[0] - x) - a[1]) % P


def multiply(k, point=G):
	result = None
	while k:
		if k & 1:
			result = add(result, point)
		point = add(point, point)
		k >>= 1
	return result


def public_key(secret, compressed):
	x, y = multiply(secret)
	if compressed:
		return bytes([2 + (y & 1)]) + x.to_bytes(32, "big")
	return b"\x04" + x.to_bytes(32, "big") + y.to_bytes(32, "big")


def message_hash(text):
	data = bytes([len(MAGIC)]) + MAGIC + bytes([len(text)]) + text.encode("ascii")
	return hashlib.sha256(hashlib.sha256(data).digest()).digest()


def nonce(secret, digest):
	"""RFC 6979 section 3.2 with HMAC-SHA-256, for a hash as long as the order."""
	x = secret.to_bytes(32, "big")
	h = (int.from_bytes(digest, "big") % N).to_bytes(32, "big")
	v, k = b"\x01" * 32, b"\x00" * 32
	k = hmac.new(k, v + b"\x00" + x + h, hashlib.sha256).digest()
	v = hmac.new(k, v, hashlib.sha256).digest()
	k = hmac.new(k, v + b"\x01" + x + h, hashlib.sha256).digest()
	v = hmac.new(k, v, hashlib.sha256).digest()
	while True:
		v = hmac.new(k, v, hashlib.sha256).digest()
		candidate = int.from_bytes(v, "big")
		if 1 <= candidate < N:
			return candidate
		k = hmac.new(k, v + b"\x00", hashlib.sha256).digest()
		v = hmac.new(k, v, hashlib.sha256).digest()


def sign(secret, compressed, text):
	"""The low s is kept, which flips the nonce point and so the parity of its y."""
	digest = message_hash(text)
	k = nonce(secret, digest)
	x, y = multiply(k)
	r = x % N
	s = pow(k, -1, N) * (int.from_bytes(digest, "big") + secret * r) % N
	flipped = s > N // 2
	recovery_id = ((y & 1) ^ flipped) + (2 if x >= N else 0)
	header = 27 + recovery_id + (4 if compressed else 0)
	return bytes([header]) + r.to_bytes(32, "big") + (N - s if flipped else s).to_bytes(32, "big")


keys = [line.split() for line in (HERE / "plain-wallet.keys").read_text().splitlines()]
chosen = [k for k in keys if k[2] == "compressed"][:EACH] + [k for k in keys if k[2] == "uncompressed"][:EACH]
lines = [f"# {TEXT}"]
for address, secret, encoding in chosen:
	signature = sign(int(secret, 16), encoding == "compressed", TEXT)
	lines.append(f"{address} {base64.b64encode(signature).decode()}")
(HERE / "verify-signatures.txt").write_text("\n".join(lines) + "\n")
