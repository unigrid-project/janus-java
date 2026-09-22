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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * The key and value pairs in the {@code main} database of a Berkeley DB 4.8 btree file, the format the
 * legacy daemon kept its wallet in. It only ever reads, and refuses what it does not understand rather
 * than guess.
 */
public final class BerkeleyFile {
	private static final int MAGIC = 0x053162;
	private static final int VERSION = 9;
	private static final int NAMED_DATABASES = 0x20;
	private static final int MINIMUM_PAGE_SIZE = 512;
	private static final int MAXIMUM_PAGE_SIZE = 65_536;
	private static final byte[] MAIN = "main".getBytes(StandardCharsets.US_ASCII);

	private static final int META_MAGIC = 12;
	private static final int META_VERSION = 16;
	private static final int META_PAGE_SIZE = 20;
	private static final int META_ENCRYPTION = 24;
	private static final int META_FLAGS = 48;
	private static final int META_ROOT = 88;

	private static final int PAGE_NEXT = 16;
	private static final int PAGE_ENTRIES = 20;
	private static final int PAGE_USED = 22;
	private static final int PAGE_TYPE = 25;
	private static final int PAGE_HEADER = 26;

	private static final byte INTERNAL = 3;
	private static final byte LEAF = 5;
	private static final byte OVERFLOW = 7;
	private static final byte META = 9;

	private static final int ITEM_TYPE = 2;
	private static final int ITEM_DATA = 3;
	private static final int ITEM_PAGE = 4;
	private static final int ITEM_LENGTH = 8;
	private static final int INLINE = 1;
	private static final int OUT_OF_LINE = 3;
	private static final int DELETED = 0x80;
	private static final int OUT_OF_LINE_ITEM_SIZE = 12;
	private static final int INTERNAL_ITEM_HEADER = 12;

	public record Entry(byte[] key, byte[] value) {
	}

	private final Path path;
	private final ByteBuffer file;
	private final BitSet visited = new BitSet();
	private final int pageSize;

	private BerkeleyFile(final Path path, final ByteBuffer file) {
		this.path = path;
		this.file = file;

		if (file.getInt(META_MAGIC) != MAGIC) {
			throw refusal("it is not a Berkeley DB btree");
		}

		pageSize = file.getInt(META_PAGE_SIZE);

		if (Integer.bitCount(pageSize) != 1 || pageSize < MINIMUM_PAGE_SIZE || pageSize > MAXIMUM_PAGE_SIZE) {
			throw refusal("its pages are " + pageSize + " bytes");
		}

		if ((file.getInt(META_FLAGS) & NAMED_DATABASES) == 0) {
			throw refusal("it holds no named databases");
		}
	}

