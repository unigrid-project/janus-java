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

package org.unigrid.janus.core.wallet;

/** How far the wallet's ledger is from being shown, and why it never will be when it failed. */
public record LedgerState(Phase phase, String reason, boolean unreadable, WalletFunds funds) {
	public static final LedgerState IDLE = new LedgerState(Phase.IDLE, null, false, null);
	public static final LedgerState LOADING = new LedgerState(Phase.LOADING, null, false, null);

	public enum Phase {
		IDLE,
		LOADING,
		LOADED,
		FAILED
	}

	public static LedgerState loaded(final WalletFunds funds) {
		return new LedgerState(Phase.LOADED, null, false, funds);
	}

	public static LedgerState failed(final String reason, final boolean unreadable) {
		return new LedgerState(Phase.FAILED, reason, unreadable, null);
	}
}
