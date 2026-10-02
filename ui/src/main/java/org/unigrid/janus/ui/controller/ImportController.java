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

package org.unigrid.janus.ui.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.legacy.BerkeleyFile;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.core.legacy.LegacyWallet;
import org.unigrid.janus.core.legacy.UnreadableWallet;
import org.unigrid.janus.core.legacy.WalletDump;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.web.action.Action;
import org.unigrid.janus.web.action.Form;

@ApplicationScoped
public class ImportController {
	private static final String PATH = "path";

	private final DataDirectory directory;
	private final WalletChoice choice;

	@Inject
	public ImportController(final DataDirectory directory, final WalletChoice choice) {
		this.directory = directory;
		this.choice = choice;
	}

	@Action("import")
	public ImportView onClickImport() {
		return view();
	}

	/* A wallet that has gone since the card was drawn is not an error to report; the card drawn
	   again says that nothing was found. */
	@Action("import-found")
	public ImportView onClickUseFound() {
		return directory.wallet().map(this::choose).orElseGet(this::view);
	}

	@Action("import-file")
	public ImportView onChooseFile(final Form form) {
		if (!form.has(PATH)) {
			throw new IllegalArgumentException("No file was named");
		}

		return choose(Path.of(form.get(PATH)));
	}

	/* The file is read as the kind it opens as, and one that cannot be read is said so at once, leaving the
	   earlier choice as it was. Only a Berkeley DB can be a wallet.dat and no Berkeley DB can be a dump, so
	   reading it as the other kind as well could only fail. */
	private ImportView choose(final Path file) {
		if (Files.isRegularFile(file)) {
			final Optional<String> refusal = refusal(file);

			if (refusal.isPresent()) {
				return view(refusal.get());
			}
		}

		choice.choose(file);
		return view(null);
	}

	private static Optional<String> refusal(final Path file) {
		final boolean walletFile = BerkeleyFile.holds(file);

		try {
			if (walletFile) {
				LegacyWallet.addresses(file);
			} else {
				WalletDump.keys(file).forEach(LegacyKey::wipe);
			}

			return Optional.empty();
		} catch (UnreadableWallet e) {
			return Optional.of(walletFile ? e.getMessage() : file + " is neither a wallet.dat nor a wallet dump "
				+ "Janus can read: it does not start like a Berkeley DB file, and as a dump " + e.reason()
			);
		}
	}

	/** The private keys of the wallet dump chosen, for the caller to seal and wipe. */
	public List<LegacyKey> dumpKeys() {
		return WalletDump.keys(choice.chosen().filter(file -> choice.backup().isEmpty())
			.orElseThrow(() -> new IllegalStateException("No wallet dump has been chosen"))
		);
	}

	private ImportView view() {
		return view(null);
	}

	private ImportView view(final String error) {
		return new ImportView(directory.path(), directory.wallet().orElse(null), choice.chosen().orElse(null),
			choice.backup().orElse(null), error
		);
	}
}
