/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.unigrid.janus.controller.NodesController;

@Data
public class SSHConnection {
	private static final int MAX_OUTPUT_SIZE = 65535;
	private Session session;
	private ChannelExec channel;

	public static SSHConnection connect(InetSocketAddress address, String username, String password) throws JSchException {
		final SSHConnection sshService = new SSHConnection();
		final Session session = new JSch().getSession(username, address.getHostString(), address.getPort());

		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		sshService.setSession(session);

		return sshService;
	}

	public void send(String command, String commandType, Consumer<String> consumer) throws JSchException {
		channel = (ChannelExec) session.openChannel(commandType);
		channel.setCommand(command);
		channel.setOutputStream(new InterceptingOutputStream(consumer));
		channel.connect();
	}

	@RequiredArgsConstructor
	public class InterceptingOutputStream extends ByteArrayOutputStream {
		private final Consumer<String> consumer;

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			for (String line : new String(b, off, len).split("\n")) {
				consumer.accept(line);
			}
		}
	}

	public void close() {
		session.disconnect();	
	}

	public boolean isConnected() {
		return session.isConnected();
	}
}
