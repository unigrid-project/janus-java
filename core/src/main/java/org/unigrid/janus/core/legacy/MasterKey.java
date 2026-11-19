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

import java.nio.ByteBuffer;

/**
 * The mkey record of an encrypted wallet: the master key every private key is encrypted under, itself encrypted
 * under the passphrase, with the salt and number of rounds the passphrase is hashed with. The rounds differ from
 * wallet to wallet, as the daemon timed the computer when the wallet was encrypted.
 */
record MasterKey(byte[] crypted, byte[] salt, int rounds) {
	/* SHA-512, the only method the legacy daemon wrote or could read (crypter.cpp); 1 is a scrypt it never had. */
	private static final int SHA512_METHOD = 0;
	private static final int SALT_SIZE = 8;

	/** Reads the record's value, refusing what the legacy daemon itself would refuse to open. */
	static MasterKey parse(final byte[] value) {
		final ByteBuffer record = WalletRecords.littleEndian(value);
		final byte[] crypted = WalletRecords.lengthPrefixed(record);
		final byte[] salt = WalletRecords.lengthPrefixed(record);
		final int method = record.getInt();
		final long rounds = Integer.toUnsignedLong(record.getInt());

		if (method != SHA512_METHOD) {
			throw new IllegalArgumentException("its keys are encrypted with method "
				+ Integer.toUnsignedString(method) + ", which the legacy wallet never used");
		}

		if (salt.length != SALT_SIZE) {
			throw new IllegalArgumentException("its master key has a salt of " + salt.length + " bytes");
		}

		if (rounds < 1 || rounds > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("its master key asks for " + rounds + " rounds");
		}

		return new MasterKey(crypted, salt, (int) rounds);
	}
}
