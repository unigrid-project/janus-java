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

package org.unigrid.janus.core.hedgehog;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Where the Hedgehog executable is on this computer. */
@ApplicationScoped
public class HedgehogLocation {
	public static final String PROPERTY = "janus.hedgehog";

	private final String configured;
	private final Path installation;
	private final String path;
	private final String name;

	public HedgehogLocation() {
		this(System.getProperty(PROPERTY), Optional.ofNullable(System.getProperty("jpackage.app-path"))
			.map(launcher -> Path.of(launcher).getParent()).orElse(null),
			Objects.requireNonNullElse(System.getenv("PATH"), ""), System.getProperty("os.name")
		);
	}

	HedgehogLocation(final String configured, final Path installation, final String path, final String os) {
		this.configured = configured;
		this.installation = installation;
		this.path = path;
		this.name = os.toLowerCase(Locale.ROOT).contains("win") ? "hedgehog.exe" : "hedgehog";
	}

	/* A configured executable is a deliberate choice, so when it is missing nothing else is tried. */
	public Optional<Path> find() {
		if (configured != null) {
			return Optional.of(Path.of(configured)).filter(Files::isExecutable);
		}

		return Stream.concat(Stream.ofNullable(installation), Arrays.stream(path.split(File.pathSeparator))
			.filter(entry -> !entry.isBlank()).map(Path::of))
			.map(folder -> folder.resolve(name)).filter(Files::isExecutable).findFirst();
	}
}
