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

package org.unigrid.janus.shell;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JanusTest {
	/* What a log line starts with, so that plain output can be told from what went through the logger. */
	private static final String LOG_PREFIX = "^\\d{2}:\\d{2}:\\d{2}\\.\\d{3} .*";

	private final StringWriter out = new StringWriter();
	private final StringWriter err = new StringWriter();
	private CommandLine command;

	@BeforeEach
	public void captureWhatJanusPrints() {
		command = Janus.command();
		command.setOut(new PrintWriter(out));
		command.setErr(new PrintWriter(err));
	}

	@Test
	public void shouldPrintTheHelpPlainlyAndNotStart() {
		assertTrue(Janus.options(command, "--help").isEmpty(), "Janus would start after showing the help");
		assertTrue(out.toString().startsWith("Usage: Unigrid"), out.toString());
		assertFalse(out.toString().lines().anyMatch(line -> line.matches(LOG_PREFIX)), out.toString());
	}

	@Test
	public void shouldRefuseAnOptionItDoesNotTakeAndSayWhyPlainly() {
		final CommandLine.ParameterException refused = assertThrows(CommandLine.ParameterException.class,
			() -> Janus.options(command, "--bogus")
		);

		Janus.refuse(command, refused);
		assertTrue(err.toString().startsWith(refused.getMessage()), err.toString());
		assertTrue(err.toString().contains("--bogus"), err.toString());
		assertTrue(err.toString().contains("Usage: Unigrid"), err.toString());
	}

	/* Oriel reads the window system on every desktop, even where it hides it from the help, so this holds anywhere. */
	@Test
	public void shouldRefuseAWindowSystemOrielDoesNotKnow() {
		final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> Janus.options(command, "--window-system=bogus")
		);

		assertTrue(refused.getMessage().contains("bogus"), refused.getMessage());
	}
}
