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

package org.unigrid.janus.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.evm.EvmWallet;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.evm.LegacyVault;
import org.unigrid.janus.core.evm.Mnemonic;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.evm.WrongPassword;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.legacy.LegacyKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.ui.FlowSupport.entry;
import static org.unigrid.janus.ui.FlowSupport.settle;
import static org.unigrid.janus.ui.FlowSupport.tap;
import static org.unigrid.janus.ui.FlowSupport.words;

/** Importing a wallet dump with the container wiring the controllers: the keys are sealed behind a new phrase. */
public class ImportDumpIT {
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String IMPORT_DUMP = "[hx-post=/action/import-dump]";
	private static final String SAVED = "[hx-post=/action/create-verify]";
	private static final String CONFIRMED = "[hx-post=/action/create-password]";
	private static final String PASSWORD = "form[hx-post=/action/phrase-save]";
	private static final String BACK = "[hx-post=/action/create-back]";
	private static final String SECRET = "correct horse";
	private static final List<String> ABANDON = List.of(("abandon ".repeat(11) + "about").split(" "));

	private ContainerRig rig;
	private Path dump;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ContainerRig();
		dump = rig.keepDumpElsewhere();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	private Screen onTheNewPhrase() throws Exception {
		return rig.open().click(IMPORT).trigger(CHOOSE_FILE, Map.of("path", dump.toString())).click(IMPORT_DUMP);
	}

	private static Screen confirm(final Screen seed) throws Exception {
		final List<String> written = words(seed);

		seed.click(SAVED);

		for (final String word : written) {
			tap(seed, word);
		}

		return seed;
	}

	private Screen sealed() throws Exception {
		final Screen password = confirm(onTheNewPhrase()).click(CONFIRMED);

		return password.submit(PASSWORD, Map.of("password", SECRET, "repeat", SECRET));
	}

	private List<Path> savedWallets() throws IOException {
		if (!Files.isDirectory(rig.wallets())) {
			return List.of();
		}

		try (Stream<Path> files = Files.list(rig.wallets())) {
			return files.sorted().toList();
		}
	}

