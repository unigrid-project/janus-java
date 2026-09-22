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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BerkeleyFileTest {
	private static final String REFUSAL = " is not a wallet.dat Janus can read: ";
	private static final int PAGE_SIZE = 512;
	private static final int MAIN_META_PAGE = 2;

	static Path fixture(final String name) {
		try {
			return Path.of(BerkeleyFileTest.class.getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Path copy(final byte[] content) throws IOException {
		final Path file = Files.createTempFile("wallet", ".dat");

		file.toFile().deleteOnExit();
		Files.write(file, content);
		return file;
	}

	private static void assertRefused(final Path file) {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> BerkeleyFile.read(file)
		);

		assertTrue(thrown.getMessage().startsWith(file + REFUSAL), thrown.getMessage());
	}

	@Example
	public void shouldReadEveryRecordOfTheMainDatabase() {
		assertEquals(311, BerkeleyFile.read(fixture("wallet.dat")).size());
	}

	@Example
	public void shouldFollowOverflowPagesForALargeValue() {
		final byte[] key = {8, 'd', 'e', 's', 't', 'd', 'a', 't', 'a', 3, 'b', 'i', 'g'};
		final List<BerkeleyFile.Entry> entries = BerkeleyFile.read(fixture("wallet.dat"));
		final byte[] value = entries.stream().filter(entry -> Arrays.equals(key, entry.key()))
			.findFirst().orElseThrow().value();
		final byte[] expected = new byte[3003];

		Arrays.fill(expected, (byte) 0xab);
		expected[0] = (byte) 0xfd;
		expected[1] = (byte) 0xb8;
		expected[2] = 0x0b;
		assertArrayEquals(expected, value);
	}

	@Example
	public void shouldRefuseAFileThatIsNotBerkeleyDb() throws IOException {
		assertRefused(copy("not a wallet at all, just some text".repeat(40).getBytes()));
	}

	@Example
	public void shouldRefuseAnEmptyFile() throws IOException {
		assertRefused(copy(new byte[0]));
	}

	@Example
	public void shouldRefuseATruncatedFile() throws IOException {
		final byte[] whole = Files.readAllBytes(fixture("wallet.dat"));

		assertRefused(copy(Arrays.copyOf(whole, whole.length / 2)));
	}

	@Example
	public void shouldRefuseAHashDatabase() {
		assertRefused(fixture("hash.dat"));
	}

	@Example
	public void shouldRefuseAnEncryptedFile() {
		assertRefused(fixture("encrypted-file.dat"));
	}

	@Example
	public void shouldRefuseAPageReachedTwice() throws IOException {
		final byte[] content = Files.readAllBytes(fixture("wallet.dat"));
		final ByteBuffer file = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		final int root = file.getInt(MAIN_META_PAGE * PAGE_SIZE + 88);
		final int firstChild = root * PAGE_SIZE + Short.toUnsignedInt(file.getShort(root * PAGE_SIZE + 26));

		file.putInt(firstChild + 4, root);
		assertRefused(copy(content));
	}

	@Property(tries = 300)
	public void shouldReadOrRefuseADamagedWallet(@ForAll @IntRange(max = 48_127) final int offset,
		@ForAll final byte damage) throws IOException {

		final byte[] content = Files.readAllBytes(fixture("wallet.dat"));

		content[offset] = damage;

		try {
			BerkeleyFile.read(copy(content));
		} catch (IllegalArgumentException refused) {
			assertTrue(refused.getMessage().contains(REFUSAL), refused.getMessage());
		} catch (RuntimeException e) {
			fail("Damage at " + offset + " gave " + e, e);
		}
	}
}
