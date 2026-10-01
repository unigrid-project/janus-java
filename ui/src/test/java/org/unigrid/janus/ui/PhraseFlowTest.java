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

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.jsoup.nodes.Element;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhraseFlowTest {
	private static final String ABANDON = "abandon abandon abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon about";
	private static final String CARD = "main > .card";
	private static final String CREATE = "[hx-post=/action/create]";
	private static final String SAVED = "[hx-post=/action/create-verify]";
	private static final String CONFIRMED = "[hx-post=/action/create-password]";
	private static final String RESET = "[hx-post=/action/create-reset]";
	private static final String RESTORE = "[hx-post=/action/restore]";
	private static final String WORDS = "form[hx-post=/action/restore-words]";
	private static final String PASSWORD = "form[hx-post=/action/phrase-save]";
	private static final String ERROR = CARD + " .step__error";
	private static final String APP = "#app";

	private ControlCenterRig rig;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ControlCenterRig();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	@Example
	public void shouldShowTwelveNewWordsToWriteDown() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE);

		assertEquals("Your recovery phrase", screen.find(CARD + " h1").text());
		assertEquals(12, words(screen).size());
		assertEquals("01", screen.find(".phrase__n").text());
	}

	@Example
	public void shouldGoBackToTheWelcomeCard() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE).click("[hx-post=/action/create-back]");

		assertEquals("Welcome to Unigrid", screen.find(CARD + " h1").text());
	}

	@Example
	public void shouldConfirmThePhraseTappedInOrder() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE);
		final List<String> words = words(screen);

		screen.click(SAVED);
		assertTrue(screen.find(CONFIRMED).hasAttr("disabled"));

		for (final String word : words) {
			tap(screen, word);
		}

		assertEquals("✓ confirmed", screen.find(".verify__status").text());
		assertEquals("Choose a password", screen.click(CONFIRMED).find(CARD + " h1").text());
	}

	@Example
	public void shouldNotConfirmThePhraseTappedOutOfOrderUntilReset() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE);
		final List<String> reversed = new ArrayList<>(words(screen));

		Collections.reverse(reversed);
		screen.click(SAVED);

		for (final String word : reversed) {
			tap(screen, word);
		}

		assertEquals("12/12", screen.find(".verify__status").text());
		assertTrue(screen.find(CONFIRMED).hasAttr("disabled"));

		screen.click(RESET);
		assertEquals("select below…", screen.find(".tray__hint").text());
		assertTrue(screen.document().select(".tile[disabled]").isEmpty());
	}

	@Example
	public void shouldGoBackFromVerifyingToTheSameWords() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE);
		final List<String> words = words(screen);

		tap(screen.click(SAVED), words.get(0));
		screen.click("[hx-post=/action/create-seed]");
		assertEquals(words, words(screen));

		screen.click(SAVED);
		assertEquals("select below…", screen.find(".tray__hint").text());
	}

	@Example
	public void shouldSealTheNewWalletAndOpenIt() throws Exception {
		final Screen screen = Screen.open(rig).click(CREATE);
		final List<String> words = words(screen);

		screen.click(SAVED);

		for (final String word : words) {
			tap(screen, word);
		}

		screen.click(CONFIRMED).submit(PASSWORD, Map.of("password", "short", "repeat", "short"));
		assertEquals("Use at least 8 characters", screen.find(ERROR).text());

		screen.submit(PASSWORD, Map.of("password", "correct horse", "repeat", "correct hose"));
		assertEquals("The two passwords differ", screen.find(ERROR).text());

		screen.submit(PASSWORD, Map.of("password", "correct horse", "repeat", "correct horse"));
		screen.find(APP);

		final Path wallet = rig.chosen().remembered().orElseThrow();
		final Set<String> kept;

		try (JsonReader reader = Json.createReader(Files.newBufferedReader(wallet, StandardCharsets.UTF_8))) {
			kept = texts(reader.readValue()).collect(Collectors.toSet());
		}

		assertEquals(rig.wallets(), wallet.getParent());
		assertTrue(words.stream().noneMatch(kept::contains), kept.toString());
	}

	/* A word counts as kept only where it stands on its own in a text the file holds, not where a field happens
	   to be named after it or a string of hex digits happens to spell it. */
	private static Stream<String> texts(final JsonValue value) {
		return switch (value.getValueType()) {
			case OBJECT -> value.asJsonObject().values().stream().flatMap(PhraseFlowTest::texts);
			case ARRAY -> value.asJsonArray().stream().flatMap(PhraseFlowTest::texts);
			case STRING -> Arrays.stream(((JsonString) value).getString().split("\\s+"));
			default -> Stream.empty();
		};
	}

	@Example
	public void shouldRestoreAPhrasePastedIntoTheFirstBox() throws Exception {
		final Screen screen = Screen.open(rig).click("[hx-post=/action/import]").click(RESTORE);

		assertEquals("Enter recovery phrase", screen.find(CARD + " h1").text());

		screen.submit(WORDS, Map.of("word1", ABANDON));
		assertEquals("Restore wallet →", screen.find(PASSWORD + " [type=submit]").text());

		screen.submit(PASSWORD, Map.of("password", "correct horse", "repeat", "correct horse"));
		screen.find(APP);

		assertEquals(rig.wallets().resolve("evm-0x9858EfFD232B4033E47d90003D41EC34EcaEda94.json"),
			rig.chosen().remembered().orElseThrow()
		);
	}

	@Example
	public void shouldKeepWhatWasTypedWhenAWordIsWrong() throws Exception {
		final Screen screen = Screen.open(rig).click("[hx-post=/action/import]").click(RESTORE)
			.submit(WORDS, Map.of("word1", ABANDON.replace("about", "unigrid")));

		assertEquals("\"unigrid\" is not a word recovery phrases use", screen.find(ERROR).text());
		assertEquals("abandon", screen.find("[name=word1]").attr("value"));
		assertEquals("unigrid", screen.find("[name=word12]").attr("value"));
	}

	@Example
	public void shouldGoBackFromThePasswordToTheWordsTyped() throws Exception {
		final Screen screen = Screen.open(rig).click("[hx-post=/action/import]").click(RESTORE)
			.submit(WORDS, Map.of("word1", ABANDON)).click("[hx-post=/action/phrase-back]");

		assertEquals("about", screen.find("[name=word12]").attr("value"));
		assertEquals("Bring your wallet", screen.click("[hx-post=/action/restore-back]").find(CARD + " h1").text());
	}

	private static List<String> words(final Screen screen) {
		return screen.document().select(".phrase__word > span:not(.phrase__n)").eachText();
	}

	/* A word the phrase holds twice is two tiles, and either may be tapped first. */
	private static void tap(final Screen screen, final String word) throws Exception {
		final Element tile = screen.document().select("button.tile:not([disabled])").stream()
			.filter(candidate -> candidate.text().equals(word)).findFirst()
			.orElseThrow(() -> new AssertionError("No tile left for " + word));

		screen.click(tile.cssSelector());
	}
}
