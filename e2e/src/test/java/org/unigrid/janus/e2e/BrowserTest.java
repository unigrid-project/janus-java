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

package org.unigrid.janus.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.LocatorAssertions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.ui.ControlCenterRig;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Control Center opened in headless Chromium, the engine the window embeds. The window only
 * ever reaches its host through requests, so a browser pointed at the served page runs the same
 * scripts the window does, without a frame opening on anybody's desktop.
 */
public abstract class BrowserTest {
	protected static final Duration WAIT = Duration.ofSeconds(5);

	/* Chromium reports the window's 204 answers as aborted requests even though the page received
	   them, so only the other failures say anything. */
	private static final String ABORTED = "net::ERR_ABORTED";
	private static final int FIRST_ERROR = 400;
	private static final int SEARCH_ATTEMPTS = 3;
	private static final double SEARCH_MILLIS = 4000;
	private static final int CLICK_ATTEMPTS = 3;
	private static final double CLICK_MILLIS = 5000;

	private static Playwright playwright;
	private static Browser browser;

	private final List<String> troubles = new CopyOnWriteArrayList<>();
	private ControlCenterRig rig;
	private BrowserContext context;
	private Page page;

	@BeforeContainer
	public static void launch() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch();
	}

	/* Closing Playwright closes its browsers too; it is missing only when it could not start, and
	   that failure is the one worth reporting. */
	@AfterContainer
	public static void quit() {
		if (playwright != null) {
			playwright.close();
		}
	}

	@BeforeTry
	public void open() throws Exception {
		rig = new ControlCenterRig();
		context = browser.newContext();
		page = context.newPage();

		page.onConsoleMessage(message -> {
			if ("error".equals(message.type())) {
				troubles.add(message.text());
			}
		});

		page.onPageError(error -> troubles.add(error.toString()));
		page.onResponse(response -> {
			if (response.status() >= FIRST_ERROR) {
				troubles.add(response.url() + " " + response.status());
			}
		});

		page.onRequestFailed(request -> {
			if (!ABORTED.equals(request.failure())) {
				troubles.add(request.url() + " " + request.failure());
			}
		});

		page.navigate(rig.entrance().toString());
	}

	/* A script that threw or an asset that never arrived would otherwise go unnoticed by any
	   test not looking at that exact spot. */
	@AfterTry
	public void close() throws Exception {
		context.close();
		rig.close();
		assertEquals(List.of(), troubles);
	}

	/*
	 * Types into the search box and waits for the list to follow. On a loaded runner the typing now and then
	 * leaves the list as it was, so it is tried again, and the page is printed when that is not enough.
	 */
	protected void searchFor(final String text, final int rows) {
		final Locator box = page().locator("input[name=q]");

		for (int attempt = 1; ; attempt++) {
			box.fill("");
			box.pressSequentially(text);

			try {
				assertThat(page().locator("#rows details"))
					.hasCount(rows, new LocatorAssertions.HasCountOptions().setTimeout(SEARCH_MILLIS));
				return;
			} catch (AssertionError e) {
				if (attempt == SEARCH_ATTEMPTS) {
					System.out.println("The list did not follow a search for '" + text + "':\n"
						+ page().locator("#app").innerHTML());
					throw e;
				}
			}
		}
	}

	/*
	 * Clicks and waits for the page to show what the click brings. A card is swapped for the next one by a
	 * request, and on a loaded runner a click now and then falls between two swaps and does nothing, so it is
	 * tried again; a button that really does nothing still fails, after the last try.
	 */
	protected void clickUntil(final String button, final String shown) {
		for (int attempt = 1; ; attempt++) {
			page().click(button);

			try {
				page().waitForSelector(shown, new Page.WaitForSelectorOptions().setTimeout(CLICK_MILLIS));
				return;
			} catch (TimeoutError e) {
				if (attempt == CLICK_ATTEMPTS) {
					System.out.println("Clicking " + button + " did not bring " + shown + ":\n"
						+ page().locator("#app, main").first().innerHTML());
					throw e;
				}
			}
		}
	}

	/** Chooses the wallet the legacy daemon left behind, which the import card finds, ready to be continued. */
	protected void pickTheWalletLeftBehind() {
		final String found = "[hx-post='/action/import-found']";

		clickUntil("[hx-post='/action/import']", found);
		clickUntil(found, found + ".choice--selected");
	}

	protected ControlCenterRig rig() {
		return rig;
	}

	protected Page page() {
		return page;
	}
}
