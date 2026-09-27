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

package org.unigrid.janus.ui.view;

import java.util.List;
import net.jqwik.api.Example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.unigrid.janus.ui.view.AppView.Tab;
import org.unigrid.janus.ui.view.PreparingView.Step;
import org.unigrid.janus.ui.view.PreparingView.Step.State;
import org.unigrid.janus.web.Templates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppViewsTest {
	private static final List<Step> STARTING = List.of(new Step("Starting Hedgehog", State.ACTIVE),
		new Step("Downloading the legacy ledger (~314 MB)", State.PENDING),
		new Step("Reading wallet history", State.PENDING)
	);

	private final Templates templates = new Templates(false);

	private Document render(final AppView view) {
		return Jsoup.parseBodyFragment(templates.render(view));
	}

	@Example
	public void shouldPollWhilePreparingAndHoldTheTabsBack() {
		final Document html = render(new AppView(Tab.DASHBOARD, "PREPARING", true,
			new PreparingView(STARTING, null, false)
		));

		assertEquals("/action/wallet-status", html.selectFirst("#app").attr("hx-post"));
		assertEquals("every 2s", html.selectFirst("#app").attr("hx-trigger"));
		assertTrue(html.selectFirst("[hx-post=/action/dashboard]").hasAttr("disabled"));
		assertEquals("PREPARING", html.selectFirst(".app__chip").text());
		assertEquals(List.of("active", "pending", "pending"), html.select(".preparing__step").stream()
			.map(step -> step.className().replace("preparing__step preparing__step--", "")).toList()
		);
		assertTrue(html.select("[hx-post=/action/wallet-retry]").isEmpty());
	}

	@Example
	public void shouldStopPollingOnceThereIsSomethingToShow() {
		final Document html = render(new AppView(Tab.ACTIVITY, "LEDGER FROZEN", false,
			new PreparingView(STARTING, "Hedgehog is not installed on this computer", false)
		));

		assertFalse(html.selectFirst("#app").hasAttr("hx-trigger"));
		assertFalse(html.selectFirst("#app").hasAttr("hx-post"));
		assertEquals("this", html.selectFirst("#app").attr("hx-target"));
		assertTrue(html.selectFirst("[hx-post=/action/activity]").hasClass("app__tab--active"));
		assertFalse(html.selectFirst("[hx-post=/action/activity]").hasAttr("disabled"));
	}

	@Example
	public void shouldOfferARetryAndAnotherWalletOnlyWhenTheWalletWasTheProblem() {
		final Document retryOnly = render(new AppView(Tab.DASHBOARD, "PREPARING", false,
			new PreparingView(STARTING, "Hedgehog stopped answering", false)
		));
		final Document both = render(new AppView(Tab.DASHBOARD, "PREPARING", false,
			new PreparingView(STARTING, "wallet.dat is not a wallet.dat Janus can read", true)
		));

		assertEquals("Hedgehog stopped answering", retryOnly.selectFirst(".preparing__failure").text());
		assertEquals(1, retryOnly.select("[hx-post=/action/wallet-retry]").size());
		assertTrue(retryOnly.select("[hx-post=/action/choose-another]").isEmpty());
		assertEquals(1, both.select("[hx-post=/action/choose-another]").size());
	}

	@Example
	public void shouldShowTheLaterTabsWithoutLettingThemBeUsed() {
		final Document html = render(new AppView(Tab.DASHBOARD, "LEDGER FROZEN", false,
			new PreparingView(STARTING, null, false)
		));

		assertEquals(List.of("Gridnodes", "Domains", "Proxy", "Settings"),
			html.select(".app__tab[aria-disabled=true]").eachText()
		);
		assertTrue(html.select(".app__tab[aria-disabled=true][hx-post]").isEmpty());
	}
}
