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

package org.unigrid.janus.model.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.unigrid.janus.model.UpdateURL;
import org.unigrid.janus.model.cdi.Eager;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.OS;

@Eager
@ApplicationScoped
public class Hedgehog {

	private Configuration config = null;

	private String hedgehogExecName = "hedgehog";

	private Process p;

	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][] {
		{OS.LINUX, UpdateURL.getLinuxUrl()},
		{OS.WINDOWS, UpdateURL.getWindowsUrl()},
		{OS.MAC, UpdateURL.getMacUrl()}
	});

	@PostConstruct
	@SneakyThrows
	private void init() {
		URL configURL = new URL(OS_CONFIG.get(OS.CURRENT).toString());
		Reader in = new InputStreamReader(configURL.openStream(), StandardCharsets.UTF_8);
		config = Configuration.read(in);
		List<FileMetadata> files = config.getFiles();
		for (FileMetadata file : files) {
			String name = file.getPath().getFileName().toString();
			if (name.contains("hedgehog")) {
				hedgehogExecName = file.getPath().toString();
				System.out.println(hedgehogExecName);
				if (!file.getPath().toFile().canExecute()) {
					file.getPath().toFile().setExecutable(true);
				}
			}
		}
	}

	@SneakyThrows
	public void startHedgehog() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(hedgehogExecName, "daemon");
		p = pb.start();
		//p.waitFor(10, TimeUnit.SECONDS);
		connectToHedgehog();
	}

	public boolean connectToHedgehog() {
		String uri = "https://127.0.0.1:52884/gridspork";
		Client client = ClientBuilder.newClient();
		int statusCode = 0;

		while (statusCode == 200) {
			Response response = client.target(uri).request().get();
			statusCode = response.getStatus();
		}

		return true;
	}

	public Process getProcess() {
		return p;
	}

	@SneakyThrows
	public void stopHedgehog() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(hedgehogExecName, "cli", "stop");
		pb.start();
	}
}
