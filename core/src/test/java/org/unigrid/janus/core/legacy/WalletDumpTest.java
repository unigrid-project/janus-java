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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletDumpTest {
	private static final String HEADER = "# Wallet dump created by UNIGRID 2.9.17 (2019-03-14 09:26:53 +0100)";
	private static final String UNCOMPRESSED_TWO = "68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J";
	private static final String COMPRESSED_TWO = "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx";
	private static final String TESTNET_TWO = "cMahea7zqjxrtgAbB7LSGbcQUr1uX1ojuat9jZodMN87K7XCyj5v";
	private static final String COMPRESSED_ADDRESS = "H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH";
	private static final String UNCOMPRESSED_ADDRESS = "HS6ofefYfBjXjqaKM4pb54a1SEmAxvGKTi";
	private static final String REFUSAL = " is not a wallet dump Janus can read: ";

	private static Path dump(final String... lines) throws IOException {
		final Path file = Files.createTempFile("wallet", ".dump");

		file.toFile().deleteOnExit();
		Files.write(file, String.join("\r\n", lines).getBytes(StandardCharsets.ISO_8859_1));
		return file;
	}

	private static Set<String> expected() throws IOException {
		return Set.copyOf(Files.readAllLines(BerkeleyFileTest.fixture("wallet.dump.addresses")));
	}

	@Example
	public void shouldFindTheAddressOfEveryKeyTheDumpHolds() throws IOException {
		assertEquals(expected(), WalletDump.addresses(BerkeleyFileTest.fixture("wallet.dump")));
	}

	@Example
	public void shouldGiveACompressedKeyTheAddressOfItsCompressedForm() throws IOException {
		assertEquals(Set.of(COMPRESSED_ADDRESS),
			WalletDump.addresses(dump(HEADER, COMPRESSED_TWO + " 2018-01-02T10:00:00Z label="))
		);
	}

	@Example
	public void shouldGiveAnUncompressedKeyTheAddressOfItsUncompressedForm() throws IOException {
		final Path file = dump(HEADER, UNCOMPRESSED_TWO + " 2018-01-02T10:00:00Z reserve=1 # addr=ignored");

		assertEquals(Set.of(UNCOMPRESSED_ADDRESS), WalletDump.addresses(file));
	}

	@Example
	public void shouldReadAKeyWhateverItsMark() throws IOException {
		final Path file = dump(HEADER, COMPRESSED_TWO + " 2018-01-02T10:00:00Z change=1",
			UNCOMPRESSED_TWO + " 2018-01-02T10:00:00Z"
		);

		assertEquals(Set.of(COMPRESSED_ADDRESS, UNCOMPRESSED_ADDRESS),
			WalletDump.addresses(file)
		);
	}

	@Example
	public void shouldReadADumpWrittenOnWindows() throws IOException {
		final String text = Files.readString(BerkeleyFileTest.fixture("wallet.dump"), StandardCharsets.ISO_8859_1);
		final Path file = Files.createTempFile("windows", ".dump");

		file.toFile().deleteOnExit();
		Files.writeString(file, text.replace("\n", "\r\n"), StandardCharsets.ISO_8859_1);
		assertEquals(expected(), WalletDump.addresses(file));
	}

	@Example
	public void shouldRecogniseADumpByItsFirstLine() throws IOException {
		assertTrue(WalletDump.holds(BerkeleyFileTest.fixture("wallet.dump")));
		assertFalse(WalletDump.holds(BerkeleyFileTest.fixture("plain-wallet.dat")));
		assertFalse(WalletDump.holds(dump("# A comment of some other file")));
		assertFalse(WalletDump.holds(dump()));
	}

	@Example
	public void shouldRefuseAKeyOfAnotherNetwork() throws IOException {
		final Path file = dump(HEADER, "", COMPRESSED_TWO + " 2018-01-02T10:00:00Z label=", TESTNET_TWO + " 2018");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> WalletDump.addresses(file)
		);

		assertEquals(file + REFUSAL + "line 4 holds no private key", thrown.getMessage());
	}

	@Example
	public void shouldRefuseAKeyWhoseChecksumIsWrongWithoutRepeatingIt() throws IOException {
		final String damaged = COMPRESSED_TWO.substring(0, COMPRESSED_TWO.length() - 1) + "h";
		final Path file = dump(HEADER, damaged + " 2018-01-02T10:00:00Z label=");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> WalletDump.addresses(file)
		);

		assertEquals(file + REFUSAL + "line 2 holds no private key", thrown.getMessage());
	}

	@Example
	public void shouldRefuseAKeyOfTheWrongLength() throws IOException {
		final Path file = dump(HEADER, "3QJmnh 2018-01-02T10:00:00Z label=");

		assertThrows(IllegalArgumentException.class, () -> WalletDump.addresses(file));
	}

	@Example
	public void shouldRefuseADumpWithoutKeys() throws IOException {
		final Path file = dump(HEADER, "", "# End of dump");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> WalletDump.addresses(file)
		);

		assertEquals(file + REFUSAL + "it holds no private keys", thrown.getMessage());
	}
}
