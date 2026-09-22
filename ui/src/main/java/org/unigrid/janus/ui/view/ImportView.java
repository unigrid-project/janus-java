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

package org.unigrid.janus.ui.view;

import java.nio.file.Path;
import org.unigrid.janus.web.action.View;

/**
 * The import step: where a wallet was looked for, the one found there and the one settled on.
 * Either of the latter is absent as {@code null}, which is what the template tests for.
 */
public record ImportView(Path directory, Path found, Path chosen) implements View {
	@Override
	public String template() {
		return "fragments/import :: import";
	}

	public boolean hasFound() {
		return found != null;
	}

	public boolean hasChosen() {
		return chosen != null;
	}

	public boolean foundChosen() {
		return hasFound() && found.equals(chosen);
	}

	public boolean otherChosen() {
		return hasChosen() && !chosen.equals(found);
	}

	/** The choice put forward until one has been made: the found wallet, or else picking a file. */
	public boolean recommendsFound() {
		return hasFound() && !hasChosen();
	}

	public boolean recommendsFile() {
		return !hasFound() && !hasChosen();
	}
}
