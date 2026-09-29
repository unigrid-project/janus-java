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

import java.util.Locale;
import java.util.Optional;

/** The computers Hedgehog is released an executable for. */
enum Platform {
	LINUX("x86_64-linux-gnu.bin", "hedgehog"),
	MACOS("osx-arm64.bin", "hedgehog"),
	WINDOWS("win64.exe", "hedgehog.exe");

	private final String suffix;
	private final String executable;

	Platform(final String suffix, final String executable) {
		this.suffix = suffix;
		this.executable = executable;
	}

	static Optional<Platform> of(final String os, final String arch) {
		final String system = os.toLowerCase(Locale.ROOT);
		final boolean intel = arch.equals("amd64") || arch.equals("x86_64");
		final boolean arm = arch.equals("aarch64") || arch.equals("arm64");

		if (system.contains("linux") && intel) {
			return Optional.of(LINUX);
		}

		if (system.contains("mac") && arm) {
			return Optional.of(MACOS);
		}

		return system.contains("win") && intel ? Optional.of(WINDOWS) : Optional.empty();
	}

	/** The name the executable is kept under once installed. */
	String executable() {
		return executable;
	}

	/** The name of the executable among the assets of the release. */
	String asset(final String version) {
		return "hedgehog-" + version + "-" + suffix;
	}

	/** The name the platform goes by in the build's pinned checksums. */
	String key() {
		return name().toLowerCase(Locale.ROOT);
	}
}
