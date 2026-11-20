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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.jqwik.api.Example;
import org.bouncycastle.util.BigIntegers;
import org.web3j.crypto.Sign;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LegacySignerTest {
	private static final String TEXT = "verify 00112233445566778899aabbccddeeff PERSON 1763500000";
	private static final String COMMENT = "# ";
	private static final int COORDINATE = 32;
	private static final int R_END = 1 + COORDINATE;

	/* The keys make-wallets.py wrote beside plain-wallet, by address. */
	private static Map<String, LegacyKey> keys() throws IOException {
		final Map<String, LegacyKey> keys = new TreeMap<>();

		for (final String line : Files.readAllLines(BerkeleyFileTest.fixture("plain-wallet.keys"))) {
			final String[] fields = line.split(" ");
			final boolean compressed = "compressed".equals(fields[2]);

			keys.put(fields[0], new LegacyKey(HexFormat.of().parseHex(fields[1]), compressed));
		}

		return keys;
	}

	/* What make-signatures.py signed: its text on the first line, then an address and its signature on each. */
	private static List<String[]> signatures() throws IOException {
		final List<String> lines = Files.readAllLines(BerkeleyFileTest.fixture("verify-signatures.txt"));

		assertEquals(COMMENT + TEXT, lines.get(0));
		return lines.subList(1, lines.size()).stream().map(line -> line.split(" ")).toList();
	}

	private static byte[] withS(final byte[] signature, final BigInteger s) {
		final byte[] copy = signature.clone();

		System.arraycopy(BigIntegers.asUnsignedByteArray(COORDINATE, s), 0, copy, R_END, COORDINATE);
		return copy;
	}

	/* Made by plain Python apart from this code, so a fault in how the hash or the header is built fails here. */
	@Example
	public void shouldSignAsAnIndependentSignerDoesByteForByte() throws IOException {
		final Map<String, LegacyKey> keys = keys();

		for (final String[] signed : signatures()) {
			final byte[] signature = LegacySigner.sign(keys.get(signed[0]), TEXT);

			assertEquals(signed[1], Base64.getEncoder().encodeToString(signature), signed[0]);
		}
	}

	@Example
	public void shouldGiveTheAddressOfTheKeyThatSignedForBothEncodings() throws IOException {
		final Map<String, LegacyKey> keys = keys();

		for (final String[] signed : signatures()) {
			final LegacyKey key = keys.get(signed[0]);

			assertEquals(Optional.of(key.address()), LegacySigner.addressOf(TEXT, LegacySigner.sign(key, TEXT)));
		}
	}

	/* Made by the signing code of the legacy wallet itself: the claim of an account on a chain, signed by both
	   encodings of one key. */
	@Example
	public void shouldReadTheSignaturesTheLegacyWalletMade() {
		final String text = "claim def0b9a3cdc4f37149f7b4616748a5d428fed991fb5ed33ddbeffb3af5376cca"
			+ "11a14e7aab4d6374ed195ed549a1279c8a3afd910bc61e7fabf6e8b046a3aa27 "
			+ "e468f1df4a0267c7dc5e0b8869c51820ff1db5204cc7d1aa64cba7f2b79b5e7a";
		final Map<String, String> signatures = Map.of(
			"02ffce729dcedd30046c63f3284feea462650ef2ebc5cff62570ad3aecdf934881",
			"HzPXLYr6vriQLWK92mt3wWdIaO+nfUy8IbAayYaOjIIlXUzGCy9B4WRKObBsAUSgnCLBSRETaZBR9lzlOEzgP7s=",
			"04ffce729dcedd30046c63f3284feea462650ef2ebc5cff62570ad3aecdf934881"
				+ "94a63953ef2268ab06d825b6804bd1e1ac25030450d4ee323cfdfeb921f006d0",
			"GzPXLYr6vriQLWK92mt3wWdIaO+nfUy8IbAayYaOjIIlXUzGCy9B4WRKObBsAUSgnCLBSRETaZBR9lzlOEzgP7s=");

		signatures.forEach((key, base64) -> assertEquals(
			Optional.of(LegacyAddress.of(HexFormat.of().parseHex(key))),
			LegacySigner.addressOf(text, Base64.getDecoder().decode(base64)), key
		));
	}

	@Example
	public void shouldSignTheSameTextTheSameWay() throws IOException {
		final LegacyKey key = keys().values().iterator().next();

		assertArrayEquals(LegacySigner.sign(key, TEXT), LegacySigner.sign(key, TEXT));
	}

	@Example
	public void shouldGiveAnotherAddressForAnotherText() throws IOException {
		final LegacyKey key = keys().values().iterator().next();
		final byte[] signature = LegacySigner.sign(key, TEXT);

		assertNotEquals(Optional.of(key.address()), LegacySigner.addressOf(TEXT + "0", signature));
	}

	@Example
	public void shouldGiveNoAddressForWhatTheWalletCouldNotHaveMade() throws IOException {
		final byte[] signature = LegacySigner.sign(keys().values().iterator().next(), TEXT);
		final BigInteger n = Sign.CURVE_PARAMS.getN();
		final BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, R_END, LegacySigner.SIZE));
		final byte[] header26 = signature.clone();
		final byte[] header35 = signature.clone();

		header26[0] = 26;
		header35[0] = 35;

		for (final byte[] bad : List.of(Arrays.copyOf(signature, LegacySigner.SIZE - 1),
			Arrays.copyOf(signature, LegacySigner.SIZE + 1), header26, header35, withS(signature, n.subtract(s)),
			withS(signature, BigInteger.ZERO), withS(signature, n))) {

			assertEquals(Optional.empty(), LegacySigner.addressOf(TEXT, bad));
		}
	}

	@Example
	public void shouldRefuseATextTheWalletCouldNotSign() throws IOException {
		final LegacyKey key = keys().values().iterator().next();
		final byte[] signature = LegacySigner.sign(key, TEXT);

		assertThrows(IllegalArgumentException.class, () -> LegacySigner.sign(key, "café"));
		assertThrows(IllegalArgumentException.class, () -> LegacySigner.sign(key, "x".repeat(253)));
		assertEquals(Optional.empty(), LegacySigner.addressOf("café", signature));
	}
}
