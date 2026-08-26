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

package org.unigrid.janus.web;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Proof that a request came from the interface this process opened, rather than from anything
 * else that happens to be running on the machine and can reach a loopback port.
 */
public final class SessionToken {
	public static final String COOKIE = "janus-session";
	public static final String PARAMETER = "t";

	private static final int LENGTH = 32;

	private final String value;

	private SessionToken(final String value) {
		this.value = value;
	}

	public static SessionToken random() {
		final byte[] bytes = new byte[LENGTH];

		new SecureRandom().nextBytes(bytes);
		return new SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
	}

	public String value() {
		return value;
	}

	public boolean matches(final String candidate) {
		if (candidate == null) {
			return false;
		}

		/* Constant time, so that a caller cannot learn the token one character at a time
		   by measuring how long the comparison takes. */
		return MessageDigest.isEqual(candidate.getBytes(), value.getBytes());
	}
}
