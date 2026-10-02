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
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.legacy.LegacyKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
