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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.unigrid.janus.core.evm.Mnemonic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.ui.FlowSupport.tap;
import static org.unigrid.janus.ui.FlowSupport.words;

/** Making and restoring wallets from recovery phrases with the container wiring the controller, as the shell does. */
public class PhraseControllerIT {
	private static final String ABANDON = String.join(" ", Collections.nCopies(11, "abandon")) + " about";
	private static final String CARD = "main > .card";
	private static final String BOX_ERROR = ".step__error";
	private static final String ERROR = CARD + " " + BOX_ERROR;
	private static final String CREATE = "[hx-post=/action/create]";
	private static final String SAVED = "[hx-post=/action/create-verify]";
	private static final String CONFIRMED = "[hx-post=/action/create-password]";
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String RESTORE = "[hx-post=/action/restore]";
	private static final String WORDS = "form[hx-post=/action/restore-words]";
	private static final String PASSWORD = "form[hx-post=/action/phrase-save]";
	private static final String STATUS = ".verify__status";
	private static final String PASSWORDS = "password=correct+horse&repeat=correct+horse";
	private static final String SHORT = "Use at least 8 characters";

	private ContainerRig rig;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ContainerRig();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	private static int status(final Screen screen, final String action, final String form) throws Exception {
		return screen.client().submit("/action/" + action, form).statusCode();
	}

	private static Element asked(final Screen screen, final String action, final String form) throws Exception {
		return Jsoup.parseBodyFragment(screen.client().submit("/action/" + action, form).body()).body();
	}

	private Screen restoring() throws Exception {
		return rig.open().click(IMPORT).click(RESTORE);
	}

	private Screen confirmed() throws Exception {
		final Screen screen = rig.open().click(CREATE);
		final List<String> written = words(screen);

		screen.click(SAVED);

		for (final String word : written) {
			tap(screen, word);
		}

		return screen;
	}

