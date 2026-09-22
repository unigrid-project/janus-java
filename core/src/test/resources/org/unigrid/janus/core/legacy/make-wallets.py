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

Needs db4.8_load on the PATH. The keys are made up and never held coins. Small pages force the
btree to grow internal pages, and the large values force overflow pages. The plain and encrypted
wallets carry the records the legacy daemon writes (walletdb.cpp), and beside each goes the list of
addresses its keys give, worked out here rather than by the reader under test.
"""

import hashlib
import struct
import subprocess
import sys
from pathlib import Path

HERE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent

UNCOMPRESSED = bytes.fromhex(
	"04b36556d4e6822708431cce73eaf447a0ec89a8ae6eb48aa412cb5b56bb6410"
	"acaa7cda7000e270b9900eb77667bb421728cab77e720c7ca2118150430c4f418a")
COMPRESSED = b"\x02" + UNCOMPRESSED[1:33]
POOLED = b"\x03" + hashlib.sha256(b"pool").digest()
OLD_STYLE = b"\x02" + hashlib.sha256(b"wkey").digest()
ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
CREATED = 1541767134


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


def own_key(name, n):
	seed = hashlib.sha256(f"{name}/{n}".encode()).digest()
	return b"\x04" + seed + hashlib.sha256(seed).digest()


def daemon_records(name, encrypted, used, pool):
	yield key("version"), struct.pack("<i", 61000)
	yield key("minversion"), struct.pack("<i", 61000 if encrypted else 60000)
	yield key("defaultkey"), compact(used[0])
	yield key("bestblock"), struct.pack("<i", 70930) + b"\x00"
	yield key("orderposnext"), struct.pack("<q", 12)
	for n, public_key in enumerate(used):
		label = compact(address(public_key).encode())
		yield key("name", label), compact(f"account {n}".encode())
		yield key("purpose", label), compact(b"receive")
	for public_key in used + pool:
		yield key("keymeta", compact(public_key)), struct.pack("<iq", 1, CREATED)
		if encrypted:
			yield key("ckey", compact(public_key)), compact(hashlib.sha256(public_key).digest() + bytes(16))
		else:
			yield key("key", compact(public_key)), compact(b"\x30" * 279) + hashlib.sha256(public_key).digest()
	for index, public_key in enumerate(pool, start=1):
		yield key("pool", struct.pack("<q", index)), struct.pack("<iq", 61000, CREATED) + compact(public_key)
	if encrypted:
		yield key("mkey", struct.pack("<I", 1)), \
			compact(bytes(48)) + compact(bytes(8)) + struct.pack("<II", 0, 144109) + compact(b"")
	friend = compact(b"HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE")
	yield key("name", friend), compact(b"friend")
	yield key("purpose", friend), compact(b"send")
	yield key("watchs", compact(b"\x76\xa9\x14" + bytes(20) + b"\x88\xac")), b"1"
	yield key("cscript", bytes(20)), compact(b"\x51\x41" + used[1] + b"\x51\xae")
	for n in range(40):
		yield key("tx", hashlib.sha256(f"{name}/tx/{n}".encode()).digest()), b"\xee" * (3000 if n % 8 == 0 else 250)


def daemon_wallet(name, encrypted):
	used = [own_key(name, n) for n in range(6)]
	pool = [own_key(name, n) for n in range(6, 106)]
	load(f"{name}.dat", dump(4096, daemon_records(name, encrypted, used, pool)))
	(HERE / f"{name}.addresses").write_text("".join(f"{a}\n" for a in sorted(map(address, used + pool))))


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
daemon_wallet("plain-wallet", encrypted=False)
daemon_wallet("encrypted-wallet", encrypted=True)
