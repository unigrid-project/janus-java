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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletRecordsTest {
	private static final byte[] COMPRESSED = filled(33, 2);
	private static final byte[] UNCOMPRESSED = filled(65, 4);

	private static byte[] filled(final int size, final int first) {
		final byte[] key = new byte[size];

		Arrays.fill(key, (byte) 0x11);
		key[0] = (byte) first;
		return key;
	}

	private static byte[] compact(final byte[] data) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		out.write(data.length);
		out.writeBytes(data);
		return out.toByteArray();
	}

	private static byte[] join(final byte[]... parts) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		Arrays.stream(parts).forEach(out::writeBytes);
		return out.toByteArray();
	}

	private static BerkeleyFile.Entry record(final String type, final byte[] rest, final byte[] value) {
		return new BerkeleyFile.Entry(join(compact(type.getBytes(StandardCharsets.US_ASCII)), rest), value);
	}

	private static BerkeleyFile.Entry pool(final byte[] publicKey) {
		final byte[] header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(1010000)
			.putLong(1_500_000_000L).array();

		return record("pool", new byte[8], join(header, compact(publicKey)));
	}

	@Example
	public void shouldTakeThePublicKeyOfEveryKindOfOwnKey() {
		final List<byte[]> keys = WalletRecords.publicKeys(List.of(
			record("key", compact(UNCOMPRESSED), new byte[] {1}),
			record("wkey", compact(COMPRESSED), new byte[] {1}),
			record("ckey", compact(COMPRESSED), new byte[] {1}),
			pool(UNCOMPRESSED)
		));

		assertEquals(4, keys.size());
		assertArrayEquals(UNCOMPRESSED, keys.get(0));
		assertArrayEquals(COMPRESSED, keys.get(1));
		assertArrayEquals(COMPRESSED, keys.get(2));
		assertArrayEquals(UNCOMPRESSED, keys.get(3));
	}

	@Example
	public void shouldNeedTheValueOfAKeypoolRecordAlone() {
		assertTrue(WalletRecords.needsValue(pool(COMPRESSED).key()));
		assertFalse(WalletRecords.needsValue(record("key", compact(UNCOMPRESSED), new byte[0]).key()));
		assertFalse(WalletRecords.needsValue(record("ckey", compact(COMPRESSED), new byte[0]).key()));
		assertFalse(WalletRecords.needsValue(record("wkey", compact(COMPRESSED), new byte[0]).key()));
		assertFalse(WalletRecords.needsValue(record("mkey", new byte[4], new byte[0]).key()));
	}

	@Example
	public void shouldIgnoreRecordsThatHoldNoOwnKey() {
		assertTrue(WalletRecords.publicKeys(List.of(
			record("name", compact("HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE".getBytes()), new byte[] {0}),
			record("keymeta", compact(COMPRESSED), new byte[12]),
			record("watchs", compact(new byte[25]), new byte[] {1}),
			record("cscript", new byte[20], new byte[] {1}),
			record("version", new byte[0], new byte[4])
		)).isEmpty());
	}

	@Example
	public void shouldReadALengthWrittenInItsLongForm() {
		final byte[] longForm = join(new byte[] {(byte) 0xfd, 33, 0}, COMPRESSED);

		final List<byte[]> keys = WalletRecords.publicKeys(List.of(record("key", longForm, new byte[0])));

		assertArrayEquals(COMPRESSED, keys.get(0));
	}

	@Example
	public void shouldRefuseAKeyOfTheWrongSize() {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> WalletRecords.publicKeys(List.of(record("key", compact(new byte[10]), new byte[0])))
		);

		assertEquals("a public key is 10 bytes", thrown.getMessage());
	}

	@Example
	public void shouldRefuseARecordCutShort() {
		final byte[] cut = Arrays.copyOf(compact(COMPRESSED), 12);

		assertThrows(IllegalArgumentException.class,
			() -> WalletRecords.publicKeys(List.of(record("ckey", cut, new byte[0])))
		);
	}
}
