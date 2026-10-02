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

/** Importing a wallet dump, the text file the legacy daemon's dumpwallet writes, from the file choice to the dashboard. */
public class DumpFlowTest {
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String CONTINUE = "[hx-post=/action/open-wallet]";

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

	@Example
	public void shouldGoFromAPickedDumpToTheFundsOfItsKeys() throws Exception {
		rig.hedgehog().address(rig.dumpAddresses().get(0), "30", entry("aa", 1, "30", EntryKind.RECEIVED))
			.address(rig.dumpAddresses().get(5), "12", entry("bb", 2, "12", EntryKind.RECEIVED));

		final Screen screen = pick(rig.keepDumpElsewhere());

		assertTrue(screen.find(CHOOSE_FILE).hasClass("choice--selected"));
		assertFalse(screen.find(CONTINUE).hasAttr("disabled"));
		assertEquals("42.00", settle(screen.click(CONTINUE)).find(".dashboard__total").text());
	}

	@Example
	public void shouldKeepACopyOfTheDumpPickedAndOpenOnItNextTime() throws Exception {
		rig.hedgehog().address(rig.dumpAddresses().get(2), "7", entry("cc", 3, "7", EntryKind.RECEIVED));

		final Screen screen = pick(rig.keepDumpElsewhere());

		assertEquals(1, Files.list(rig.backups()).count());
		settle(screen.click(CONTINUE));
		assertEquals("7.00", settle(Screen.open(rig)).find(".dashboard__total").text());
	}

	@Example
	public void shouldRefuseAFileThatIsNeitherAWalletNorADump() throws Exception {
		final Path file = Files.writeString(rig.keepDumpElsewhere().resolveSibling("notes.txt"), "no keys here");
		final Screen screen = settle(pick(file).click(CONTINUE));

		final String shown = screen.document().text();

		assertTrue(shown.contains("is not a wallet.dat Janus can read"), shown);
		assertEquals(1, screen.document().select("[hx-post=/action/choose-another]").size(), shown);
	}
}