	private static void restoredWith(final Screen screen, final String password) throws Exception {
		final String typed = "word1=" + URLEncoder.encode(ABANDON, StandardCharsets.UTF_8);
		final String sealed = "password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) + "&repeat="
			+ URLEncoder.encode(password, StandardCharsets.UTF_8);

		assertEquals(200, status(screen, "restore", ""));
		assertEquals(200, status(screen, "restore-words", typed));
		assertEquals(200, status(screen, "phrase-save", sealed));
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
	public void shouldRefuseEveryStepThatNeedsAPhraseWhileNoneIsInHand() throws Exception {
		final Screen screen = rig.open();

		for (final String action : List.of("create-seed", "create-verify", "create-reset", "create-password",
			"phrase-back")) {

			assertEquals(500, status(screen, action, ""), action);
		}

		assertEquals(500, status(screen, "create-pick", "index=0"));
		assertEquals(500, status(screen, "phrase-save", PASSWORDS));
		assertEquals(List.of(), savedWallets());
	}

	@Example
	public void shouldForgetThePhraseWhenTheFlowIsLeftInEitherDirection() throws Exception {
		final Screen screen = rig.open();

		for (final String leaving : List.of("create-back", "restore-back")) {
			assertEquals(200, status(screen, "create", ""));
			assertEquals(200, status(screen, "create-verify", ""));
			assertEquals(200, status(screen, leaving, ""), leaving);
			assertEquals(500, status(screen, "create-verify", ""), "after " + leaving);
			assertEquals(500, status(screen, "phrase-save", PASSWORDS), "after " + leaving);
		}
	}

	@Example
	public void shouldGiveNewWordsAndClearWhatWasPickedWhenCreatingAgain() throws Exception {
		final Screen screen = rig.open();
		final List<String> first = words(asked(screen, "create", ""));

		asked(screen, "create-verify", "");
		assertEquals("1/12", asked(screen, "create-pick", "index=0").select(STATUS).text());

		final List<String> second = words(asked(screen, "create", ""));

		assertEquals(12, second.size());
		assertNotEquals(first, second);
		assertEquals("select below…", asked(screen, "create-verify", "").select(".tray__hint").text());
	}

	@Example
	public void shouldTakeEachWordOnlyOnceAndOnlyOneThePhraseHolds() throws Exception {
		final Screen screen = rig.open().click(CREATE);

		screen.click(SAVED);
		assertEquals("1/12", asked(screen, "create-pick", "index=3").select(STATUS).text());
		assertEquals("1/12", asked(screen, "create-pick", "index=3").select(STATUS).text());
		assertEquals("1/12", asked(screen, "create-pick", "index=99").select(STATUS).text());
		assertEquals("1/12", asked(screen, "create-pick", "index=-1").select(STATUS).text());
		assertEquals(500, status(screen, "create-pick", "index=three"));
		assertEquals(500, status(screen, "create-pick", ""));
		assertEquals("1/12", asked(screen, "create-pick", "index=3").select(STATUS).text());
	}

	@Example
	public void shouldGoOnToThePasswordOnlyOnceThePhraseIsConfirmed() throws Exception {
		final Screen screen = rig.open().click(CREATE);

		screen.click(SAVED);
		assertEquals(500, status(screen, "create-password", ""));

		for (final String word : words(asked(screen, "create-seed", ""))) {
			tap(screen, word);
		}

		assertEquals(200, status(screen, "create-password", ""));
	}

	@Example
	public void shouldKeepWhatWasPickedWhenGoingBackFromThePassword() throws Exception {
		final Screen screen = confirmed().click(CONFIRMED);

		screen.click("[hx-post=/action/phrase-back]");
		assertEquals("✓ confirmed", screen.find(STATUS).text());
	}

	@Example
	public void shouldShowWhatTheParserSaysOfAPhraseThatCannotBeRestored() throws Exception {
		final List<String> typedPhrases = List.of(ABANDON.substring(0, ABANDON.length() - " about".length()), "",
			"abandon ".repeat(12), ABANDON + " about", ABANDON.replace("about", "zebra")
		);

		for (final String typed : typedPhrases) {
			final String reason = assertThrows(IllegalArgumentException.class,
				() -> Mnemonic.parse(typed)
			).getMessage();
			final Screen screen = restoring().submit(WORDS, Map.of("word1", typed));

			assertEquals(reason, screen.find(ERROR).text(), "for '" + typed + "'");
		}
	}

	@Example
	public void shouldRestoreWordsTypedOnePerBox() throws Exception {
		final String[] typed = ABANDON.split(" ");
		final Map<String, String> boxes = IntStream.range(0, typed.length).boxed()
			.collect(Collectors.toMap(n -> "word" + (n + 1), n -> typed[n]));
		final Screen screen = restoring().submit(WORDS, boxes);

		assertEquals("Restore wallet →", screen.find(PASSWORD + " [type=submit]").text());
	}

	@Example
	public void shouldRestoreWordsWhateverTheirCaseAndSpacing() throws Exception {
		final Screen screen = restoring().submit(WORDS, Map.of("word1", "  " + ABANDON.toUpperCase() + " \t "));

		assertEquals("Restore wallet →", screen.find(PASSWORD + " [type=submit]").text());
	}

	@Example
	public void shouldTakeAFormThatLeavesOutEveryBox() throws Exception {
		final Screen screen = restoring();

		assertEquals("A recovery phrase has 12 words, not 1",
			asked(screen, "restore-words", "").select(BOX_ERROR).text()
		);
	}

	@Example
	public void shouldAskForAPasswordOfEightCharactersOnceRestoring() throws Exception {
		final Screen screen = restoring().submit(WORDS, Map.of("word1", ABANDON));

		screen.submit(PASSWORD, Map.of("password", "1234567", "repeat", "1234567"));
		assertEquals(SHORT, screen.find(ERROR).text());
		assertEquals("Restore wallet →", screen.find(PASSWORD + " [type=submit]").text());

		assertEquals(SHORT, asked(screen, "phrase-save", "").select(BOX_ERROR).text());
		assertEquals("The two passwords differ",
			asked(screen, "phrase-save", "password=12345678").select(BOX_ERROR).text()
		);
		assertEquals(List.of(), savedWallets());

		screen.submit(PASSWORD, Map.of("password", "12345678", "repeat", "12345678"));
		assertEquals(1, savedWallets().size());
	}

	@Example
	public void shouldForgetThePhraseOnceItIsSealedAndSaved() throws Exception {
		final Screen screen = restoring().submit(WORDS, Map.of("word1", ABANDON));

		screen.submit(PASSWORD, Map.of("password", "correct horse", "repeat", "correct horse"));
		assertEquals(500, status(screen, "phrase-save", PASSWORDS));
		assertEquals(500, status(screen, "create-verify", ""));
		assertEquals(1, savedWallets().size());
	}

	@Example
	public void shouldKeepThePhraseWhenTheWalletCannotBeSavedAndSaveItOnceItCan() throws Exception {
		final Path blocker = Files.createDirectories(rig.wallets().getParent()).resolve("wallets");
		final Screen screen = restoring().submit(WORDS, Map.of("word1", ABANDON));

		Files.createFile(blocker);
		assertEquals(500, status(screen, "phrase-save", PASSWORDS));
		Files.delete(blocker);

		assertEquals(200, status(screen, "phrase-save", PASSWORDS));
		assertEquals(1, savedWallets().size());
	}

	@Example
	public void shouldReplaceAWalletRestoredAgainWithTheLastPasswordGiven() throws Exception {
		final Screen screen = rig.open();

		restoredWith(screen, "first password");

		final Path wallet = savedWallets().get(0);
		final String first = Files.readString(wallet);

		restoredWith(screen, "second password");

		assertEquals(List.of(wallet), savedWallets());
		assertNotEquals(first, Files.readString(wallet));
		assertTrue(savedWallets().stream().noneMatch(file -> file.toString().endsWith(".part")));
	}
}
