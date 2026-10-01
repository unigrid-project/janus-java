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

package org.unigrid.janus.desktop;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.unigrid.janus.core.hedgehog.HedgehogService;
import org.unigrid.janus.shell.BrowserWindow;

/**
 * The installed Janus with its window open to a debugging connection, so a test can work it the way a person
 * would, and with Hedgehog told to trust the key the mocked bootstrap is signed with.
 */
final class RunningJanus implements AutoCloseable {
	private static final int PORT = 9333;
	private static final String ADDRESS = "http://127.0.0.1:" + PORT;
	private static final Duration START = Duration.ofMinutes(3);
	private static final Duration STOP = Duration.ofMinutes(1);
	private static final Duration POLL = Duration.ofSeconds(1);
	private static final int OK = 200;

	private final Process process;
	private final Playwright playwright;
	private final Page page;
	private final List<Path> hedgehog;
	private final Path threads;
	private final List<ProcessHandle> family = new ArrayList<>();

	private RunningJanus(final Process process, final Playwright playwright, final Page page,
		final List<Path> hedgehog, final Path threads) {

		this.process = process;
		this.playwright = playwright;
		this.page = page;
		this.hedgehog = hedgehog;
		this.threads = threads;
	}

	/** Starts the launcher; the hedgehog folders are where Hedgehog's executables run from. */
	static RunningJanus start(final Path launcher, final String networkKeys, final Path log,
		final List<Path> hedgehog) throws IOException, InterruptedException {

		final ProcessBuilder builder = new ProcessBuilder(launcher.toString()).redirectErrorStream(true)
			.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));

		builder.environment().put("JAVA_TOOL_OPTIONS", "-D" + BrowserWindow.DEBUGGING_PORT + "=" + PORT + " -D"
			+ HedgehogService.NETWORK_KEYS + "=" + networkKeys);

		final Process process = builder.start();
		final Path threads = log.resolveSibling(log.getFileName() + ".threads");

		try {
			awaitDebugging(process);

			/* Playwright only attaches to the browser Janus runs, so it needs none of its own. */
			final Playwright playwright = Playwright.create(new Playwright.CreateOptions()
				.setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));

			final Browser browser = playwright.chromium().connectOverCDP(ADDRESS);

			return new RunningJanus(process, playwright, awaitPage(browser, log.resolveSibling(log.getFileName()
				+ ".png")), hedgehog, threads);
		} catch (IOException | InterruptedException | RuntimeException e) {
			if (process.isAlive()) {
				dumpThreads(process, threads);
			}

			end(process, process.descendants().toList(), hedgehog);
			throw e;
		}
	}

	Page page() {
		return page;
	}

	/* Closed from the page, the way a person closes it, so quitting is tested too. The click is dispatched by the
	   page because the window, and the connection with it, goes away while Playwright would still be waiting. */
	boolean quit() throws IOException, InterruptedException {
		family.addAll(process.descendants().toList());
		page.evaluate("document.querySelector('[data-window=close]').click()");

		if (process.waitFor(STOP.toSeconds(), TimeUnit.SECONDS)) {
			return true;
		}

		dumpThreads(process, threads);
		return false;
	}

	/* A Janus that is stuck, opening or quitting, says where through its threads. The launcher runs Java inside
	   its own process except on Windows, where Java is one of its children, so each of them is asked. */
	private static void dumpThreads(final Process process, final Path file)
		throws IOException, InterruptedException {

		final Path jcmd = Path.of(System.getProperty("java.home"), "bin",
			Installation.WINDOWS ? "jcmd.exe" : "jcmd");

		for (final ProcessHandle candidate : Stream.concat(Stream.of(process.toHandle()), process.descendants())
			.toList()) {

			new ProcessBuilder(jcmd.toString(), Long.toString(candidate.pid()), "Thread.print")
				.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(file.toFile()))
				.start().waitFor(STOP.toSeconds(), TimeUnit.SECONDS);
		}
	}

	void screenshot(final Path file) {
		page.screenshot(new Page.ScreenshotOptions().setPath(file));
	}

	/* Hedgehog would hold its port with a token the next start does not know. It runs as Janus's child, but one
	   orphaned by a Janus that died on its own is only found by the folder it runs from. */
	@Override
	public void close() {
		try {
			family.addAll(process.descendants().toList());
			end(process, family, hedgehog);
		} finally {
			playwright.close();
		}
	}

	/* Each process is waited for, so the next start does not race a dying one for its ports. */
	private static void end(final Process process, final List<ProcessHandle> family, final List<Path> hedgehog) {
		final List<ProcessHandle> all = Stream.of(Stream.of(process.toHandle()), family.stream(),
			ProcessHandle.allProcesses().filter(candidate -> runsFrom(candidate, hedgehog))
		).flatMap(Function.identity()).toList();

		all.forEach(ProcessHandle::destroyForcibly);
		all.forEach(RunningJanus::awaitEnd);
	}

	private static boolean runsFrom(final ProcessHandle candidate, final List<Path> folders) {
		return candidate.info().command().map(Path::of)
			.filter(command -> folders.stream().anyMatch(command::startsWith)).isPresent();
	}

	private static void awaitEnd(final ProcessHandle handle) {
		try {
			handle.onExit().get(STOP.toSeconds(), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException e) {
			throw new IllegalStateException("Process " + handle.pid() + " did not end", e);
		}
	}

	private static void awaitDebugging(final Process process) throws IOException, InterruptedException {
		final HttpClient http = HttpClient.newHttpClient();
		final HttpRequest request = HttpRequest.newBuilder(URI.create(ADDRESS + "/json/version")).build();
		final Instant deadline = Instant.now().plus(START);

		while (Instant.now().isBefore(deadline)) {
			if (!process.isAlive()) {
				throw new IOException("Janus stopped with exit " + process.exitValue()
					+ " before its window opened");
			}

			try {
				if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == OK) {
					return;
				}
			} catch (IOException e) {
				/* Nothing listens until the browser engine is up. */
			}

			Thread.sleep(POLL.toMillis());
		}

		throw new IOException("No debugging connection on " + ADDRESS + " within " + START);
	}

	/* Without a page there is nothing for the browser to take a picture of, so the screen is taken instead. */
	private static Page awaitPage(final Browser browser, final Path screen) throws IOException, InterruptedException {
		final Instant deadline = Instant.now().plus(START);

		while (Instant.now().isBefore(deadline)) {
			final Optional<Page> page = browser.contexts().stream().flatMap(context -> context.pages().stream())
				.filter(candidate -> candidate.url().startsWith("http://127.0.0.1")).findFirst();

			if (page.isPresent()) {
				return page.get();
			}

			Thread.sleep(POLL.toMillis());
		}

		final String pages = browser.contexts().stream().flatMap(context -> context.pages().stream())
			.map(Page::url).toList().toString();
		final String targets = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(ADDRESS
			+ "/json/list")).build(), HttpResponse.BodyHandlers.ofString()).body();

		String picture = "the screen is in " + screen;

		try {
			final Rectangle bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

			ImageIO.write(new Robot().createScreenCapture(bounds), "png", screen.toFile());
		} catch (AWTException | HeadlessException e) {
			picture = "no picture of the screen: " + e;
		}

		throw new IOException("The window showed no page of Janus within " + START + "; Playwright saw " + pages
			+ " and the browser lists " + targets + "; " + picture);
	}
}
