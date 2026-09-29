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
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Where the Hedgehog executable is on this computer. One that is found is used only when it is the release
 * this Janus is made for, so a stale Hedgehog left on the computer is never run by mistake.
 */
@ApplicationScoped
public class HedgehogLocation {
	public static final String PROPERTY = "janus.hedgehog";

	private final String configured;
	private final Path installation;
	private final String path;
	private final String name;
	private final HedgehogRelease release;

	@Inject
	public HedgehogLocation(final HedgehogRelease release) {
		this(System.getProperty(PROPERTY), Optional.ofNullable(System.getProperty("jpackage.app-path"))
			.map(launcher -> Path.of(launcher).getParent()).orElse(null),
			Objects.requireNonNullElse(System.getenv("PATH"), ""), System.getProperty("os.name"), release
		);
	}

	HedgehogLocation(final String configured, final Path installation, final String path, final String os,
		final HedgehogRelease release) {

		this.configured = configured;
		this.installation = installation;
		this.path = path;
		this.name = os.toLowerCase(Locale.ROOT).contains("win") ? "hedgehog.exe" : "hedgehog";
		this.release = release;
	}

	/* A configured executable is a deliberate choice, so it is taken as it is and nothing else is tried. */
	public Optional<Path> find() {
		if (configured != null) {
			return Optional.of(Path.of(configured)).filter(Files::isExecutable);
		}

		return folders().map(folder -> folder.resolve(name)).filter(Files::isExecutable).filter(release::matches)
			.findFirst();
	}

	private Stream<Path> folders() {
		final Stream<Path> onPath = Arrays.stream(path.split(File.pathSeparator)).filter(entry -> !entry.isBlank())
			.map(Path::of);

		return Stream.of(Stream.ofNullable(installation), onPath, Stream.of(release.installation()))
			.flatMap(Function.identity());
	}
}
