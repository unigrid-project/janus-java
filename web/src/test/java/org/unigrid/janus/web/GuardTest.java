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
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuardTest extends ServedTest {
	@Example
	public void shouldRefuseARequestWithoutTheToken() throws Exception {
		final HttpResponse<String> response = anonymous().get("/");

		assertEquals(403, response.statusCode());
	}

	@Example
	public void shouldRefuseAWrongToken() throws Exception {
		final HttpResponse<String> response = anonymous().get("/?t=not-the-token");

		assertEquals(403, response.statusCode());
	}

	@Example
	public void shouldExchangeTheQueryTokenForACookie() throws Exception {
		final Client client = anonymous();
		final HttpResponse<String> bootstrap = client.get("/?" + SessionToken.PARAMETER + "=" + token().value());

		assertEquals(200, bootstrap.statusCode(), "the redirect should have been followed to the page");
		assertTrue(bootstrap.body().contains("<h1>Janus</h1>"), bootstrap.body());
		assertEquals(200, client.get("/").statusCode(), "the cookie should carry the next request");
	}

	@Example
	public void shouldRefuseARequestSentFromSomebodyElsesPage() throws Exception {
		final HttpResponse<String> response = admitted()
			.header("Origin", "https://unigrid.example")
			.get("/window/close");

		assertEquals(403, response.statusCode());
	}

	@Example
	public void shouldGuardTheAssetsToo() throws Exception {
		assertEquals(403, anonymous().get("/static/css/janus.css").statusCode());
		assertEquals(200, admitted().get("/static/css/janus.css").statusCode());
	}

	@Example
	public void shouldIssueADifferentTokenEachTime() {
		assertTrue(!SessionToken.random().value().equals(SessionToken.random().value()));
	}

	@Example
	public void shouldRejectAnAbsentCandidate() {
		assertTrue(!token().matches(null));
	}
}
