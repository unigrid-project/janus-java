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

package org.unigrid.janus.desktop;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.Arrays;
import java.util.List;

/**
 * A legacy chain bootstrap in Hedgehog's format, holding one address with the history a test chooses and signed
 * with a key the test makes, so the installed wallet can be shown a ledger whose numbers are known. The layout
 * follows Hedgehog's SnapshotWriter: header, address table, block times, entries, transaction ids, signature.
 */
final class MockBootstrap {
	enum Kind { RECEIVED, SENT, MINED, STAKED }

	record Entry(long satoshis, int height, Kind kind) {
	}

	static final List<Entry> HISTORY = List.of(new Entry(102_000_000_000L, 1, Kind.RECEIVED),
		new Entry(-54_000_000_000L, 9, Kind.SENT)
	);

	private static final byte[] MAGIC = "UGDSNAP1".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] SIGNATURE_MAGIC = "UGDSIGN1".getBytes(StandardCharsets.US_ASCII);
	private static final int VERSION = 2;
	private static final int HEADER = 128;
	private static final int VERSION_OFFSET = 8;
	private static final int TIP_HEIGHT_OFFSET = 48;
	private static final int ADDRESS_COUNT_OFFSET = 52;
	private static final int ENTRY_COUNT_OFFSET = 56;
	private static final int TRANSACTION_COUNT_OFFSET = 64;
	private static final int BUILT_AT_OFFSET = 72;
	private static final int ADDRESS_TABLE_OFFSET = 80;
	private static final int BLOCK_TIME_TABLE_OFFSET = 88;
	private static final int ENTRY_TABLE_OFFSET = 96;
	private static final int TRANSACTION_TABLE_OFFSET = 104;
	private static final int TOTAL_UNSPENT_OFFSET = 112;
	private static final int ADDRESS_RECORD = 40;
	private static final int ADDRESS_BALANCE_OFFSET = 20;
	private static final int ADDRESS_ENTRY_COUNT_OFFSET = 32;
	private static final int ENTRY_RECORD = 20;
	private static final int ENTRY_HEIGHT_OFFSET = 8;
	private static final int ENTRY_TRANSACTION_OFFSET = 12;
	private static final int ENTRY_KIND_OFFSET = 16;
	private static final int TRANSACTION_ID = 32;
	private static final int HASH160 = 20;
	private static final int CHECKSUM = 4;
	private static final int COORDINATE_BITS = 521;
	private static final int HEX = 16;
	private static final long GENESIS = 1_500_000_000L;
	private static final int BLOCK_SECONDS = 60;
	private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

	private MockBootstrap() {
	}

	/* Hedgehog splits a public key's hex down the middle into X and Y, so both have to print all 131 digits. */
	static KeyPair keys() throws GeneralSecurityException {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");

		generator.initialize(new ECGenParameterSpec("secp521r1"));

		while (true) {
			final KeyPair keys = generator.generateKeyPair();
			final ECPoint point = ((ECPublicKey) keys.getPublic()).getW();

			if (point.getAffineX().bitLength() == COORDINATE_BITS
				&& point.getAffineY().bitLength() == COORDINATE_BITS) {
				return keys;
			}
		}
	}

	static String publicKey(final KeyPair keys) {
		final ECPoint point = ((ECPublicKey) keys.getPublic()).getW();

		return point.getAffineX().toString(HEX) + point.getAffineY().toString(HEX);
	}

	static void write(final Path file, final String address, final List<Entry> entries, final KeyPair keys)
		throws IOException, GeneralSecurityException {

		final int tip = entries.stream().mapToInt(Entry::height).max().orElse(0);
		final long balance = entries.stream().mapToLong(Entry::satoshis).sum();
		final int blockTimes = HEADER + ADDRESS_RECORD;
		final int entryTable = blockTimes + (tip + 1) * Integer.BYTES;
		final int transactionTable = entryTable + entries.size() * ENTRY_RECORD;
		final ByteBuffer content = ByteBuffer.allocate(transactionTable + entries.size() * TRANSACTION_ID);

		content.put(MAGIC).putInt(VERSION_OFFSET, VERSION).putInt(TIP_HEIGHT_OFFSET, tip)
			.putInt(ADDRESS_COUNT_OFFSET, 1).putLong(ENTRY_COUNT_OFFSET, entries.size())
			.putLong(TRANSACTION_COUNT_OFFSET, entries.size()).putLong(BUILT_AT_OFFSET, timeOf(tip))
			.putLong(ADDRESS_TABLE_OFFSET, HEADER).putLong(BLOCK_TIME_TABLE_OFFSET, blockTimes)
			.putLong(ENTRY_TABLE_OFFSET, entryTable).putLong(TRANSACTION_TABLE_OFFSET, transactionTable)
			.putLong(TOTAL_UNSPENT_OFFSET, balance);
		content.put(HEADER, hash160(address)).putLong(HEADER + ADDRESS_BALANCE_OFFSET, balance)
			.putInt(HEADER + ADDRESS_ENTRY_COUNT_OFFSET, entries.size());

		for (int height = 0; height <= tip; height++) {
			content.putInt(blockTimes + height * Integer.BYTES, (int) timeOf(height));
		}

		for (int i = 0; i < entries.size(); i++) {
			final int entry = entryTable + i * ENTRY_RECORD;
			final byte[] transaction = new byte[TRANSACTION_ID];

			content.putLong(entry, entries.get(i).satoshis())
				.putInt(entry + ENTRY_HEIGHT_OFFSET, entries.get(i).height())
				.putInt(entry + ENTRY_TRANSACTION_OFFSET, i)
				.put(entry + ENTRY_KIND_OFFSET, (byte) entries.get(i).kind().ordinal());
			Arrays.fill(transaction, (byte) (i + 1));
			content.put(transactionTable + i * TRANSACTION_ID, transaction);
		}

		final byte[] signature = sign(content.array(), keys);

		Files.write(file, ByteBuffer.allocate(content.capacity() + SIGNATURE_MAGIC.length + Integer.BYTES
			+ signature.length).put(content.array()).put(SIGNATURE_MAGIC).putInt(signature.length).put(signature)
			.array()
		);
	}

	/* Hedgehog signs a digest of the content rather than the content itself, which can run to gigabytes. */
	private static byte[] sign(final byte[] content, final KeyPair keys) throws GeneralSecurityException {
		final Signature signer = Signature.getInstance("SHA512withECDSA");

		signer.initSign(keys.getPrivate());
		signer.update(MessageDigest.getInstance("SHA-512").digest(content));
		return signer.sign();
	}

	/* An address is Base58Check over a version byte, the hash and a checksum; the hash sits between the two. */
	private static byte[] hash160(final String address) {
		BigInteger value = BigInteger.ZERO;

		for (final char digit : address.toCharArray()) {
			value = value.multiply(BigInteger.valueOf(ALPHABET.length()))
				.add(BigInteger.valueOf(ALPHABET.indexOf(digit)));
		}

		final byte[] raw = value.toByteArray();
		return Arrays.copyOfRange(raw, raw.length - HASH160 - CHECKSUM, raw.length - CHECKSUM);
	}

	private static long timeOf(final int height) {
		return GENESIS + (long) height * BLOCK_SECONDS;
	}
}
