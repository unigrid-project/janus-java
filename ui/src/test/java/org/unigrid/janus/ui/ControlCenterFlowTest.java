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
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ControlCenterFlowTest {
	private static final String CARD = "main > .card";
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String USE_FOUND = "[hx-post=/action/import-found]";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String BACK = "[hx-post=/action/welcome]";
	private static final String CONTINUE = ".button--primary";
	private static final String PRIMARY = "choice--primary";
	private static final String SELECTED = "choice--selected";

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
	public void shouldOpenOnTheWelcomeCard() throws Exception {
		final Screen screen = Screen.open(rig);

		assertEquals(200, screen.status());
		assertEquals(ControlCenterRig.TITLE, screen.document().title());
		assertEquals("Welcome to Unigrid", screen.find(CARD + " h1").text());
	}

	/* A path nothing is kept at is answered with the page itself, so an asset that went missing
	   only shows as HTML arriving where something else was asked for. */
	@Example
	public void shouldServeEverythingThePageAsksFor() throws Exception {
		final Screen screen = Screen.open(rig);

		assertAll(screen.document().select("script[src], link[href], img[src]").stream().map(asset -> () -> {
			final String path = asset.hasAttr("src") ? asset.attr("src") : asset.attr("href");
			final HttpResponse<String> response = screen.client().get(path);

			assertEquals(200, response.statusCode(), path);
			assertFalse(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"), path);
		}));
	}

	@Example
	public void shouldOfferTheWalletLeftBehind() throws Exception {
		final Path wallet = rig.leaveWalletBehind();
		final Screen screen = Screen.open(rig).click(IMPORT);

		assertEquals(wallet.toString(), screen.find(USE_FOUND + " .choice__note").text());
		assertTrue(screen.find(USE_FOUND).hasClass(PRIMARY));
		assertEquals(2, litSteps(screen));
	}

	@Example
	public void shouldSayWhereItLookedWhenNothingIsThere() throws Exception {
		final Screen screen = Screen.open(rig).click(IMPORT);

		assertTrue(screen.document().select(USE_FOUND).isEmpty());
		assertEquals(rig.data().toString(), screen.find(CARD + " .step__note code").text());
		assertTrue(screen.find(CHOOSE_FILE).hasClass(PRIMARY));
	}

	@Example
	public void shouldCopyTheWalletLeftBehindOnceItIsUsed() throws Exception {
		rig.leaveWalletBehind();

		final Screen screen = Screen.open(rig).click(IMPORT).click(USE_FOUND);

		assertTrue(screen.find(USE_FOUND).hasClass(SELECTED));
		assertEquals(1, backups().size());
		assertEquals(backups().get(0).toString(), screen.find(CARD + " > .step__note code").text());
		assertFalse(screen.find(CONTINUE).hasAttr("disabled"));
	}

	@Example
	public void shouldTakeTheFileThatWasPicked() throws Exception {
		final Path wallet = rig.keepWalletElsewhere();
		final Screen screen = Screen.open(rig).click(IMPORT).trigger(CHOOSE_FILE, Map.of("path", wallet.toString()));

		assertTrue(screen.find(CHOOSE_FILE).hasClass(SELECTED));
		assertEquals(wallet.toString(), screen.find(CHOOSE_FILE + " .choice__note").text());
		assertEquals(1, backups().size());
	}

	@Example
	public void shouldLeaveTheCardAsItWasWhenNoFileIsNamed() throws Exception {
		final Screen screen = Screen.open(rig).click(IMPORT);
		final String before = screen.find(CARD).outerHtml();

		screen.trigger(CHOOSE_FILE, Map.of());
		assertEquals(500, screen.status());
		assertEquals(before, screen.find(CARD).outerHtml());
		assertTrue(screen.find(CONTINUE).hasAttr("disabled"));
	}

	@Example
	public void shouldGoBackToTheWelcomeCard() throws Exception {
		final Screen screen = Screen.open(rig).click(IMPORT).click(BACK);

		assertEquals("Welcome to Unigrid", screen.find(CARD + " h1").text());
		assertEquals(1, screen.document().select(CARD).size());
		assertEquals(1, litSteps(screen));
	}

	/* An action nobody registered is answered 404, and htmx then leaves the card as it was, so a
	   control named differently from its controller would look merely unresponsive. */
	@Example
	public void shouldKnowEveryActionThePagesCanSend() throws Exception {
		rig.leaveWalletBehind();

		final Screen screen = Screen.open(rig);
		final Set<String> sent = new HashSet<>();
		final Deque<Element> reached = new ArrayDeque<>(List.of(screen.document()));

		while (!reached.isEmpty()) {
			for (final Element control : reached.pop().select("[hx-post]")) {
				final String action = control.attr("hx-post");

				if (sent.add(action)) {
					final HttpResponse<String> response = screen.client().submit(action, "");

					assertNotEquals(404, response.statusCode(), action);
					reached.push(Jsoup.parseBodyFragment(response.body()));
				}
			}
		}

		assertTrue(sent.contains("/action/import-found"), sent::toString);
	}

	/* A command the host does not have is answered with the page and a 200, so only the host
	   hearing about it shows that the name on the control is one it knows. */
	@Example
	public void shouldReachTheWindowFromEveryControlOnThePage() throws Exception {
		final Screen screen = Screen.open(rig);
		final List<String> commands = new ArrayList<>(List.of("move/start", "move/end"));

		screen.document().select("[data-window]").forEach(control -> commands.add(control.attr("data-window")));
		screen.document().select("[data-resize]").forEach(handle -> {
			commands.add("resize/start/" + handle.attr("data-resize"));
			commands.add("resize/end");
		});

		for (final String command : commands) {
			assertEquals(204, screen.client().post("/window/" + command).statusCode(), command);
		}

		assertEquals(commands, rig.window().commands());
	}

	private List<Path> backups() throws IOException {
		try (Stream<Path> files = Files.list(rig.backups())) {
			return files.toList();
		}
	}

	private static int litSteps(final Screen screen) {
		return screen.document().select(CARD + " .steps__dot--lit").size();
	}
}
