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

import org.unigrid.janus.web.action.View;

/**
 * The wallet's own window: its tabs, the state of the ledger and the screen being shown. It is drawn and
 * swapped as a whole, so the tabs always agree with the screen. While the ledger is being prepared it
 * asks for itself again every two seconds. Only a screen that has just arrived moves in.
 */
public record AppView(Tab tab, String chip, boolean preparing, boolean entering, View screen) implements View {
	public enum Tab {
		DASHBOARD,
		ACTIVITY
	}

	public boolean on(final String name) {
		return tab.name().equals(name);
	}

	@Override
	public String template() {
		return "fragments/app :: app";
	}
}
