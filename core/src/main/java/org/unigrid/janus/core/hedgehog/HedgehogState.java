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

package org.unigrid.janus.core.hedgehog;

/** How far Hedgehog is from answering about the ledger, and why not when it never will. */
public record HedgehogState(Phase phase, String reason, SnapshotInfo snapshot) {
	public static final HedgehogState IDLE = new HedgehogState(Phase.IDLE, null, null);
	public static final HedgehogState STARTING = new HedgehogState(Phase.STARTING, null, null);
	public static final HedgehogState FETCHING = new HedgehogState(Phase.FETCHING, null, null);

	public enum Phase {
		IDLE,
		STARTING,
		FETCHING,
		READY,
		FAILED
	}

	public static HedgehogState failed(final String reason) {
		return new HedgehogState(Phase.FAILED, reason, null);
	}

	public static HedgehogState ready(final SnapshotInfo snapshot) {
		return new HedgehogState(Phase.READY, null, snapshot);
	}
}
