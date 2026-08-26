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

import java.net.URI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class UiServer {
	/* The wallet holds keys, so the interface is never reachable from off the machine.
	   Port zero keeps it off a predictable address that another local process could
	   guess ahead of time. */
	private static final String LOOPBACK = "127.0.0.1";
	private static final int EPHEMERAL = 0;

	private final Server server = new Server();
	private final ServerConnector connector = new ServerConnector(server);

	public UiServer(final Handler handler) {
		connector.setHost(LOOPBACK);
		connector.setPort(EPHEMERAL);
		server.addConnector(connector);
		server.setHandler(handler);
	}

	public URI start() throws Exception {
		server.start();
		return URI.create("http://" + LOOPBACK + ":" + connector.getLocalPort() + "/");
	}

	public void stop() throws Exception {
		server.stop();
	}

	public void join() throws InterruptedException {
		server.join();
	}
}
