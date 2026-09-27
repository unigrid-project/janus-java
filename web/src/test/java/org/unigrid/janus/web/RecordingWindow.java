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

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A host that remembers every command it was given, in the words the page uses for them, and
 * answers a file dialog with whatever file it was told the person picked. Commands arrive on the
 * server's threads, so a caller that has to wait for them does so with {@link #await}.
 */
public class RecordingWindow implements WindowControl {
	private static final long POLL_MILLIS = 10;

	private final List<String> commands = new CopyOnWriteArrayList<>();
	private volatile Path picked;

	public void picking(final Path file) {
		picked = file;
	}

	public List<String> commands() {
		return List.copyOf(commands);
	}

	/** The commands so far, once there are at least as many as expected or the time is up. */
	public List<String> await(final int count, final Duration timeout) throws InterruptedException {
		final Instant deadline = Instant.now().plus(timeout);

		while (commands.size() < count && Instant.now().isBefore(deadline)) {
			Thread.sleep(POLL_MILLIS);
		}

		return commands();
	}

	@Override
	public Optional<Path> chooseFile(final String title) {
		commands.add("choose-file:" + title);
		return Optional.ofNullable(picked);
	}

	@Override
	public Optional<Path> saveFile(final String title, final String name) {
		commands.add("save-file:" + title + ":" + name);
		return Optional.ofNullable(picked);
	}

	@Override
	public void minimise() {
		commands.add("minimise");
	}

	@Override
	public void toggleMaximise() {
		commands.add("maximise");
	}

	@Override
	public void close() {
		commands.add("close");
	}

	@Override
	public void beginMove() {
		commands.add("move/start");
	}

	@Override
	public void endMove() {
		commands.add("move/end");
	}

	@Override
	public void beginResize(final Edge edge) {
		commands.add("resize/start/" + edge.pathName());
	}

	@Override
	public void endResize() {
		commands.add("resize/end");
	}
}
