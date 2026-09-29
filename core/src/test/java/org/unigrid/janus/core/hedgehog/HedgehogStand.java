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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An already running Hedgehog, played by a stub, that the modules building on core can hand their
 * services to. It answers balances and pages of history the way Hedgehog does, a thousand at most.
 */
public final class HedgehogStand implements AutoCloseable {
	private static final int PAGE = 1000;

	private final StubHedgehog stub;
	private final HedgehogClient client;
	private final List<String> mints = new ArrayList<>();

	private HedgehogStand(final StubHedgehog stub) {
		this.stub = stub;
		this.client = new HedgehogClient(stub.uri(), Duration.ofSeconds(2));
		stub.answer("/version", 200, "{\"version\":\"0.0.8\"}");
		stub.answer("/gridspork/mint-storage", 204, "");
	}

	public static HedgehogStand start() {
		try {
			return new HedgehogStand(new StubHedgehog());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public HedgehogStand signed() {
		stub.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));
		return this;
	}

	public HedgehogStand unsigned() {
		stub.answer("/bootstrap", 200, FakeHedgehog.snapshot("UNSIGNED"));
		return this;
	}

	public HedgehogStand address(final String address, final String balance, final AddressTransaction... entries) {
		final String path = "/bootstrap/address/" + address;

		stub.answer(path, 200, "{\"address\":\"" + address + "\",\"balance\":" + balance
			+ ",\"transactionCount\":" + entries.length + "}"
		);

		for (int offset = 0; offset < Math.max(entries.length, 1); offset += PAGE) {
			final List<AddressTransaction> page = Arrays.asList(entries)
				.subList(offset, Math.min(entries.length, offset + PAGE));

			stub.answer(path + "/transactions?offset=" + offset + "&limit=" + PAGE, 200, json(page));
		}

		stub.answer(path + "/transactions?offset=" + pageAfter(entries.length) + "&limit=" + PAGE, 200, "[]");
		return this;
	}

	/** Adds a mint for the address to the mint storage spork the stand answers with. */
	public HedgehogStand mint(final String address, final int height, final String amount) {
		mints.add("\"" + address + "/" + height + "\":" + amount);
		stub.answer("/gridspork/mint-storage", 200, mints.stream()
			.collect(Collectors.joining(",", "{\"type\":\"MINT_STORAGE\",\"data\":{\"mints\":{", "}}}"))
		);
		return this;
	}

	/** Makes the history of the address fail the way a Hedgehog that fell over would. */
	public HedgehogStand broken(final String address) {
		return broken(address, 0);
	}

	/** Makes the history of the address fail from the page at the offset on, as though Hedgehog fell over midway. */
	public HedgehogStand broken(final String address, final int offset) {
		stub.answer("/bootstrap/address/" + address + "/transactions?offset=" + offset + "&limit=" + PAGE, 500, "");
		return this;
	}

	/** Everything asked of the stand so far. */
	public List<URI> requests() {
		return List.copyOf(stub.requests());
	}

	/** Stops answering as Hedgehog, the way one that died would. */
	public HedgehogStand gone() {
		stub.answer("/version", 404, "");
		stub.answer("/bootstrap", 404, "");
		return this;
	}

	public HedgehogClient client() {
		return client;
	}

	/** A service that finds this stand already answering, so it never looks for an executable. */
	public HedgehogService service() {
		try {
			final Path log = Files.createTempFile("hedgehog", ".log");
			final HedgehogRelease release = Releases.unpinned(Files.createTempDirectory("hedgehog"));
			final HedgehogLocation location = new HedgehogLocation(null, null, "", "Linux", release);
			final HedgehogClient answering = new HedgehogClient(stub.uri(), Duration.ofSeconds(2));

			return new HedgehogService(location, new HedgehogInstaller(release), answering, stub.uri(), log,
				Duration.ofSeconds(5)
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public URI uri() {
		return stub.uri();
	}

	@Override
	public void close() {
		client.close();
		stub.close();
	}

	private static int pageAfter(final int count) {
		return (count + PAGE - 1) / PAGE * PAGE;
	}

	private static String json(final List<AddressTransaction> page) {
		return page.stream().map(entry -> "{\"transaction\":\"" + entry.transaction() + "\",\"time\":\""
			+ entry.time() + "\",\"height\":" + entry.height() + ",\"amount\":" + entry.amount().toPlainString()
			+ ",\"kind\":\"" + entry.kind() + "\"}"
		).collect(Collectors.joining(",", "[", "]"));
	}
}
