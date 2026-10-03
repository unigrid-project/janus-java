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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.io.TempDir;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.legacy.LegacyWallet;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Installs the package built for this computer, the way a person would, and walks the installed wallet through
 * its journeys in its own window: a legacy wallet.dat over a mocked ledger, an EVM wallet restored from its words
 * and one made new. Hedgehog is the real one, downloaded by Janus. It installs software and opens windows, so it
 * runs only where janus.installed-wallet is true, which only the workflow sets.
 */
@EnabledIfSystemProperty(named = "janus.installed-wallet", matches = "true")
@Timeout(value = 20, unit = TimeUnit.MINUTES)
public class InstalledWalletIT {
	private static final Path DIST = Path.of("target", "dist").toAbsolutePath();
	private static final Path EVIDENCE = Path.of("target", "installed-wallet").toAbsolutePath();
	private static final Path HOME = Path.of(System.getProperty("user.home"));
	private static final Path JANUS = HOME.resolve(".janus");
	private static final String ABANDON = "abandon abandon abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon about";
	private static final String RESTORED = "evm-0x9858EfFD232B4033E47d90003D41EC34EcaEda94.json";
	private static final String PASSWORD = "correct horse";
	private static final String CARD = "main > .card";
	private static final String CONTINUE = CARD + " .button--primary";
	private static final String IMPORT = "[hx-post='/action/import']";
	private static final String TOTAL = ".dashboard__total";
	private static final String RETRY = "[hx-post='/action/wallet-retry']";
	private static final int ATTEMPTS = 3;
	private static final int BROWSER_LOG_DEPTH = 3;
	private static final int DELETE_ATTEMPTS = 30;
	private static final long DELETE_PAUSE_MILLIS = 1000;
	private static final double FIRST_LEDGER_MILLIS = 600_000;

	/* The first wallet waits on Hedgehog's download as well as the ledger. */
	private static final LocatorAssertions.HasTextOptions LOADED = new LocatorAssertions.HasTextOptions()
		.setTimeout(FIRST_LEDGER_MILLIS);
	private static final Page.WaitForSelectorOptions SHOWN = new Page.WaitForSelectorOptions()
		.setTimeout(FIRST_LEDGER_MILLIS);

	@TempDir
	private static Path scratch;

	private static Path launcher;
	private static Path wallet;
	private static String networkKeys;

	private RunningJanus janus;

	@RegisterExtension
	private final TestExecutionExceptionHandler evidence = (context, failure) -> {
		keepEvidence(context.getRequiredTestMethod().getName(), failure);
		throw failure;
	};

	@BeforeAll
	static void installOverAMockedLedger() throws Exception {
		final KeyPair keys = MockBootstrap.keys();
		final Path bootstrap = hedgehogData().resolve("bootstrap.dat");

		Files.createDirectories(EVIDENCE);
		Files.createDirectories(bootstrap.getParent());
		wallet = scratch.resolve("wallet.dat");

		try (InputStream in = InstalledWalletIT.class.getResourceAsStream("wallet.dat")) {
			Files.copy(in, wallet);
		}

		networkKeys = MockBootstrap.publicKey(keys);
		MockBootstrap.write(bootstrap, LegacyWallet.addresses(wallet).first(), MockBootstrap.HISTORY, keys);
		launcher = Installation.install(DIST, scratch, EVIDENCE);
	}

	/* Hedgehog's download under ~/.janus is kept, so only the first journey waits for it. Only the legacy wallet
	   goes from its folder: on Windows that folder is also Hedgehog's, since UNIGRID and Unigrid are one name. */
	@BeforeEach
	void forgetTheLastWallet() throws IOException, InterruptedException {
		for (final String chosen : List.of("wallet", "wallets", "backups")) {
			deleteTree(JANUS.resolve(chosen));
		}

		deleteTree(new DataDirectory().path().resolve("wallet.dat"));
	}

	@AfterEach
	void stop() {
		if (janus != null) {
			janus.close();
		}
	}

	@Test
	public void shouldOpenTheLegacyWalletAndOpenOnItAgainAfterARestart() throws Exception {
		Files.createDirectories(new DataDirectory().path());
		Files.copy(wallet, new DataDirectory().path().resolve("wallet.dat"));

		final Page page = start("legacy");

		page.click(IMPORT);
		page.click("[hx-post='/action/import-found']");
		page.click("[hx-post='/action/open-wallet']");
		assertTotal(page, "480.00");
		page.click("button.app__tab:has-text('Activity')");
		assertThat(page.locator("#rows details")).hasCount(2);

		assertTrue(janus.quit(), "Janus kept running after its window was closed");
		janus.close();
		assertTotal(start("legacy-again"), "480.00");
	}

