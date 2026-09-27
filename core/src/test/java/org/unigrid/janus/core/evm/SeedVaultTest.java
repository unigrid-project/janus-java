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

import java.security.SecureRandom;
import java.util.HexFormat;
import net.jqwik.api.Example;
import org.unigrid.janus.core.evm.Sealed.CipherParams;
import org.unigrid.janus.core.evm.Sealed.KdfParams;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SeedVaultTest {
	static final int CHEAP = 16;

	private static final byte[] SECRET = HexFormat.of().parseHex("00112233445566778899aabbccddeeff");

	private final SeedVault vault = new SeedVault(new SecureRandom(), CHEAP);

	/*
	 * A keystore ethers.js 6 wrote for the key of the Web3 Secret Storage test vector. That vector itself
	 * uses r = 1 with a cost above what RFC 7914 allows for it, which Bouncy Castle refuses.
	 */
	@Example
	public void shouldOpenAKeystoreAnotherEthereumWalletWrote() {
		final Sealed keystore = new Sealed("aes-128-ctr", new CipherParams("6c82e5244e3a035e81d2abbd646957f6"),
			"64ea38662f50d7dd9a3292c719c473f60e5c54c213e407b8cc42b6e2bcd247cc", "scrypt",
			new KdfParams(131072, 8, 1, 32, "ccf7bdf340e17feff3a1ebc0d47f4800e3e9b26707c001fe84e4fa2f8b32fb57"),
			"a75abed2d3eeb9e937ead6b3c71f74aebd72e3b7c508146ee28f1293f406917f"
		);

		assertEquals("7a28b5ba57c53603b0b07b56bba752f7784bf506fa95edc395f5cf6c7514fe9d",
			HexFormat.of().formatHex(vault.open(keystore, "testpassword"))
		);
	}

	@Example
	public void shouldOpenWhatItSealedWithTheSamePassword() {
		assertArrayEquals(SECRET, vault.open(vault.seal(SECRET, "correct horse"), "correct horse"));
	}

	@Example
	public void shouldSealTheSameSecretDifferentlyEachTime() {
		final Sealed first = vault.seal(SECRET, "correct horse");
		final Sealed second = vault.seal(SECRET, "correct horse");

		assertNotEquals(first.kdfparams().salt(), second.kdfparams().salt());
		assertNotEquals(first.ciphertext(), second.ciphertext());
	}

	@Example
	public void shouldRecordTheCostItSealedWith() {
		assertEquals(CHEAP, vault.seal(SECRET, "pw").kdfparams().n());
	}

	@Example
	public void shouldRefuseAnotherPassword() {
		final Sealed sealed = vault.seal(SECRET, "correct horse");

		assertThrows(WrongPassword.class, () -> vault.open(sealed, "battery staple"));
	}

	@Example
	public void shouldRefuseASecretThatWasAltered() {
		final Sealed sealed = vault.seal(SECRET, "correct horse");
		final String flipped = (sealed.ciphertext().charAt(0) == '0' ? "1" : "0") + sealed.ciphertext().substring(1);
		final Sealed altered = new Sealed(sealed.cipher(), sealed.cipherparams(), flipped, sealed.kdf(),
			sealed.kdfparams(), sealed.mac()
		);

		assertThrows(WrongPassword.class, () -> vault.open(altered, "correct horse"));
	}

	@Example
	public void shouldNameACipherItCannotOpen() {
		final Sealed sealed = vault.seal(SECRET, "pw");
		final Sealed other = new Sealed("aes-128-cbc", sealed.cipherparams(), sealed.ciphertext(), "pbkdf2",
			sealed.kdfparams(), sealed.mac()
		);
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> vault.open(other, "pw")
		);

		assertEquals("Only aes-128-ctr under scrypt can be opened, not aes-128-cbc under pbkdf2",
			thrown.getMessage()
		);
	}
}
