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
 * A page of the ledger. The first page is the list itself; the pages after it take the place of the
 * button that asked for them, so they arrive without the list around them.
 */
public record RowsView(List<RowView> rows, Integer nextOffset, boolean filtered, boolean continuation) implements View {
	public String moreVals() {
		return "{\"offset\":\"" + nextOffset + "\"}";
	}

	@Override
	public String template() {
		return continuation ? "fragments/activity :: more" : "fragments/activity :: rows";
	}
}
