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

package org.unigrid.janus.core.legacy;

import java.nio.file.Path;

/** A file that is not a wallet of the kind it was read as, with the reason kept apart from the sentence around it. */
public class UnreadableWallet extends IllegalArgumentException {
	private final String reason;

	public UnreadableWallet(final Path file, final String kind, final String reason, final Throwable cause) {
		super(file + " is not a " + kind + " Janus can read: " + reason, cause);
		this.reason = reason;
	}

	/** What is wrong with the file, as a phrase that carries on after "because". */
	public String reason() {
		return reason;
	}
}
