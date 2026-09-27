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

/** Which kinds of transaction the Activity shows; rewards are what mining and staking earned. */
public enum Filter {
	ALL("All"),
	RECEIVED("Received"),
	SENT("Sent"),
	REWARDS("Rewards");

	private final String label;

	Filter(final String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** The values a control sends to show this kind, as htmx takes them. */
	public String vals() {
		return "{\"filter\":\"" + name() + "\"}";
	}
}
