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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.ui.ControlCenterRig;
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

	protected ControlCenterRig rig() {
		return rig;
	}

	protected Page page() {
		return page;
	}
}
