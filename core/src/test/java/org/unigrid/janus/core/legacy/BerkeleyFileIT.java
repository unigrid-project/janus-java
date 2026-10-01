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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Wallet files damaged on disk in one place at a time, each place a check of the reader stands guard over.
 * The positions are those of the fixture wallet: 512-byte pages, the outer meta page first, the meta page
 * of the main database as page 2 with its btree below it, and the large values on overflow pages 83 to 93.
 */
public class BerkeleyFileIT {
	private static final String REFUSAL = " is not a wallet.dat Janus can read: ";
	private static final Predicate<byte[]> EVERY_VALUE = key -> true;
	private static final int PAGE = 512;
	private static final int OUTER_META = 0;
	private static final int OUTER_LEAF = 1;
	private static final int MAIN_META = 2;
	private static final int FIRST_LEAF = 4;
	private static final int OVERFLOW_START = 87;
	private static final int LAST_LINK = 5;
	private static final int MAGIC_AT = 12;
	private static final int VERSION_AT = 16;
	private static final int PAGE_SIZE_AT = 20;
	private static final int FLAGS_AT = 48;
	private static final int ROOT_AT = 88;
	private static final int NEXT_AT = 16;
	private static final int ENTRIES_AT = 20;
	private static final int USED_AT = 22;
	private static final int TYPE_AT = 25;
	private static final int FIRST_ITEM_AT = 26;
	private static final int ITEM_TYPE = 2;
	private static final int ITEM_PAGE = 4;
	private static final int ITEM_LENGTH = 8;
	private static final byte LEAF = 5;
	private static final int OUT_OF_LINE = 3;
	private static final int DELETED = 0x80;

	private Path folder;

	@BeforeTry
	public void makeAFolder() throws IOException {
		folder = Files.createTempDirectory("berkeley");
	}

	@AfterTry
	public void removeIt() throws IOException {
		try (Stream<Path> paths = Files.walk(folder)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	private Path damaged(final Consumer<ByteBuffer> damage) throws IOException {
		final byte[] content = Files.readAllBytes(BerkeleyFileTest.fixture("wallet.dat"));

		damage.accept(ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN));
		return Files.write(folder.resolve("wallet.dat"), content);
	}

	private void assertRefused(final String reason, final Consumer<ByteBuffer> damage) throws IOException {
		final Path file = damaged(damage);
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> BerkeleyFile.read(file, EVERY_VALUE)
		);

		assertEquals(file + REFUSAL + reason, thrown.getMessage());
	}

	private static int itemOf(final ByteBuffer file, final int leaf, final int index) {
		final int start = leaf * PAGE;

		return start + Short.toUnsignedInt(file.getShort(start + FIRST_ITEM_AT + Short.BYTES * index));
	}

	private static int itemPointingAt(final ByteBuffer file, final int overflowPage) {
		return IntStream.range(FIRST_LEAF, OVERFLOW_START).filter(page -> file.get(page * PAGE + TYPE_AT) == LEAF)
			.flatMap(leaf -> IntStream.range(0, file.getShort(leaf * PAGE + ENTRIES_AT))
				.map(index -> itemOf(file, leaf, index)))
			.filter(item -> (file.get(item + ITEM_TYPE) & ~DELETED) == OUT_OF_LINE
				&& file.getInt(item + ITEM_PAGE) == overflowPage)
			.findFirst().orElseThrow(() -> new AssertionError("No item points at page " + overflowPage));
	}

	@Example
	public void shouldRefuseAPathThatHoldsNoFile() {
		final Path missing = folder.resolve("gone.dat");

		final UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
			() -> BerkeleyFile.read(missing, EVERY_VALUE)
		);

