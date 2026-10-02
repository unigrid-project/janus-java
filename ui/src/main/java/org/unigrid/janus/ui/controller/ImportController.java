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
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.legacy.BerkeleyFile;
import org.unigrid.janus.core.legacy.LegacyKey;
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

	/* A file that is no Berkeley DB must be a wallet dump, and one that cannot be read as such is said so at
	   once, leaving the earlier choice as it was. */
	private ImportView choose(final Path file) {
		if (Files.isRegularFile(file) && !BerkeleyFile.holds(file)) {
			try {
				WalletDump.keys(file).forEach(LegacyKey::wipe);
			} catch (IllegalArgumentException e) {
				return view(e.getMessage());
			}
		}

		choice.choose(file);
		return view(null);
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
