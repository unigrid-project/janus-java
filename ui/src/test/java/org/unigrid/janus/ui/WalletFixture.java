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

package org.unigrid.janus.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The legacy wallet the rigs hand out, from the fixtures of core, and the addresses it holds keys for. */
final class WalletFixture {
	private static final String FIXTURE = "/org/unigrid/janus/core/legacy/plain-wallet";
	private static final String DUMP = "/org/unigrid/janus/core/legacy/wallet.dump";

	private WalletFixture() {
	}

	static Path copyTo(final Path to) throws IOException {
		return copy(FIXTURE + ".dat", to);
	}

	static List<String> addresses() throws IOException {
		return lines(FIXTURE + ".addresses");
	}

	/** The same kind of wallet as a dumpwallet text file, with keys of its own. */
	static Path copyDumpTo(final Path to) throws IOException {
		return copy(DUMP, to);
	}

	static List<String> dumpAddresses() throws IOException {
		return lines(DUMP + ".addresses");
	}

	private static Path copy(final String resource, final Path to) throws IOException {
		try (InputStream in = WalletFixture.class.getResourceAsStream(resource)) {
			Files.copy(in, to);
			return to;
		}
	}

	private static List<String> lines(final String resource) throws IOException {
		try (InputStream in = WalletFixture.class.getResourceAsStream(resource)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		}
	}
}
