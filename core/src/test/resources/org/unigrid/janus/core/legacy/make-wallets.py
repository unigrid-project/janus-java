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

"""Builds the Berkeley DB 4.8 wallets the legacy reader is tested against.

Needs db4.8_load on the PATH and Python's cryptography package. The keys are made up and never held
coins. Small pages force the btree to grow internal pages, and the large values force overflow pages.
The plain and encrypted wallets carry the records the legacy daemon writes (walletdb.cpp), with real
throwaway keys, encrypted as its crypter.cpp encrypts them. Beside each go the addresses its keys give
and the keys themselves, worked out here rather than by the reader under test.
"""

import hashlib
import struct
import subprocess
import sys
from pathlib import Path

from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

HERE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent

UNCOMPRESSED = bytes.fromhex(
	"04b36556d4e6822708431cce73eaf447a0ec89a8ae6eb48aa412cb5b56bb6410"
	"acaa7cda7000e270b9900eb77667bb421728cab77e720c7ca2118150430c4f418a")
COMPRESSED = b"\x02" + UNCOMPRESSED[1:33]
POOLED = b"\x03" + hashlib.sha256(b"pool").digest()
OLD_STYLE = b"\x02" + hashlib.sha256(b"wkey").digest()
ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
CREATED = 1541767134
ORDER = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141
WALLET_VERSION = 61000

# The encrypted wallet's passphrase, with a letter outside ASCII, as the daemon hashed the passphrase's UTF-8.
PASSPHRASE = "Unigrid tëst passphrase".encode()
ROUNDS = 144109

# What the daemon writes around a secret in DER (key.cpp, ec_privkey_export_der), before it and after it up to
# the public key; only what comes before the secret is ever read back.
DER_COMPRESSED = bytes.fromhex("3081d30201010420"), bytes.fromhex(
	"a08185308182020101302c06072a8648ce3d0101022100ffffffffffffffffffffffffffffffffffffffffffffffffff"
	"fffffefffffc2f300604010004010704210279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f8"
	"1798022100fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141020101a124032200")
DER_UNCOMPRESSED = bytes.fromhex("308201130201010420"), bytes.fromhex(
	"a081a53081a2020101302c06072a8648ce3d0101022100ffffffffffffffffffffffffffffffffffffffffffffffffff"
	"fffffefffffc2f300604010004010704410479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f8"
	"1798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8022100ffffffffffffffffffffff"
	"fffffffffebaaedce6af48a03bbfd25e8cd0364141020101a144034200")


def compact(data):
	return bytes([len(data)]) + data if len(data) < 253 else b"\xfd" + struct.pack("<H", len(data)) + data


def key(kind, rest=b""):
	return compact(kind.encode()) + rest


def records(*extra):
	yield from extra
	yield key("version"), struct.pack("<i", 1010000)
	yield key("name", compact(b"HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE")), compact(b"someone else")
	yield key("key", compact(UNCOMPRESSED)), compact(b"\x30" * 214) + b"\x00" * 32
	yield key("keymeta", compact(UNCOMPRESSED)), struct.pack("<iq", 1, 1500000000)
	yield key("ckey", compact(COMPRESSED)), compact(b"\x5a" * 48)
	yield key("pool", struct.pack("<q", 7)), struct.pack("<iq", 1010000, 1500000000) + compact(POOLED)
	yield key("watchs", compact(b"\x76\xa9\x14" + bytes(20) + b"\x88\xac")), b"\x01"
	yield key("cscript", bytes(20)), compact(b"\x51")
	yield key("wkey", compact(OLD_STYLE)), compact(b"\x30" * 214)
	yield key("mkey", struct.pack("<I", 1)), compact(b"\x11" * 48)
	yield key("destdata", compact(b"big")), compact(b"\xab" * 3000)
	for n in range(300):
		yield key("tx", hashlib.sha256(struct.pack("<I", n)).digest()), b"\xcd" * 40


def address(public_key):
	payload = bytes([40]) + hashlib.new("ripemd160", hashlib.sha256(public_key).digest()).digest()
	number = int.from_bytes(payload + hashlib.sha256(hashlib.sha256(payload).digest()).digest()[:4], "big")
	text = ""
	while number:
		number, digit = divmod(number, 58)
		text = ALPHABET[digit] + text
	return text


def hash256(data):
	return hashlib.sha256(hashlib.sha256(data).digest()).digest()


def own_key(name, n):
	"""A throwaway key, its secret made from the wallet's name and the key's number; every fifth is uncompressed."""
	secret = int.from_bytes(hashlib.sha256(f"{name}/{n}".encode()).digest(), "big") % (ORDER - 1) + 1
	form = PublicFormat.UncompressedPoint if n % 5 == 0 else PublicFormat.CompressedPoint
	public_key = ec.derive_private_key(secret, ec.SECP256K1()).public_key().public_bytes(Encoding.X962, form)
	return secret.to_bytes(32, "big"), public_key


def encrypt(key_bytes, iv, data):
	"""AES-256-CBC with PKCS#7 padding, as OpenSSL does it for the daemon."""
	padder = padding.PKCS7(128).padder()
	encryptor = Cipher(algorithms.AES(key_bytes), modes.CBC(iv)).encryptor()
	return encryptor.update(padder.update(data) + padder.finalize()) + encryptor.finalize()


def passphrase_key(salt):
	"""EVP_BytesToKey with SHA-512: the key and IV the master key is encrypted under."""
	digest = hashlib.sha512(PASSPHRASE + salt).digest()
	for _ in range(ROUNDS - 1):
		digest = hashlib.sha512(digest).digest()
	return digest[:32], digest[32:48]


