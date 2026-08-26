/*
    The Janus Wallet
    Copyright © 2021-2022 Stiftelsen The Unigrid Foundation

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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class UiHandler extends Handler.Abstract {
	private static final String HTML = "text/html;charset=utf-8";

	private final Templates templates;

	public UiHandler(final Templates templates) {
		this.templates = templates;
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) {
		final String html = templates.render("index", Map.of("title", "Janus"));
		final ByteBuffer body = ByteBuffer.wrap(html.getBytes(StandardCharsets.UTF_8));

		response.setStatus(HttpStatus.OK_200);
		response.getHeaders().put(HttpHeader.CONTENT_TYPE, HTML);
		response.write(true, body, callback);
		return true;
	}
}
