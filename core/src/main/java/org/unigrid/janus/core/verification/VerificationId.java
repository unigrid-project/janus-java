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

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * The ID of one verification request: 128 random bits as 32 lower-case hex characters. It is made apart from the
 * addresses, so it says nothing of them, and every address of the request signs it.
 */
public record VerificationId(String value) {
	private static final int BYTES = 16;
	private static final int LENGTH = 2 * BYTES;
	private static final Pattern FORM = Pattern.compile("[0-9a-f]{%d}".formatted(LENGTH));
	private static final String MALFORMED = "A verification ID is %d lower-case hex characters";

	public VerificationId {
		if (value == null || !FORM.matcher(value).matches()) {
			throw new IllegalArgumentException(MALFORMED.formatted(LENGTH));
		}
	}

	public static VerificationId random(final SecureRandom random) {
		final byte[] bytes = new byte[BYTES];

		random.nextBytes(bytes);
		return new VerificationId(HexFormat.of().formatHex(bytes));
	}

	@Override
	public String toString() {
		return value;
	}
}
