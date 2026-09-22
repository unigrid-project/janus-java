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

package org.unigrid.janus.ui.controller;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletBackup;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.web.action.ActionExtension;
import org.unigrid.janus.web.action.Actions;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.View;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportControllerTest {
	private Path directory;
	private Path elsewhere;
	private Path backups;
	private WalletChoice choice;
	private ImportController controller;

	@BeforeTry
	public void prepareAnEmptyDataDirectory() throws IOException {
		directory = Files.createTempDirectory("janus");
		elsewhere = Files.createTempFile("backup", ".dat");
		backups = Files.createTempDirectory("backups");
		choice = new WalletChoice(new WalletBackup(backups, Clock.systemUTC()));
		controller = new ImportController(new DataDirectory(directory), choice);
	}

	@AfterTry
	public void removeIt() throws IOException {
		Files.deleteIfExists(directory.resolve("wallet.dat"));
		Files.delete(directory);
		Files.delete(elsewhere);

		try (Stream<Path> paths = Files.walk(backups)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	private Path leaveAWalletBehind() throws IOException {
		return Files.createFile(directory.resolve("wallet.dat"));
	}

	@Example
	public void shouldOfferTheWalletFoundOnThisComputer() throws IOException {
		final Path wallet = leaveAWalletBehind();
		final ImportView view = controller.onClickImport();

		assertEquals(wallet, view.found());
		assertTrue(view.hasFound());
		assertFalse(view.hasChosen());
		assertEquals("fragments/import :: import", view.template());
	}

	@Example
	public void shouldSayWhereItLookedWhenNothingIsThere() {
		final ImportView view = controller.onClickImport();

		assertEquals(directory, view.directory());
		assertFalse(view.hasFound());
	}

	@Example
	public void shouldShowThatTheWalletIsGoneWhenItWasUsedTooLate() throws IOException {
		Files.delete(leaveAWalletBehind());

		final ImportView view = controller.onClickUseFound();

		assertFalse(view.hasFound());
		assertEquals(Optional.empty(), choice.chosen());
	}

	@Example
	public void shouldRememberTheFoundWalletWhenItIsUsed() throws IOException {
		final Path wallet = leaveAWalletBehind();
		final ImportView view = controller.onClickUseFound();

		assertEquals(Optional.of(wallet), choice.chosen());
		assertTrue(view.foundChosen());
		assertFalse(view.otherChosen());
	}

	@Example
	public void shouldRememberTheFileThatWasPicked() throws IOException {
		leaveAWalletBehind();

		final ImportView view = controller.onChooseFile(form(elsewhere));

		assertEquals(Optional.of(elsewhere), choice.chosen());
		assertEquals(elsewhere, view.chosen());
		assertTrue(view.otherChosen());
		assertFalse(view.foundChosen());
	}

	@Example
	public void shouldRefuseAPickedPathThatIsNotAFile() {
		assertThrows(IllegalArgumentException.class, () -> controller.onChooseFile(form(directory)));
		assertEquals(Optional.empty(), choice.chosen());
	}

	@Example
	public void shouldRefuseAPickThatNamesNoFile() {
		assertThrows(IllegalArgumentException.class, () -> controller.onChooseFile(Form.parse("")));
		assertEquals(Optional.empty(), choice.chosen());
	}

	@Example
	public void shouldBeFoundAndWiredByTheContainer() {
		try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
			final Actions actions = Actions.discovered(
				container.getBeanManager().getExtension(ActionExtension.class)
			);
			final View view = actions.invoke("import", Form.parse("")).orElseThrow();

			assertInstanceOf(ImportView.class, view);
			assertTrue(actions.knows("import-found"));
			assertTrue(actions.knows("import-file"));
		}
	}

	private static Form form(final Path path) {
		return Form.parse("path=" + URLEncoder.encode(path.toString(), StandardCharsets.UTF_8));
	}
}
