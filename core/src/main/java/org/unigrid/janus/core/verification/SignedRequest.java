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

package org.unigrid.janus.core.verification;

import java.time.Instant;
import java.util.List;

/**
 * A verification request as it is sent: the ID, whether the holder is a person or a company, when it was
 * signed, and every snapshot address of the wallet with its signature over {@link #text()}.
 */
public record SignedRequest(VerificationId id, Holder holder, Instant signedAt, List<SignedAddress> addresses) {
	private static final String TEXT = "verify %s %d";

	public SignedRequest {
		addresses = List.copyOf(addresses);
	}

	/**
	 * What every address signs. It starts with "verify", where a ledger claim starts with "claim", so neither
	 * signature can stand in for the other; the address is left out, as the signature only checks out under its key.
	 */
	public static String text(final VerificationId id, final Instant signedAt) {
		return TEXT.formatted(id.value(), signedAt.getEpochSecond());
	}

	public String text() {
		return text(id, signedAt);
	}
}
