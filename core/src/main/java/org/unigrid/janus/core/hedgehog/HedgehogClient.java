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
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.glassfish.jersey.client.ClientProperties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Asks Hedgehog what the frozen legacy ledger holds for the addresses of a wallet. */
public class HedgehogClient implements AutoCloseable {
	public static final URI LOCAL = URI.create("https://127.0.0.1:52884");

	private static final String LOOPBACK = "127.0.0.1";
	private static final Duration TIMEOUT = Duration.ofSeconds(10);
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

	public HedgehogClient() {
		this(LOCAL, TIMEOUT);
	}

	HedgehogClient(final URI base, final Duration timeout) {
		if (!LOOPBACK.equals(base.getHost())) {
			throw new IllegalArgumentException("Hedgehog is only ever asked on this computer, never at " + base);
		}

		client = ClientBuilder.newBuilder().sslContext(trustingContext())
			.hostnameVerifier((host, session) -> LOOPBACK.equals(host))
			.property(ClientProperties.FOLLOW_REDIRECTS, false)
			.connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
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

	/**
	 * The version of the Hedgehog answering here, or empty when none does. Something else answering on
	 * Hedgehog's port counts as no Hedgehog, since it cannot be asked about the ledger either.
	 */
	public Optional<String> version() {
		try {
			return Optional.of(ask(hedgehog.path("version"),
				response -> read(response, answer -> answer.readEntity(Version.class))
			).version());
		} catch (HedgehogUnavailable | SnapshotMissing | IllegalArgumentException | IllegalStateException e) {
			return Optional.empty();
		}
	}

	public void stop() {
		ask(hedgehog.path("stop"), request -> request.post(Entity.json("")),
			response -> read(response, answer -> answer.getStatus())
		);
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
		return ask(target, request -> request.get(), reader);
	}

	private static <T> T ask(final WebTarget target, final Function<Invocation.Builder, Response> call,
		final Function<Response, T> reader) {

		try (Response response = call.apply(target.request(MediaType.APPLICATION_JSON_TYPE))) {
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
			if (cutOff(e)) {
				throw new HedgehogUnavailable("Hedgehog stopped answering halfway", e);
			}

			throw new IllegalStateException("Hedgehog answered with something Janus cannot read", e);
		}
	}

	/* A read that times out or breaks halfway surfaces several layers down, wrapped by the JSON reader. */
	private static boolean cutOff(final Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof IOException) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Hedgehog makes a new self-signed certificate every time it starts, so there is no certificate to
	 * pin. What is trusted instead is the address: the client talks to this computer and nowhere else,
	 * never following a redirect away from it, and all it asks for is public ledger data whose signature
	 * Hedgehog has already checked.
	 */
	private static SSLContext trustingContext() {
		final X509TrustManager anyCertificate = new X509TrustManager() {
			@Override
			public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
			}

			@Override
			public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};

		try {
			final SSLContext context = SSLContext.getInstance("TLS");

			context.init(null, new TrustManager[] {anyCertificate}, null);
			return context;
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("This Java platform offers no TLS", e);
		}
	}
}
