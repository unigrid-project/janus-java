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

import java.util.List;
import org.unigrid.janus.web.action.View;

/**
 * Proof that the phrase was written down: its words, shuffled into tiles, tapped back in order. A tile
 * stands for one place in the phrase, so a word the phrase holds twice is two tiles.
 */
public record VerifyView(List<String> picked, List<Tile> tiles, boolean confirmed) implements View {
	public record Tile(int index, String word, boolean used) {
		public String vals() {
			return "{\"index\":\"" + index + "\"}";
		}
	}

	@Override
	public String template() {
		return "fragments/phrase :: verify";
	}

	public String status() {
		return confirmed ? "✓ confirmed" : picked.isEmpty() ? "" : picked.size() + "/" + tiles.size();
	}
}