	/* A damaged file points past its own end sooner or later, and the buffer says so by throwing. */
	public static List<Entry> read(final Path path) {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			final ByteBuffer file = channel.map(MapMode.READ_ONLY, 0, channel.size())
				.order(ByteOrder.LITTLE_ENDIAN);

			return new BerkeleyFile(path, file).main();
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			throw new IllegalArgumentException(path + " is not a wallet.dat Janus can read: it is cut short", e);
		} catch (IOException e) {
			throw new UncheckedIOException("The wallet at " + path + " could not be read", e);
		}
	}

	private List<Entry> main() {
		final Entry main = entries(root(0)).stream().filter(entry -> Arrays.equals(MAIN, entry.key()))
			.findFirst().orElseThrow(() -> refusal("it has no main database"));

		return entries(root(ByteBuffer.wrap(main.value()).getInt()));
	}

	private int root(final int page) {
		final int start = offset(page);

		if (file.getInt(start + META_MAGIC) != MAGIC) {
			throw refusal("page " + page + " is not a btree");
		}

		if (file.getInt(start + META_VERSION) != VERSION) {
			throw refusal("it is btree version " + file.getInt(start + META_VERSION));
		}

		if (file.get(start + META_ENCRYPTION) != 0) {
			throw refusal("the file itself is encrypted");
		}

		if (file.get(start + PAGE_TYPE) != META) {
			throw refusal("page " + page + " is not a meta page");
		}

		return file.getInt(start + META_ROOT);
	}

	private List<Entry> entries(final int root) {
		final List<Entry> entries = new ArrayList<>();
		final Deque<Integer> pending = new ArrayDeque<>(List.of(root));

		while (!pending.isEmpty()) {
			final int page = pending.pop();
			final int start = visit(page);

			switch (file.get(start + PAGE_TYPE)) {
				case INTERNAL -> items(page, start, this::internalExtent)
					.forEach(item -> pending.push(file.getInt(item + ITEM_PAGE)));
				case LEAF -> entries.addAll(pairs(page, start));
				default -> throw refusal("page " + page + " is of a kind a wallet does not use");
			}
		}

		return entries;
	}

	private List<Entry> pairs(final int page, final int start) {
		final List<Integer> items = items(page, start, this::leafExtent);
		final List<Entry> pairs = new ArrayList<>();

		if (items.size() % 2 != 0) {
			throw refusal("page " + page + " has a key without a value");
		}

		for (int i = 0; i < items.size(); i += 2) {
			final int key = items.get(i);
			final int value = items.get(i + 1);

			if (!deleted(key) && !deleted(value)) {
				pairs.add(new Entry(data(key), data(value)));
			}
		}

		return pairs;
	}

	/* Items lie side by side between a page's index and its end. Holding a hostile file to that keeps it
	   from having one large item copied once for every entry that points at it. */
	private List<Integer> items(final int page, final int start, final IntUnaryOperator extent) {
		final int count = Short.toUnsignedInt(file.getShort(start + PAGE_ENTRIES));
		final int indexEnd = PAGE_HEADER + Short.BYTES * count;

		if (indexEnd > pageSize) {
			throw refusal("page " + page + " claims " + count + " entries");
		}

		final int[] items = IntStream.range(0, count)
			.map(i -> Short.toUnsignedInt(file.getShort(start + PAGE_HEADER + Short.BYTES * i))).toArray();
		int end = indexEnd;

		for (final int item : IntStream.of(items).sorted().toArray()) {
			if (item < end) {
				throw refusal("page " + page + " has items that overlap");
			}

			end = item + extent.applyAsInt(start + item);
		}

		if (end > pageSize) {
			throw refusal("page " + page + " has an item reaching past its end");
		}

		return IntStream.of(items).mapToObj(item -> start + item).toList();
	}

	private int leafExtent(final int item) {
		return switch (Byte.toUnsignedInt(file.get(item + ITEM_TYPE)) & ~DELETED) {
			case INLINE -> ITEM_DATA + Short.toUnsignedInt(file.getShort(item));
			case OUT_OF_LINE -> OUT_OF_LINE_ITEM_SIZE;
			default -> throw refusal("it holds an item of a kind a wallet does not use");
		};
	}

	private int internalExtent(final int item) {
		return INTERNAL_ITEM_HEADER + Short.toUnsignedInt(file.getShort(item));
	}

	private boolean deleted(final int item) {
		return (Byte.toUnsignedInt(file.get(item + ITEM_TYPE)) & DELETED) != 0;
	}

	private byte[] data(final int item) {
		return switch (Byte.toUnsignedInt(file.get(item + ITEM_TYPE)) & ~DELETED) {
			case INLINE -> bytes(item + ITEM_DATA, Short.toUnsignedInt(file.getShort(item)));
			case OUT_OF_LINE -> overflow(file.getInt(item + ITEM_PAGE), file.getInt(item + ITEM_LENGTH));
			default -> throw refusal("it holds an item of a kind a wallet does not use");
		};
	}

	private byte[] overflow(final int first, final int length) {
		if (length < 0 || length > file.limit()) {
			throw refusal("an item claims to be " + length + " bytes");
		}

		final ByteBuffer data = ByteBuffer.allocate(length);

		for (int page = first; data.hasRemaining(); page = file.getInt(offset(page) + PAGE_NEXT)) {
			final int start = visit(page);
			final int used = Short.toUnsignedInt(file.getShort(start + PAGE_USED));

			if (file.get(start + PAGE_TYPE) != OVERFLOW || used > pageSize - PAGE_HEADER) {
				throw refusal("page " + page + " is not the overflow page an item points at");
			}

			data.put(bytes(start + PAGE_HEADER, Math.min(used, data.remaining())));
		}

		return data.array();
	}

	private byte[] bytes(final int start, final int length) {
		final byte[] bytes = new byte[length];

		file.get(start, bytes);
		return bytes;
	}

	private int visit(final int page) {
		final int start = offset(page);

		if (visited.get(page)) {
			throw refusal("page " + page + " is reached twice");
		}

		visited.set(page);
		return start;
	}

	private int offset(final int page) {
		if (page < 0 || (long) page * pageSize >= file.limit()) {
			throw refusal("it points at page " + page + ", past its end");
		}

		return page * pageSize;
	}

	private IllegalArgumentException refusal(final String reason) {
		return new IllegalArgumentException(path + " is not a wallet.dat Janus can read: " + reason);
	}
}