	private static String enc(final String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	/** Imports the dump under the password and answers the words of the phrase made for it. */
	private List<String> importedUnder(final String password) throws Exception {
		final Screen seed = onTheNewPhrase();
		final List<String> words = words(seed);

		confirm(seed).click(CONFIRMED).submit(PASSWORD, Map.of("password", password, "repeat", password));
		return words;
	}

	/** Restores the phrase from its words under a password and answers how the save went. */
	private int restored(final List<String> words, final String password) throws Exception {
		final Screen screen = rig.open();

		assertEquals(200, screen.client().submit("/action/restore", "").statusCode());
		assertEquals(200, screen.client().submit("/action/restore-words",
			"word1=" + enc(String.join(" ", words))).statusCode()
		);
		final String sealed = "password=" + enc(password) + "&repeat=" + enc(password);

		return screen.client().submit("/action/phrase-save", sealed).statusCode();
	}

	private EvmWallet saved() throws IOException {
		return EvmWalletStore.read(savedWallets().get(0));
	}

	private List<String> addressesOf(final List<LegacyKey> keys) {
		return keys.stream().map(LegacyKey::address).sorted().toList();
	}

	@Example
	public void shouldRecoverTheLegacyKeysFromThePhraseAloneWhenThePasswordIsLost() throws Exception {
		final List<String> words = importedUnder(SECRET);
		final List<LegacyKey> keys = saved().legacyKeys(Mnemonic.parse(String.join(" ", words)), new LegacyVault());

		assertEquals(rig.dumpAddresses().stream().sorted().toList(), addressesOf(keys));
	}

	@Example
	public void shouldKeepEveryLegacyKeyWhenThePhraseIsRestoredUnderANewPassword() throws Exception {
		final List<String> words = importedUnder(SECRET);
		final EvmWallet before = saved();

		assertEquals(200, restored(words, "a brand new password"));

		final EvmWallet after = saved();

		assertEquals(1, savedWallets().size());
		assertEquals(before.legacy(), after.legacy());
		assertEquals(rig.dumpAddresses().stream().sorted().toList(),
			addressesOf(after.legacyKeys("a brand new password", new SeedVault(), new LegacyVault()))
		);
		assertThrows(WrongPassword.class, () -> after.legacyKeys(SECRET, new SeedVault(), new LegacyVault()));
	}

	@Example
	public void shouldKeepEveryLegacyKeyWhenThePhraseIsRestoredUnderTheSamePassword() throws Exception {
		final List<String> words = importedUnder(SECRET);

		assertEquals(200, restored(words, SECRET));
		assertEquals(6, saved().legacyKeys(SECRET, new SeedVault(), new LegacyVault()).size());
	}

	@Example
	public void shouldKeepEveryLegacyKeyThroughSeveralRestoresInARow() throws Exception {
		final List<String> words = importedUnder(SECRET);

		assertEquals(200, restored(words, "second password"));
		assertEquals(200, restored(words, "third password"));
		assertEquals(rig.dumpAddresses().stream().sorted().toList(),
			addressesOf(saved().legacyKeys("third password", new SeedVault(), new LegacyVault()))
		);
	}

	@Example
	public void shouldLeaveTheWalletAsItWasWhenARestoreIsRefused() throws Exception {
		final List<String> words = importedUnder(SECRET);
		final String before = Files.readString(savedWallets().get(0));
		final Screen screen = rig.open();

		screen.client().submit("/action/restore", "");
		screen.client().submit("/action/restore-words", "word1=" + enc(String.join(" ", words)));
		screen.client().submit("/action/phrase-save", "password=short&repeat=short");
		screen.client().submit("/action/phrase-save", "password=" + enc(SECRET) + "&repeat=other+password");

		assertEquals(before, Files.readString(savedWallets().get(0)));
		assertEquals(6, saved().legacyKeys(SECRET, new SeedVault(), new LegacyVault()).size());
	}

	@Example
	public void shouldNotCarryLegacyKeysToAWalletOfAnotherPhrase() throws Exception {
		importedUnder(SECRET);

		final Path imported = savedWallets().get(0);
		final String before = Files.readString(imported);

		assertEquals(200, restored(ABANDON, SECRET));
		assertEquals(2, savedWallets().size());
		assertEquals(before, Files.readString(imported));
		assertNull(EvmWalletStore.read(savedWallets().stream().filter(file -> !file.equals(imported)).findFirst()
			.orElseThrow()).legacy());
	}

	@Example
	public void shouldNotInventLegacyKeysWhenAPlainWalletIsRestoredAgain() throws Exception {
		assertEquals(200, restored(ABANDON, SECRET));
		assertEquals(200, restored(ABANDON, "another password"));
		assertEquals(1, savedWallets().size());
		assertNull(saved().legacy());
	}

	@Example
	public void shouldSealTheKeysOfTheDumpBehindTheNewPhraseAndPassword() throws Exception {
		sealed();

		final EvmWallet wallet = EvmWalletStore.read(savedWallets().get(0));
		final List<LegacyKey> keys = wallet.legacyKeys(SECRET, new SeedVault(), new LegacyVault());
		final List<String> expected = rig.dumpAddresses().stream().sorted().toList();

		assertEquals(EvmWallet.LEGACY_VERSION, wallet.version());
		assertEquals(expected, wallet.legacy().addresses());
		assertEquals(expected, keys.stream().map(LegacyKey::address).toList());
	}

	@Example
	public void shouldKeepNoPrivateKeyOfTheDumpInTheClearOrInACopy() throws Exception {
		sealed();

		final String written = Files.readString(savedWallets().get(0));

		for (final String line : Files.readAllLines(dump)) {
			if (!line.isBlank() && !line.startsWith("#")) {
				assertFalse(written.contains(line.split(" ")[0]));
			}
		}

		assertFalse(Files.exists(rig.home().resolve(".janus").resolve("backups")));
	}

	@Example
	public void shouldOpenOnTheFundsOfTheKeysOfTheDump() throws Exception {
		rig.hedgehog().address(rig.dumpAddresses().get(0), "30", entry("aa", 1, "30", EntryKind.RECEIVED))
			.address(rig.dumpAddresses().get(5), "12", entry("bb", 2, "12", EntryKind.RECEIVED));

		assertEquals("42.00", settle(sealed()).find(".dashboard__total").text());
		assertEquals(savedWallets().get(0), rig.chosen().remembered().orElseThrow());
	}

	@Example
	public void shouldSayThatTheImportedKeysAreSealedOnThePasswordCard() throws Exception {
		final String note = confirm(onTheNewPhrase()).click(CONFIRMED).find("main > .card .step__note").text();

		assertTrue(note.contains("imported keys"), note);
	}

	@Example
	public void shouldAskForAPasswordTwiceAndSealNothingUntilBothAreRight() throws Exception {
		final Screen screen = confirm(onTheNewPhrase()).click(CONFIRMED);

		assertEquals(1, screen.document().select(PASSWORD + " input[type=password][name=password]").size());
		assertEquals(1, screen.document().select(PASSWORD + " input[type=password][name=repeat]").size());

		screen.submit(PASSWORD, Map.of("password", SECRET, "repeat", "correct hose"));
		assertEquals("The two passwords differ", screen.find(".step__error").text());
		screen.submit(PASSWORD, Map.of("password", "short", "repeat", "short"));
		assertEquals("Use at least 8 characters", screen.find(".step__error").text());
		assertEquals(List.of(), savedWallets());

		screen.submit(PASSWORD, Map.of("password", SECRET, "repeat", SECRET));
		assertEquals(1, savedWallets().size());
	}

	@Example
	public void shouldNotAskForAPasswordWhenAWalletFileIsImported() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "30", entry("aa", 1, "30", EntryKind.RECEIVED));

