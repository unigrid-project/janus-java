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

package org.unigrid.janus.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The wallet file the user has settled on, kept until the daemon is started with it. A wallet only
 * counts as chosen once a copy of it has been made.
 */
@ApplicationScoped
public class WalletChoice {
	private record Chosen(Path wallet, Path backup) { }

	private final WalletBackup backups;
	private volatile Chosen chosen;

	@Inject
	public WalletChoice(final WalletBackup backups) {
		this.backups = backups;
	}

	public synchronized void choose(final Path wallet) {
		if (!Files.isRegularFile(wallet)) {
			throw new IllegalArgumentException("There is no wallet file at " + wallet);
		}

		if (!chosen().equals(Optional.of(wallet))) {
			chosen = new Chosen(wallet, backups.backup(wallet));
		}
	}

	public Optional<Path> chosen() {
		return Optional.ofNullable(chosen).map(Chosen::wallet);
	}

	/** Where the copy of the chosen wallet was put. */
	public Optional<Path> backup() {
		return Optional.ofNullable(chosen).map(Chosen::backup);
	}
}
