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

package org.unigrid.janus.core;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/** The build this wallet was made from. */
@Slf4j
@ApplicationScoped
public class Release {
	private static final String FILE = "/org/unigrid/janus/core/application.properties";
	private static final String UNKNOWN = "unknown";

	private final String file;

	public Release() {
		this(FILE);
	}

	Release(final String file) {
		this.file = file;
	}

	public String version() {
		final Properties properties = new Properties();

		/* Read through the class rather than the class loader, so the file is still found
		   once the modules carry descriptors and resources stop being visible globally. */
		try (InputStream in = Release.class.getResourceAsStream(file)) {
			if (in == null) {
				return UNKNOWN;
			}

			properties.load(in);
			return properties.getProperty("version", UNKNOWN);
		} catch (final IOException e) {
			log.warn("Could not read {}", file, e);
			return UNKNOWN;
		}
	}
}
