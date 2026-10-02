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

import org.unigrid.janus.ui.view.DashboardView.FundedPage;
import org.unigrid.janus.web.action.View;

/**
 * The list of funded addresses and its pager without the dialog around them, so that a page or a filter
 * replaces only them and leaves the filter box, and the cursor in it, where they are.
 */
public record FundedBodyView(FundedPage fundedPage) implements View {
	@Override
	public String template() {
		return "fragments/dashboard :: funded-body";
	}
}
