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

package org.unigrid.janus.model.entity;

import io.sentry.Sentry;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import org.unigrid.janus.Janus;
import org.update4j.OS;

public class InitSentry {
	public static void init(String s) {
		Properties myProperties = new Properties();

		try {
			myProperties.load(Janus.class.getResourceAsStream("application.properties"));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
		}

		String fullVer = Objects.requireNonNull((String) myProperties.get("proj.ver"));
		String filteredVer = fullVer.replace("-SNAPSHOT", "");

		final String env = s.equals("") ? "test" : "production";
		Sentry.init(options -> {
			options.setDsn("https://18a30d2bf41643ce9efe84a451ecef1a@o266736.ingest.sentry.io/6632466");
			options.setRelease(filteredVer);
			options.setServerName("unigrid");
			options.setTag("os", OS.CURRENT.getShortName());
			options.setEnvironment(env);
			//TODO: lower this number when using prerformens metrics using sentry. 1.0 is 100%
			options.setTracesSampleRate(1.0);
			options.setDebug(false);
		});
	}
}
