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

import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** The addresses a legacy wallet.dat holds keys for, read without its passphrase. */
public final class LegacyWallet {
	private LegacyWallet() {
	}

	public static SortedSet<String> addresses(final Path wallet) {
		final List<BerkeleyFile.Entry> entries = BerkeleyFile.read(wallet);

		try {
			return WalletRecords.publicKeys(entries).stream().map(LegacyAddress::of)
				.collect(Collectors.toCollection(TreeSet::new));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(wallet + " is not a wallet.dat Janus can read: " + e.getMessage(),
				e
			);
		}
	}
}