def der(secret, public_key):
	before, after = DER_COMPRESSED if len(public_key) == 33 else DER_UNCOMPRESSED
	return before + secret + after + public_key


def key_record(n, secret, public_key, master):
	"""The daemon's record of a key: ckey in an encrypted wallet, key in a plain one, and the second and third as
	the oldest wallets wrote them, a wkey and a key without its check hash."""
	if master:
		return key("ckey", compact(public_key)), compact(encrypt(master, hash256(public_key)[:16], secret))
	private_key = der(secret, public_key)
	if n == 1:
		return key("wkey", compact(public_key)), struct.pack("<i", WALLET_VERSION) + compact(private_key) \
			+ struct.pack("<qq", CREATED, 0) + compact(b"")
	if n == 2:
		return key("key", compact(public_key)), compact(private_key)
	return key("key", compact(public_key)), compact(private_key) + hash256(public_key + private_key)


def master_key(name):
	"""A wallet's master key and the salt its passphrase is hashed with, both made from the wallet's name."""
	return hashlib.sha256(f"{name}/master".encode()).digest(), hashlib.sha256(f"{name}/salt".encode()).digest()[:8]


def mkey_record(master, salt):
	"""The master key encrypted under the passphrase, with the salt, method 0 (SHA-512) and the rounds."""
	return key("mkey", struct.pack("<I", 1)), compact(encrypt(*passphrase_key(salt), master)) \
		+ compact(salt) + struct.pack("<II", 0, ROUNDS) + compact(b"")


def legacy_records(name, encrypted, used, pool):
	master, salt = master_key(name) if encrypted else (None, None)
	yield key("version"), struct.pack("<i", WALLET_VERSION)
	yield key("minversion"), struct.pack("<i", WALLET_VERSION if encrypted else 60000)
	yield key("defaultkey"), compact(used[0][1])
	yield key("bestblock"), struct.pack("<i", 70930) + b"\x00"
	yield key("orderposnext"), struct.pack("<q", 12)
	for n, (_, public_key) in enumerate(used):
		label = compact(address(public_key).encode())
		yield key("name", label), compact(f"account {n}".encode())
		yield key("purpose", label), compact(b"receive")
	for n, (secret, public_key) in enumerate(used + pool):
		yield key("keymeta", compact(public_key)), struct.pack("<iq", 1, CREATED)
		yield key_record(n, secret, public_key, master)
	for index, (_, public_key) in enumerate(pool, start=1):
		yield key("pool", struct.pack("<q", index)), \
			struct.pack("<iq", WALLET_VERSION, CREATED) + compact(public_key)
	if encrypted:
		yield mkey_record(master, salt)
	friend = compact(b"HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE")
	yield key("name", friend), compact(b"friend")
	yield key("purpose", friend), compact(b"send")
	yield key("watchs", compact(b"\x76\xa9\x14" + bytes(20) + b"\x88\xac")), b"1"
	yield key("cscript", bytes(20)), compact(b"\x51" + compact(used[1][1]) + b"\x51\xae")
	for n in range(40):
		yield key("tx", hashlib.sha256(f"{name}/tx/{n}".encode()).digest()), b"\xee" * (3000 if n % 8 == 0 else 250)


def legacy_wallet(name, encrypted):
	used = [own_key(name, n) for n in range(6)]
	pool = [own_key(name, n) for n in range(6, 106)]
	load(f"{name}.dat", dump(4096, legacy_records(name, encrypted, used, pool)))
	keys = sorted((address(public_key), secret.hex(), len(public_key) == 33) for secret, public_key in used + pool)
	(HERE / f"{name}.addresses").write_text("".join(f"{a}\n" for a, _, _ in keys))
	(HERE / f"{name}.keys").write_text(
		"".join(f"{a} {s} {'compressed' if c else 'uncompressed'}\n" for a, s, c in keys))


def bad_hash_wallet():
	"""A wallet whose one key carries a check hash that is off by a bit, which the daemon would refuse."""
	secret, public_key = own_key("bad-hash", 0)
	record, value = key_record(3, secret, public_key, None)
	load("bad-hash.dat", dump(512, [(record, value[:-1] + bytes([value[-1] ^ 1]))]))


def damaged_key_wallet():
	"""An encrypted wallet whose one key opens under the master key to another key's secret: a damaged wallet, which
	the right passphrase still unlocks."""
	master, salt = master_key("damaged-key")
	_, public_key = own_key("damaged-key", 0)
	other, _ = own_key("damaged-key", 1)
	ckey = key("ckey", compact(public_key)), compact(encrypt(master, hash256(public_key)[:16], other))
	load("damaged-key.dat", dump(512, [ckey, mkey_record(master, salt)]))


def dump(pagesize, entries):
	lines = ["VERSION=3", "format=bytevalue", "database=main", "type=btree",
		f"db_pagesize={pagesize}", "HEADER=END"]
	for k, v in sorted(entries):
		lines += [" " + k.hex(), " " + v.hex()]
	return "\n".join(lines + ["DATA=END", ""])


def load(name, text, *options):
	target = HERE / name
	target.unlink(missing_ok=True)
	subprocess.run(["db4.8_load", *options, str(target)], input=text.encode(), check=True)


load("wallet.dat", dump(512, records()))
load("hash.dat", dump(512, records()).replace("type=btree", "type=hash"))
load("encrypted-file.dat", dump(512, records()), "-P", "secret")
load("short-key.dat", dump(512, records((key("key", compact(b"\x02" * 10)), b""))))
legacy_wallet("plain-wallet", encrypted=False)
legacy_wallet("encrypted-wallet", encrypted=True)
bad_hash_wallet()
damaged_key_wallet()