		assertEquals("The wallet at " + missing + " could not be read", thrown.getMessage());
	}

	@Example
	public void shouldRefuseAFolderInPlaceOfAFile() {
		final UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
			() -> BerkeleyFile.read(folder, EVERY_VALUE)
		);

		assertEquals("The wallet at " + folder + " could not be read", thrown.getMessage());
	}

	@Example
	public void shouldLetGoOfTheFileSoItCanBeReplacedAndDeleted() throws IOException {
		final Path file = damaged(ByteBuffer::rewind);
		final Path replacement = Files.copy(file, folder.resolve("replacement.dat"));

		assertEquals(311, BerkeleyFile.read(file, EVERY_VALUE).size());
		Files.move(replacement, file, StandardCopyOption.REPLACE_EXISTING);
		assertEquals(311, BerkeleyFile.read(file, EVERY_VALUE).size());
		Files.delete(file);
		assertFalse(Files.exists(file));
	}

	@Example
	public void shouldRefusePagesOfASizeNoBtreeHas() throws IOException {
		for (final int size : new int[] {0, 256, 513, 131_072}) {
			assertRefused("its pages are " + size + " bytes", wallet -> wallet.putInt(PAGE_SIZE_AT, size));
		}
	}

	@Example
	public void shouldRefuseAFileWithoutNamedDatabases() throws IOException {
		assertRefused("it holds no named databases", wallet -> wallet.putInt(FLAGS_AT, 0));
	}

	@Example
	public void shouldRefuseAFileWrittenTheOtherWayRound() throws IOException {
		assertRefused("it is not a Berkeley DB btree",
			wallet -> wallet.putInt(MAGIC_AT, Integer.reverseBytes(wallet.getInt(MAGIC_AT)))
		);
	}

	@Example
	public void shouldRefuseAMasterDatabaseThatNamesNoMainDatabase() throws IOException {
		assertRefused("it has no main database", wallet -> {
			final int key = itemOf(wallet, OUTER_LEAF, 0) + 3;

			assertEquals("main", new String(wallet.array(), key, 4, StandardCharsets.US_ASCII));
			wallet.put(key, (byte) 'x');
		});
	}

	@Example
	public void shouldRefuseABtreeOfAnotherVersion() throws IOException {
		assertRefused("it is btree version 8", wallet -> wallet.putInt(MAIN_META * PAGE + VERSION_AT, 8));
		assertRefused("it is btree version 8", wallet -> wallet.putInt(OUTER_META * PAGE + VERSION_AT, 8));
	}

	@Example
	public void shouldRefuseAMetaPageThatIsNoBtree() throws IOException {
		assertRefused("page 2 is not a btree", wallet -> wallet.putInt(MAIN_META * PAGE + MAGIC_AT, 0));
	}

	@Example
	public void shouldRefuseARootThatIsNotAMetaPage() throws IOException {
		assertRefused("page 2 is not a meta page", wallet -> wallet.put(MAIN_META * PAGE + TYPE_AT, LEAF));
	}

	@Example
	public void shouldRefuseAnOverflowPageReachedAsAPartOfTheTree() throws IOException {
		assertRefused("page 83 is of a kind a wallet does not use",
			wallet -> wallet.putInt(MAIN_META * PAGE + ROOT_AT, 83)
		);
	}

	@Example
	public void shouldRefuseARootBeforeTheFirstPage() throws IOException {
		assertRefused("it points at page -1, past its end", wallet -> wallet.putInt(MAIN_META * PAGE + ROOT_AT, -1));
	}

	@Example
	public void shouldRefuseALeafWithAKeyThatHasNoValue() throws IOException {
		assertRefused("page 4 has a key without a value",
			wallet -> wallet.putShort(FIRST_LEAF * PAGE + ENTRIES_AT, (short) 7)
		);
	}

	@Example
	public void shouldRefuseAnItemOfAKindAWalletDoesNotUse() throws IOException {
		assertRefused("it holds an item of a kind a wallet does not use",
			wallet -> wallet.put(itemOf(wallet, FIRST_LEAF, 0) + ITEM_TYPE, (byte) 2)
		);
	}

	@Example
	public void shouldRefuseAnOverflowItemThatClaimsALengthNoFileHolds() throws IOException {
		for (final int length : new int[] {-1, PAGE * 94 + 1, Integer.MAX_VALUE}) {
			assertRefused("an item claims to be " + length + " bytes",
				wallet -> wallet.putInt(itemPointingAt(wallet, OVERFLOW_START) + ITEM_LENGTH, length)
			);
		}
	}

	@Example
	public void shouldRefuseAnOverflowChainThatLoopsBack() throws IOException {
		assertRefused("page 88 is reached twice",
			wallet -> wallet.putInt((OVERFLOW_START + 2) * PAGE + NEXT_AT, OVERFLOW_START + 1)
		);
	}

	@Example
	public void shouldRefuseAnOverflowChainThatLeadsToAnotherKindOfPage() throws IOException {
		assertRefused("page 89 is not the overflow page an item points at",
			wallet -> wallet.put((OVERFLOW_START + 2) * PAGE + TYPE_AT, LEAF)
		);
	}

	@Example
	public void shouldRefuseAnOverflowPageUsedBeyondItsEnd() throws IOException {
		assertRefused("page 88 is not the overflow page an item points at",
			wallet -> wallet.putShort((OVERFLOW_START + 1) * PAGE + USED_AT, (short) (PAGE - 10))
		);
	}

	@Example
	public void shouldRefuseAChainThatEndsBeforeTheValueDoes() throws IOException {
		assertRefused("page 0 is not the overflow page an item points at",
			wallet -> wallet.putInt((OVERFLOW_START + LAST_LINK) * PAGE + NEXT_AT, 0)
		);
	}

	@Example
	public void shouldRefuseAChainLeadingBeforeTheFirstPage() throws IOException {
		assertRefused("it points at page -1, past its end",
			wallet -> wallet.putInt((OVERFLOW_START + LAST_LINK) * PAGE + NEXT_AT, -1)
		);
	}

	@Example
	public void shouldSkipAnItemMarkedDeletedAndReadTheRest() throws IOException {
		final Path file = damaged(wallet -> {
			final int key = itemOf(wallet, FIRST_LEAF, 0) + ITEM_TYPE;

			wallet.put(key, (byte) (wallet.get(key) | DELETED));
		});

		assertEquals(310, BerkeleyFile.read(file, EVERY_VALUE).size());
	}
}
