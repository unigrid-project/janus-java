/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.unigrid.janus.model.UpdateURL;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.signal.HedgehogError;
import org.unigrid.janus.model.signal.SplashMessage;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.OS;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Eager
@ApplicationScoped
public class Hedgehog {

	@Inject
	private DebugService debug;
	@Inject
	private Event<HedgehogError> hedgehogError;
	@Inject
	private Event<SplashMessage> splashMessageEvent;

	private Configuration config = null;

	private String hedgehogExecName = "hedgehog";

	private Process p;

	private static final Map<?, ?> OS_CONFIG = ArrayUtils
		.toMap(new Object[][]{{OS.LINUX, UpdateURL.getLinuxUrl()},
			{OS.WINDOWS, UpdateURL.getWindowsUrl()},
			{OS.MAC, UpdateURL.getMacUrl()}});

	@PostConstruct
	@SneakyThrows
	private void init() {
		URL configURL = new URL(OS_CONFIG.get(OS.CURRENT).toString());
		Reader in = new InputStreamReader(configURL.openStream(), StandardCharsets.UTF_8);
		config = Configuration.read(in);
		List<FileMetadata> files = config.getFiles();
		for (FileMetadata file : files) {
			String name = file.getPath().getFileName().toString();
			if (name.contains("hedgehog")) {
				hedgehogExecName = file.getPath().toString();
				System.out.println(hedgehogExecName);

				if (Files.exists(file.getPath()) && Files.isRegularFile(file.getPath())) {
					if (!file.getPath().toFile().canExecute()) {
						file.getPath().toFile().setExecutable(true);
					}
				} else {
					debug.print("hedgehog file not found or not a valid executable file.",
						Hedgehog.class.getSimpleName());
					hedgehogError.fire(HedgehogError.NOT_FOUND);
				}
			}
		}
	}

	@SneakyThrows
	public void startHedgehog() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(hedgehogExecName, "daemon");
		p = pb.start();
		debug.print("Connecting to Hedgehog...", Hedgehog.class.getSimpleName());
		// splashMessageEvent.fire(
		// SplashMessage.builder().message("Starting Hedgehog").build());
		// p.waitFor(10, TimeUnit.SECONDS);
		CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
			try {
				return connectToHedgehog();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		int statusCode = future.get();
		if (statusCode == 200) {
			debug.print("Connected to Hedgehog", Hedgehog.class.getSimpleName());
			splashMessageEvent.fire(
				SplashMessage.builder().message("Connected to Hedgehog").build());
		} else {
			debug.print("Failed to connect to Hedgehog (status code " + statusCode + ")",
				Hedgehog.class.getSimpleName());
		}
	}

	public int connectToHedgehog()
		throws InterruptedException, ExecutionException, TimeoutException {
		String uri = "https://127.0.0.1:52884/gridspork";

		// Trust all certificates
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}
		};

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			// Handle the exception
		}

		// Disable hostname verification
		HostnameVerifier allHostsValid = (hostname, session) -> true;
		Client client = ClientBuilder.newBuilder().sslContext(sc)
			.hostnameVerifier(allHostsValid).build();

		final AtomicInteger statusCode = new AtomicInteger(-1);
		WebTarget target = client.target(uri);
		Callable<Integer> task = () -> {
			int maxRetries = 10; // Limit the number of retries
			int retries = 0;
			while (statusCode.get() != 200 && retries < maxRetries) {
				retries++;
				Response response;
				try {
					response = target.request()
						.property("javax.xml.ws.client.receiveTimeout", 5000)
						.get();
				} catch (ProcessingException e) {
					System.err.println("response Error: " + e.getMessage());
					debug.print("response Error: " + e.getMessage(),
						Hedgehog.class.getSimpleName());
					TimeUnit.SECONDS.sleep(1); // Wait a second before retrying
					continue;
				}
				statusCode.set(response.getStatus());
			}
			return statusCode.get();
		};
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Integer> future = executor.submit(task);
		int timeout = 5; // Timeout in seconds
		try {
			statusCode.set(future.get(timeout, TimeUnit.SECONDS));
		} catch (TimeoutException e) {
			future.cancel(true); // Interrupt the task if it's still running
			throw e; // Rethrow the TimeoutException
		} finally {
			executor.shutdown(); // Shutdown the executor
		}
		debug.print("Status Code from hedgehog: " + statusCode.get(), Hedgehog.class.getSimpleName());
		if (statusCode.get() != 200) {
			debug.print("Failed to connect to Hedgehog (status code " + statusCode.get() + ")",
				Hedgehog.class.getSimpleName());
			hedgehogError.fire(HedgehogError.CONNECTION_FAILED);
			Platform.runLater(() -> splashMessageEvent.fire(
				SplashMessage.builder().message("Failed to connect to Hedgehog").build()));
		} else {
			Platform.runLater(() -> splashMessageEvent.fire(
				SplashMessage.builder().message("Connected to Hedgehog").build()));
		}
		return statusCode.get();
	}

	public Process getProcess() {
		return p;
	}

	@SneakyThrows
	public void stopHedgehog() {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(hedgehogExecName, "cli", "stop");
			pb.start();
		} catch (IOException e) {
			// Log or print a message indicating that an error occurred
			System.err.println("Error stopping Hedgehog: " + e.getMessage());
			debug.print("Error stopping Hedgehog: " + e.getMessage() + ")",
				Hedgehog.class.getSimpleName());
		}
	}
}
