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

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** Asks Hedgehog what the frozen legacy ledger holds for the addresses of a wallet. */
public class HedgehogClient implements AutoCloseable {
	private static final int BAD_REQUEST = 400;
	private static final int NOT_FOUND = 404;
	private static final int UNAVAILABLE = 503;
	private static final GenericType<List<AddressTransaction>> TRANSACTIONS = new GenericType<>() {
	};

	/** The part of Hedgehog's version answer Janus reads. */
	public record Version(String version) {
	}

	private final Client client;
	private final WebTarget hedgehog;

	HedgehogClient(final URI base, final Duration timeout) {
		client = ClientBuilder.newBuilder().connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
			.readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).build();
		hedgehog = client.target(base);
	}

	public SnapshotInfo snapshot() {
		return ask(hedgehog.path("bootstrap"),
			response -> read(response, answer -> answer.readEntity(SnapshotInfo.class))
		);
	}

	public Optional<AddressBalance> balance(final String address) {
		return ask(address(address), response -> response.getStatus() == NOT_FOUND ? Optional.empty()
			: Optional.of(read(response, answer -> answer.readEntity(AddressBalance.class)))
		);
	}

	public List<AddressTransaction> transactions(final String address, final int offset, final int limit) {
		final WebTarget page = address(address).path("transactions").queryParam("offset", offset)
			.queryParam("limit", limit);

		return ask(page, response -> read(response, answer -> answer.readEntity(TRANSACTIONS)));
	}

	/** The version of the Hedgehog answering here, or empty when none does. */
	public Optional<String> version() {
		try {
			return Optional.of(ask(hedgehog.path("version"),
				response -> read(response, answer -> answer.readEntity(Version.class))
			).version());
		} catch (HedgehogUnavailable e) {
			return Optional.empty();
		}
	}

	@Override
	public void close() {
		client.close();
	}

	/* The address is a template value, so a slash or a question mark in it stays inside its own segment. */
	private WebTarget address(final String address) {
		return hedgehog.path("bootstrap/address/{address}").resolveTemplate("address", address);
	}

	private static <T> T ask(final WebTarget target, final Function<Response, T> reader) {
		try (Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get()) {
			return reader.apply(response);
		} catch (ProcessingException e) {
			throw new HedgehogUnavailable("Hedgehog does not answer at " + target.getUri(), e);
		}
	}

	private static <T> T read(final Response response, final Function<Response, T> entity) {
		final int status = response.getStatus();

		if (status == UNAVAILABLE) {
			throw new SnapshotMissing("Hedgehog holds no legacy ledger yet");
		}

		if (status == BAD_REQUEST) {
			throw new IllegalArgumentException("Hedgehog does not take that for a legacy address");
		}

		if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
			throw new IllegalStateException("Hedgehog answered " + status);
		}

		try {
			return entity.apply(response);
		} catch (ProcessingException e) {
			throw new IllegalStateException("Hedgehog answered with something Janus cannot read", e);
		}
	}
}
