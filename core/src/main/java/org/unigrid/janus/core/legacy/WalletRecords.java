/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.core.legacy;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The public keys of the wallet's own keys. Every key record carries its public key in the clear, even
 * in an encrypted wallet, so none of this needs the passphrase.
 */
public final class WalletRecords {
	private static final Set<String> KEY_RECORDS = Set.of("key", "wkey", "ckey");
	private static final String POOL_RECORD = "pool";
	private static final int POOL_VERSION_AND_TIME = Integer.BYTES + Long.BYTES;
	private static final Set<Integer> PUBLIC_KEY_SIZES = Set.of(33, 65);

	private static final int SHORT_LENGTH = 0xfd;
	private static final int INT_LENGTH = 0xfe;
	private static final int LONG_LENGTH = 0xff;

	private WalletRecords() {
	}

	public static List<byte[]> publicKeys(final List<BerkeleyFile.Entry> entries) {
		final List<byte[]> keys = new ArrayList<>();

		try {
			for (final BerkeleyFile.Entry entry : entries) {
				final ByteBuffer key = littleEndian(entry.key());
				final String type = new String(lengthPrefixed(key), StandardCharsets.US_ASCII);

				if (KEY_RECORDS.contains(type)) {
					keys.add(publicKey(key));
				} else if (POOL_RECORD.equals(type)) {
					keys.add(publicKey(littleEndian(entry.value()).position(POOL_VERSION_AND_TIME)));
				}
			}
		} catch (BufferUnderflowException e) {
			throw new IllegalArgumentException("a record is cut short", e);
		}

		return keys;
	}

	private static ByteBuffer littleEndian(final byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
	}

	private static byte[] publicKey(final ByteBuffer record) {
		final byte[] key = lengthPrefixed(record);

		if (!PUBLIC_KEY_SIZES.contains(key.length)) {
			throw new IllegalArgumentException("a public key is " + key.length + " bytes");
		}

		return key;
	}

	private static byte[] lengthPrefixed(final ByteBuffer buffer) {
		final int first = Byte.toUnsignedInt(buffer.get());
		final long length = switch (first) {
			case SHORT_LENGTH -> Short.toUnsignedInt(buffer.getShort());
			case INT_LENGTH -> Integer.toUnsignedLong(buffer.getInt());
			case LONG_LENGTH -> buffer.getLong();
			default -> first;
		};

		if (length < 0 || length > buffer.remaining()) {
			throw new BufferUnderflowException();
		}

		final byte[] bytes = new byte[(int) length];

		buffer.get(bytes);
		return bytes;
	}
}
