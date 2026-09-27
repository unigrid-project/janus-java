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

/** Every transaction of the wallet, narrowed by kind and by what was searched for. */
public record ActivityView(Filter filter, String query, List<Summary> summaries, RowsView rows, ExportNoteView note)
	implements View {

	/** One all-time figure, which narrows the list to its kind when pressed. */
	public record Summary(Filter filter, String label, String value, String kind) {
	}

	public List<Filter> filters() {
		return List.of(Filter.values());
	}

	@Override
	public String template() {
		return "fragments/activity :: activity";
	}
}