	@Test
	public void shouldRestoreAnEvmWalletFromItsWords() throws Exception {
		final Page page = start("restore");

		page.click(IMPORT);
		page.click("[hx-post='/action/restore']");
		page.fill("[name=word1]", ABANDON);
		page.click(CONTINUE);
		seal(page);

		assertTotal(page, "0.00");
		assertTrue(Files.exists(JANUS.resolve("wallets").resolve(RESTORED)));
	}

	@Test
	public void shouldMakeANewEvmWalletFromTheWordsTappedBack() throws Exception {
		final Page page = start("create");

		page.click("[hx-post='/action/create']");
		assertThat(page.locator(".phrase__word")).hasCount(12);

		final List<String> words = page.locator(".phrase__word > span:not(.phrase__n)").allTextContents();

		page.click("[hx-post='/action/create-verify']");

		for (final String word : words) {
			final Locator.FilterOptions tile = new Locator.FilterOptions()
				.setHasText(Pattern.compile("^" + word + "$"));

			page.locator("button.tile:not([disabled])").filter(tile).first().click();
		}

		page.click(CONTINUE);
		seal(page);

		assertTotal(page, "0.00");

		try (Stream<Path> made = Files.list(JANUS.resolve("wallets"))) {
			assertEquals(1, made.filter(file -> file.getFileName().toString().startsWith("evm-0x")).count());
		}
	}

	private Page start(final String journey) throws IOException, InterruptedException {
		janus = RunningJanus.start(launcher, networkKeys, EVIDENCE.resolve(journey + ".log"),
			List.of(JANUS.resolve("hedgehog"), hedgehogData())
		);
		return janus.page();
	}

	/* GitHub turns a download away now and then, and a person would press Retry, so the test does too. */
	private static void assertTotal(final Page page, final String total) {
		for (int attempt = 1; attempt < ATTEMPTS; attempt++) {
			page.waitForSelector(TOTAL + ", " + RETRY, SHOWN);

			if (!page.locator(RETRY).isVisible()) {
				break;
			}

			page.click(RETRY);
		}

		assertThat(page.locator(TOTAL)).hasText(total, LOADED);
	}

	/* The card focuses its first field once it has settled, and text typed for the second field before then lands
	   in the first. */
	private static void seal(final Page page) {
		page.waitForSelector("[name=password]:focus");
		page.fill("[name=password]", PASSWORD);
		page.fill("[name=repeat]", PASSWORD);
		page.click(CONTINUE);
	}

	/* The log is kept first and on its own, since the screenshot fails exactly when Janus has died. */
	private void keepEvidence(final String test, final Throwable failure) {
		final Path log = JANUS.resolve("hedgehog.log");

		try {
			if (Files.exists(log)) {
				Files.copy(log, EVIDENCE.resolve(test + "-hedgehog.log"),
					StandardCopyOption.REPLACE_EXISTING);
			}

			keepBrowserLogs(test);
		} catch (IOException e) {
			failure.addSuppressed(e);
		}

		try {
			if (janus != null) {
				janus.screenshot(EVIDENCE.resolve(test + ".png"));
			}
		} catch (RuntimeException e) {
			failure.addSuppressed(e);
		}
	}

	/* Where there is no console, as on Windows, the browser engine writes its log next to the executable or into
	   the folder it was started from. */
	private static void keepBrowserLogs(final String test) throws IOException {
		int found = 0;

		for (final Path root : List.of(launcher.getParent(), Path.of("").toAbsolutePath())) {
			try (Stream<Path> paths = Files.walk(root, BROWSER_LOG_DEPTH)) {
				for (final Path browserLog : paths.filter(path -> path.endsWith("debug.log")).toList()) {
					Files.copy(browserLog, EVIDENCE.resolve(test + "-browser-" + found++ + ".log"),
						StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	/* Where Hedgehog keeps its files, and so where it looks for the bootstrap before downloading one. */
	private static Path hedgehogData() {
		if (Installation.WINDOWS) {
			return Path.of(System.getenv("APPDATA"), "Unigrid", "Hedgehog");
		}

		if (Installation.MAC) {
			return HOME.resolve("Library").resolve("Application Support").resolve("Hedgehog");
		}

		return Optional.ofNullable(System.getenv("XDG_DATA_HOME")).map(Path::of)
			.orElse(HOME.resolve(".local").resolve("share")).resolve("hedgehog");
	}

	private static void deleteTree(final Path root) throws IOException, InterruptedException {
		if (!Files.exists(root)) {
			return;
		}

		try (Stream<Path> paths = Files.walk(root)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				delete(path);
			}
		}
	}

	/* Windows refuses to delete a file that is still open, and a process that has just ended, or a virus
	   scanner looking at a new file, lets go of it a moment later. */
	private static void delete(final Path path) throws IOException, InterruptedException {
		for (int attempt = 1;; attempt++) {
			try {
				Files.delete(path);
				return;
			} catch (AccessDeniedException e) {
				if (attempt == DELETE_ATTEMPTS) {
					throw e;
				}

				Thread.sleep(DELETE_PAUSE_MILLIS);
			}
		}
	}
}
