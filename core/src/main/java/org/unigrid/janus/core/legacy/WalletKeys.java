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
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.bitcoinj.base.Sha256Hash;

/**
 * The private keys a legacy wallet.dat holds for the addresses asked for. Only their records and the master keys are
 * copied out of the file, so no other secret of the wallet reaches memory, and every copy is wiped once read.
 */
public final class WalletKeys {
	private static final String MASTER_KEY = "mkey";
	private static final String ENCRYPTED_KEY = "ckey";
	private static final String KEY = "key";
	private static final String OLD_KEY = "wkey";
	private static final int COMPRESSED_SIZE = 33;
	private static final int HASH_SIZE = 32;

	/* The key record of an address asked for: the public key it is under, and the private key, in DER when plain. */
	private record KeyRecord(String address, byte[] publicKey, boolean encrypted, byte[] privateKey) {
		boolean compressed() {
			return publicKey.length == COMPRESSED_SIZE;
		}

		boolean matches(final LegacyKey key) {
			return Arrays.equals(publicKey, key.publicKey());
		}
	}

	private WalletKeys() {
	}

	/**
	 * The keys of the addresses, decrypted with the passphrase where the wallet is encrypted; a plain wallet needs none.
	 * The passphrase is wiped once the call ends, whatever the outcome, so that no caller can forget to; the caller
	 * wipes the keys once it has signed with them. An address the wallet holds no key for is refused, as is a
	 * passphrase that does not open every key asked for.
	 */
	public static List<LegacyKey> keys(final Path wallet, final Set<String> addresses, final char[] passphrase) {
		try {
			return read(wallet, addresses, passphrase);
		} finally {
			Arrays.fill(passphrase, '\0');
		}
	}

	private static List<LegacyKey> read(final Path wallet, final Set<String> addresses, final char[] passphrase) {
		final List<BerkeleyFile.Entry> entries = BerkeleyFile.read(wallet, key -> wanted(key, addresses));
		final List<KeyRecord> records = new ArrayList<>();
		final List<LegacyKey> keys = new ArrayList<>();

		try {
			reading(wallet, () -> records(entries, addresses, records));
			requireEvery(wallet, addresses, records);

			for (final KeyRecord record : records) {
				if (!record.encrypted()) {
					keys.add(reading(wallet, () -> plain(record)));
				}
			}

			final List<KeyRecord> encrypted = records.stream().filter(KeyRecord::encrypted).toList();

			if (!encrypted.isEmpty()) {
				final List<MasterKey> masterKeys = reading(wallet, () -> masterKeys(entries));
				keys.addAll(decrypted(wallet, encrypted, masterKeys, passphrase));
			}

			return keys;
		} catch (RuntimeException e) {
			keys.forEach(LegacyKey::wipe);
			throw e;
		} finally {
			entries.forEach(entry -> Arrays.fill(entry.value(), (byte) 0));
			records.forEach(record -> Arrays.fill(record.privateKey(), (byte) 0));
		}
	}

	/* A record whose key cannot be read is copied too, so that reading it below refuses it with the reason. */
	private static boolean wanted(final byte[] key, final Set<String> addresses) {
		try {
			final ByteBuffer record = WalletRecords.littleEndian(key);
			final String type = WalletRecords.type(record);

			return MASTER_KEY.equals(type) || WalletRecords.KEY_RECORDS.contains(type)
				&& addresses.contains(LegacyAddress.of(WalletRecords.publicKey(record)));
		} catch (BufferUnderflowException | IllegalArgumentException _) {
			return true;
		}
	}

	/* Fills the list as it goes, so that the private keys read before a refusal are still wiped. */
	private static List<KeyRecord> records(final List<BerkeleyFile.Entry> entries, final Set<String> addresses,
		final List<KeyRecord> records) {

		for (final BerkeleyFile.Entry entry : entries) {
			final ByteBuffer key = WalletRecords.littleEndian(entry.key());
			final String type = WalletRecords.type(key);

			if (!WalletRecords.KEY_RECORDS.contains(type)) {
				continue;
			}

			final byte[] publicKey = WalletRecords.publicKey(key);
			final String address = LegacyAddress.of(publicKey);

			if (!addresses.contains(address)) {
				continue;
			}

			final ByteBuffer value = WalletRecords.littleEndian(entry.value());

			/* A wkey is the oldest wallets' CWalletKey, whose DER comes after a version. */
			if (OLD_KEY.equals(type)) {
				value.getInt();
			}

			final KeyRecord record = new KeyRecord(address, publicKey, ENCRYPTED_KEY.equals(type),
				WalletRecords.lengthPrefixed(value)
			);

			records.add(record);

			if (KEY.equals(type)) {
				requireCheckHash(record, value);
			}
		}

		return records;
	}

