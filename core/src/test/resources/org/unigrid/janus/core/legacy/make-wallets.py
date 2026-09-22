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
btree to grow internal pages, and the large values force overflow pages.
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


def dump(pagesize, *extra):
	lines = ["VERSION=3", "format=bytevalue", "database=main", "type=btree",
		f"db_pagesize={pagesize}", "HEADER=END"]
	for k, v in sorted(records(*extra)):
		lines += [" " + k.hex(), " " + v.hex()]
	return "\n".join(lines + ["DATA=END", ""])


def load(name, text, *options):
	target = HERE / name
	target.unlink(missing_ok=True)
	subprocess.run(["db4.8_load", *options, str(target)], input=text.encode(), check=True)


load("wallet.dat", dump(512))
load("hash.dat", dump(512).replace("type=btree", "type=hash"))
load("encrypted-file.dat", dump(512), "-P", "secret")
load("short-key.dat", dump(512, (key("key", compact(b"\x02" * 10)), b"")))