		final Path wallet = rig.keepWalletElsewhere();
		final Screen screen = rig.open().click(IMPORT).trigger(CHOOSE_FILE, Map.of("path", wallet.toString()));

		assertEquals("/action/open-wallet", screen.find(".step__actions .button--primary").attr("hx-post"));

		final Screen opened = settle(screen.click("[hx-post=/action/open-wallet]"));

		assertEquals("30.00", opened.find(".dashboard__total").text());
		assertTrue(opened.document().select("input[type=password]").isEmpty());
		assertEquals(List.of(), savedWallets());
		assertEquals(500, opened.client().submit("/action/phrase-save", "password=a&repeat=a").statusCode());
	}

	@Example
	public void shouldGoBackFromTheNewPhraseToTheImportCardAndForgetTheKeys() throws Exception {
		final Screen screen = onTheNewPhrase().click(BACK);

		assertEquals("Bring your wallet", screen.find("main > .card h1").text());
		assertEquals(500, screen.client().submit("/action/phrase-save", "password=a&repeat=a").statusCode());
	}

	@Example
	public void shouldNotCarryTheKeysIntoAWalletMadeLater() throws Exception {
		final Screen screen = onTheNewPhrase().click(BACK).click("[hx-post=/action/welcome]");

		confirm(screen.click("[hx-post=/action/create]")).click(CONFIRMED)
			.submit(PASSWORD, Map.of("password", SECRET, "repeat", SECRET));
		assertNull(EvmWalletStore.read(savedWallets().get(0)).legacy());
	}

	@Example
	public void shouldHaveNoPhraseToMakeWhenNoDumpWasChosen() throws Exception {
		assertEquals(500, rig.open().client().submit("/action/import-dump", "").statusCode());
		assertEquals(List.of(), savedWallets());
	}

	@Example
	public void shouldKeepTheKeysWhenTheWalletCannotBeSavedAndSealThemOnceItCan() throws Exception {
		final Path blocker = Files.createDirectories(rig.wallets().getParent()).resolve("wallets");
		final Screen screen = confirm(onTheNewPhrase()).click(CONFIRMED);
		final String form = "password=correct+horse&repeat=correct+horse";

		Files.createFile(blocker);
		assertEquals(500, screen.client().submit("/action/phrase-save", form).statusCode());
		Files.delete(blocker);

		assertEquals(200, screen.client().submit("/action/phrase-save", form).statusCode());
		assertEquals(6, EvmWalletStore.read(savedWallets().get(0)).legacy().addresses().size());
	}
}