	/* Since the check hash came in, a key carries the double SHA-256 of its public key and DER after the DER, and
	   the daemon refused a key whose hash differs (walletdb.cpp). Older keys have no hash, or one of zeros. */
	private static void requireCheckHash(final KeyRecord record, final ByteBuffer rest) {
		if (rest.remaining() < HASH_SIZE) {
			return;
		}

		final byte[] hash = new byte[HASH_SIZE];

		rest.get(hash);

		if (!Arrays.equals(new byte[HASH_SIZE], hash)
			&& !Arrays.equals(hash, Sha256Hash.hashTwice(record.publicKey(), record.privateKey()))) {

			throw new IllegalArgumentException("the key of " + record.address()
				+ " does not match its check hash");
		}
	}

	/* Asking for an address the wallet has no key for is the caller's mistake, not the wallet's. */
	private static void requireEvery(final Path wallet, final Set<String> addresses, final List<KeyRecord> records) {
		final Set<String> found = Set.copyOf(records.stream().map(KeyRecord::address).toList());
		final List<String> missing = addresses.stream().filter(address -> !found.contains(address))
			.sorted().toList();

		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(wallet + " holds no key for " + String.join(", ", missing));
		}
	}

	/* A plain key is checked against its public key as an encrypted one is, so a damaged one is never used. */
	private static LegacyKey plain(final KeyRecord record) {
		final LegacyKey key = LegacyKey.fromDer(record.privateKey(), record.compressed());

		if (!record.matches(key)) {
			key.wipe();
			throw new IllegalArgumentException("the key of " + record.address()
				+ " does not match its public key");
		}

		return key;
	}

	/* Read only when an encrypted key is asked for, so a plain wallet never needs one. */
	private static List<MasterKey> masterKeys(final List<BerkeleyFile.Entry> entries) {
		final List<MasterKey> masterKeys = entries.stream()
			.filter(entry -> MASTER_KEY.equals(WalletRecords.type(WalletRecords.littleEndian(entry.key()))))
			.map(entry -> MasterKey.parse(entry.value())).toList();

		if (masterKeys.isEmpty()) {
			throw new IllegalArgumentException("it holds encrypted keys but no master key");
		}

		return masterKeys;
	}

	/* The daemon tries every master key in turn until one opens all the keys (wallet.cpp), and so does this. No
	   master key opening means a wrong passphrase. One opening while its keys do not is a damaged wallet, as the
	   passphrase that opened it is right, so the holder is not sent back to type it again. */
	private static List<LegacyKey> decrypted(final Path wallet, final List<KeyRecord> records,
		final List<MasterKey> masterKeys, final char[] passphrase) {

		final byte[] encoded = utf8(passphrase);
		boolean opened = false;

		try {
			for (final MasterKey masterKey : masterKeys) {
				final Optional<byte[]> master = LegacyCrypter.openMasterKey(masterKey, encoded);

				if (master.isPresent()) {
					opened = true;

					final Optional<List<LegacyKey>> keys = unlocked(records, master.get());

					if (keys.isPresent()) {
						return keys.get();
					}
				}
			}

			if (opened) {
				throw new UnreadableWallet(wallet, "wallet.dat", "a key does not open under its master key",
					null
				);
			}

			throw new WrongPassphrase(wallet);
		} finally {
			Arrays.fill(encoded, (byte) 0);
		}
	}

	/* Every key has to open to the secret of its own public key, which the daemon checks too (crypter.cpp): it catches
	   a key that was changed or put under another public key. The master key is wiped either way. */
	private static Optional<List<LegacyKey>> unlocked(final List<KeyRecord> records, final byte[] masterKey) {
		final List<LegacyKey> keys = new ArrayList<>();

		try {
			for (final KeyRecord record : records) {
				final Optional<LegacyKey> key = LegacyCrypter.openSecret(masterKey, record.publicKey(),
					record.privateKey()).flatMap(secret -> matching(record, secret));

				if (key.isEmpty()) {
					keys.forEach(LegacyKey::wipe);
					return Optional.empty();
				}

				keys.add(key.get());
			}

			return Optional.of(keys);
		} finally {
			Arrays.fill(masterKey, (byte) 0);
		}
	}

	/* The key of the secret, if it gives the record's public key; the secret is wiped if it does not. */
	private static Optional<LegacyKey> matching(final KeyRecord record, final byte[] secret) {
		try {
			final LegacyKey key = LegacyKey.of(secret, record.compressed());

			if (record.matches(key)) {
				return Optional.of(key);
			}

			key.wipe();
			return Optional.empty();
		} catch (IllegalArgumentException _) {
			return Optional.empty();
		}
	}

	/* The daemon hashed the passphrase's bytes as they reached it over RPC, which is JSON and so UTF-8. */
	private static byte[] utf8(final char[] passphrase) {
		final ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(passphrase));
		final byte[] bytes = new byte[encoded.remaining()];

		encoded.get(bytes);
		Arrays.fill(encoded.array(), (byte) 0);
		return bytes;
	}

	/* What goes wrong while reading the records is the wallet's fault, and is said as such. */
	private static <T> T reading(final Path wallet, final Supplier<T> read) {
		try {
			return read.get();
		} catch (BufferUnderflowException e) {
			throw new UnreadableWallet(wallet, "wallet.dat", "a record is cut short", e);
		} catch (IllegalArgumentException e) {
			throw new UnreadableWallet(wallet, "wallet.dat", e.getMessage(), e);
		}
	}
}
