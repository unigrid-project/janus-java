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
import java.nio.file.Path;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletChoice;
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
		directory.wallet().ifPresent(choice::choose);
		return view();
	}

	@Action("import-file")
	public ImportView onChooseFile(final Form form) {
		if (!form.has(PATH)) {
			throw new IllegalArgumentException("No file was named");
		}

		choice.choose(Path.of(form.get(PATH)));
		return view();
	}

	private ImportView view() {
		return new ImportView(directory.path(), directory.wallet().orElse(null), choice.chosen().orElse(null));
	}
}
