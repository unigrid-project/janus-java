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

import java.math.BigDecimal;

/** An amount the mint storage spork promises an address at a height of the chain that mints. */
public record Mint(String address, int height, BigDecimal amount) {
	/* The spork keys each mint as "address/height", and only the height can never hold a slash. */
	static Mint of(final String location, final BigDecimal amount) {
		final int slash = location.lastIndexOf('/');

		if (slash < 0) {
			throw new IllegalArgumentException("\"" + location + "\" is not an address and a height");
		}

		return new Mint(location.substring(0, slash), Integer.parseInt(location.substring(slash + 1)), amount);
	}
}
