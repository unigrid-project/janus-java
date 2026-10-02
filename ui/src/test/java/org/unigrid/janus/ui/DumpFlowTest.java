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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.hedgehog.EntryKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.ui.FlowSupport.entry;
import static org.unigrid.janus.ui.FlowSupport.settle;
import static org.unigrid.janus.ui.FlowSupport.tap;
import static org.unigrid.janus.ui.FlowSupport.words;

/** Importing a wallet dump, the text file the legacy daemon's dumpwallet writes, from the file choice to the dashboard. */
public class DumpFlowTest {
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String CONTINUE = ".step__actions .button--primary";
	private static final String SAVED = "[hx-post=/action/create-verify]";
	private static final String CONFIRMED = "[hx-post=/action/create-password]";
	private static final String PASSWORD = "form[hx-post=/action/phrase-save]";
	private static final String CARD = "main > .card";
	private static final String SECRET = "correct horse";

	private ControlCenterRig rig;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ControlCenterRig();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	private Screen pick(final Path file) throws Exception {
		return Screen.open(rig).click(IMPORT).trigger(CHOOSE_FILE, Map.of("path", file.toString()));
	}

	private Screen sealedUnderAPassword(final Screen chosen) throws Exception {
		final Screen seed = chosen.click(CONTINUE);
		final List<String> written = words(seed);

		seed.click(SAVED);

		for (final String word : written) {
			tap(seed, word);
		}

		return seed.click(CONFIRMED).submit(PASSWORD, Map.of("password", SECRET, "repeat", SECRET));
	}

	@Example
	public void shouldWarnThatTheDumpHoldsItsKeysUnprotectedAndKeepNoCopy() throws Exception {
		final Screen screen = pick(rig.keepDumpElsewhere());

		assertTrue(screen.find(CHOOSE_FILE).hasClass("choice--selected"));
		assertTrue(screen.document().select(CARD + " > .step__note").text().contains("private keys unprotected"));
		assertEquals("/action/import-dump", screen.find(CONTINUE).attr("hx-post"));
		assertFalse(Files.exists(rig.backups()));
	}

	@Example
	public void shouldGoFromAPickedDumpThroughANewPhraseToTheFundsOfItsKeys() throws Exception {
		rig.hedgehog().address(rig.dumpAddresses().get(0), "30", entry("aa", 1, "30", EntryKind.RECEIVED))
			.address(rig.dumpAddresses().get(5), "12", entry("bb", 2, "12", EntryKind.RECEIVED));

		final Screen screen = sealedUnderAPassword(pick(rig.keepDumpElsewhere()));

		assertEquals("42.00", settle(screen).find(".dashboard__total").text());
	}

	@Example
	public void shouldOpenOnTheSealedWalletNextTime() throws Exception {
		rig.hedgehog().address(rig.dumpAddresses().get(2), "7", entry("cc", 3, "7", EntryKind.RECEIVED));
		settle(sealedUnderAPassword(pick(rig.keepDumpElsewhere())));

		assertEquals("7.00", settle(Screen.open(rig)).find(".dashboard__total").text());
	}

	@Example
	public void shouldSayWhatIsWrongWithAFileThatIsNeitherAWalletNorADump() throws Exception {
		final Path file = Files.writeString(rig.keepDumpElsewhere().resolveSibling("notes.txt"), "no keys here");
		final Screen screen = pick(file);

		assertTrue(screen.find(CARD + " .step__error").text().endsWith("line 1 holds no private key"));
		assertTrue(screen.find(CONTINUE).hasAttr("disabled"));
	}
}
