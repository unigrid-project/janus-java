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

package org.unigrid.janus.web;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Example;
import org.eclipse.jetty.server.Handler;
import org.unigrid.janus.web.action.Action;
import org.unigrid.janus.web.action.Actions;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.Fragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionHandlerTest extends ServedTest {
	private final Wallet wallet = new Wallet();

	public class Wallet {
		private final List<String> unlocked = new ArrayList<>();

		@Action("lock-wallet")
		public void onClickLockWallet() {
			unlocked.clear();
		}

		@Action("unlock-wallet")
		public Fragment onClickUnlockWallet(final Form form) {
			unlocked.add(form.get("passphrase"));
			return Fragment.of("testing :: status", "holder", form.get("passphrase"));
		}

		@Action("break-wallet")
		public void onClickBreakWallet() {
			throw new IllegalStateException("the daemon said no");
		}

		public List<String> unlocked() {
			return unlocked;
		}
	}

	@Override
	protected Handler routes() {
		return Routes.create(templates(), token(), WindowControl.NONE, Actions.of(wallet));
	}

	@Example
	public void shouldCallTheAnnotatedMethod() throws Exception {
		assertEquals(204, admitted().post("/action/lock-wallet").statusCode());
	}

	@Example
	public void shouldHandTheFormToTheMethod() throws Exception {
		final HttpResponse<String> response = admitted()
			.submit("/action/unlock-wallet", "passphrase=open+sesame%21");

		assertEquals(200, response.statusCode());
		assertEquals(List.of("open sesame!"), wallet.unlocked());
	}

	@Example
	public void shouldRenderTheFragmentThatWasReturned() throws Exception {
		final HttpResponse<String> response = admitted()
			.submit("/action/unlock-wallet", "passphrase=x");

		assertTrue(response.body().contains("Janus is unlocked"), response.body());
		assertTrue(!response.body().contains("<!DOCTYPE html>"),
			"a fragment should be returned rather than the whole page: " + response.body()
		);
	}

	@Example
	public void shouldRefuseAnActionItDoesNotHave() throws Exception {
		assertEquals(404, admitted().post("/action/drain-wallet").statusCode());
	}

	@Example
	public void shouldNotActForAnUnknownCaller() throws Exception {
		assertEquals(403, anonymous().post("/action/lock-wallet").statusCode());
	}

	@Example
	public void shouldReportAFailedActionRatherThanPretendItWorked() throws Exception {
		assertEquals(500, admitted().post("/action/break-wallet").statusCode());
	}
}
