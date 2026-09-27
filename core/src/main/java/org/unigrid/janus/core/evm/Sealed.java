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

package org.unigrid.janus.core.evm;

/**
 * A secret encrypted the way a Web3 Secret Storage (version 3) keystore encrypts its key, field for field,
 * so the block reads the same as the "crypto" object of a keystore Besu or geth would write.
 */
public record Sealed(String cipher, CipherParams cipherparams, String ciphertext, String kdf, KdfParams kdfparams,
	String mac) {

	public record CipherParams(String iv) { }

	public record KdfParams(int n, int r, int p, int dklen, String salt) { }
}
