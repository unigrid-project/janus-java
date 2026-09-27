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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.unigrid.janus.web.WindowControl.Edge;

public class WindowHandler extends Handler.Abstract {
	private static final String RESIZE_START = "resize/start/";
	private static final String CHOOSE_FILE = "choose-file";
	private static final String SAVE_FILE = "save-file";
	private static final String NAME = "name";
	private static final String TITLE = "title";
	private static final String TEXT = "text/plain;charset=utf-8";

	private final WindowControl window;

	public WindowHandler(final WindowControl window) {
		this.window = window;
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) {
		final String command = Request.getPathInContext(request).substring(1);
		final Fields query = Request.extractQueryParameters(request);

		if (CHOOSE_FILE.equals(command)) {
			return answerWith(window.chooseFile(value(query, TITLE)), response, callback);
		}

		if (SAVE_FILE.equals(command)) {
			return answerWith(window.saveFile(value(query, TITLE), value(query, NAME)), response, callback);
		}

		if (!perform(command)) {
			return false;
		}

		response.setStatus(HttpStatus.NO_CONTENT_204);
		callback.succeeded();
		return true;
	}

	private static String value(final Fields query, final String name) {
		return Objects.requireNonNullElse(query.getValue(name), "");
	}

	private static boolean answerWith(final Optional<Path> chosen, final Response response, final Callback callback) {
		if (chosen.isEmpty()) {
			response.setStatus(HttpStatus.NO_CONTENT_204);
			callback.succeeded();
			return true;
		}

		response.setStatus(HttpStatus.OK_200);
		response.getHeaders().put(HttpHeader.CONTENT_TYPE, TEXT);
		response.write(true, ByteBuffer.wrap(chosen.get().toString().getBytes(StandardCharsets.UTF_8)), callback);
		return true;
	}

	private boolean perform(final String command) {
		if (command.startsWith(RESIZE_START)) {
			return beginResize(command.substring(RESIZE_START.length()));
		}

		switch (command) {
			case "minimise" -> window.minimise();
			case "maximise" -> window.toggleMaximise();
			case "close" -> window.close();
			case "move/start" -> window.beginMove();
			case "move/end" -> window.endMove();
			case "resize/end" -> window.endResize();
			default -> {
				return false;
			}
		}

		return true;
	}

	private boolean beginResize(final String name) {
		final Optional<Edge> edge = Edge.named(name);

		edge.ifPresent(window::beginResize);
		return edge.isPresent();
	}
}
