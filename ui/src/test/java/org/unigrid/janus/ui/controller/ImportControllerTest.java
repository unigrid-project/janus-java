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
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletBackup;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.ui.WalletFixture;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.web.action.ActionExtension;
import org.unigrid.janus.web.action.Actions;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.View;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportControllerTest {
	private static final String NEITHER = " is neither a wallet.dat nor a wallet dump Janus can read: "
		+ "it does not start like a Berkeley DB file, and as a dump ";
	private static final String DUMP = "# Wallet dump created by UNIGRID 2.9.17\n"
		+ "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx 2018-01-02T10:00:00Z label=\n"
		+ "68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J 2018-01-02T10:00:00Z reserve=1\n";

	private Path directory;
	private Path elsewhere;
	private Path backups;
	private WalletChoice choice;
	private ImportController controller;

	@BeforeTry
	public void prepareAnEmptyDataDirectory() throws IOException {
		directory = Files.createTempDirectory("janus");
		elsewhere = WalletFixture.copyTo(Files.createTempDirectory("backup").resolve("wallet.dat"));
		backups = Files.createTempDirectory("backups");
		choice = new WalletChoice(new WalletBackup(backups, Clock.systemUTC()));
		controller = new ImportController(new DataDirectory(directory), choice);
	}

	@AfterTry
	public void removeIt() throws IOException {
		Files.deleteIfExists(directory.resolve("wallet.dat"));
		Files.delete(directory);
		Files.delete(elsewhere);
		Files.delete(elsewhere.getParent());

		try (Stream<Path> paths = Files.walk(backups)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	private Path leaveAWalletBehind() throws IOException {
		return WalletFixture.copyTo(directory.resolve("wallet.dat"));
	}

	private Path pickADump(final String text) throws IOException {
		final Path dump = Files.writeString(Files.createTempFile("wallet", ".dump"), text);

		dump.toFile().deleteOnExit();
		return dump;
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
	public void shouldShowWhereTheChosenWalletWasCopied() throws IOException {
		final ImportView view = controller.onChooseFile(form(elsewhere));

		assertEquals(choice.backup().orElseThrow(), view.backup());
		assertEquals(backups, view.backup().getParent());
	}

	@Example
	public void shouldTakeAWalletDumpWithoutCopyingIt() throws IOException {
		final Path dump = pickADump(DUMP);
		final ImportView view = controller.onChooseFile(form(dump));

		assertEquals(Optional.of(dump), choice.chosen());
		assertEquals(dump, view.chosen());
		assertTrue(view.dumpChosen());
		assertNull(view.backup());
		assertNull(view.error());
		assertTrue(view.otherChosen());
		assertEquals(0, Files.list(backups).count());
	}

	@Example
	public void shouldNotCallAWalletFileADump() throws IOException {
		assertFalse(controller.onChooseFile(form(elsewhere)).dumpChosen());
	}

	@Example
	public void shouldSayWhatIsWrongWithADumpAndChooseNothing() throws IOException {
		final Path dump = pickADump(DUMP + "not-a-key 2018-01-02T10:00:00Z\n");
		final ImportView view = controller.onChooseFile(form(dump));

		assertEquals(dump + NEITHER + "line 4 holds no private key", view.error());
		assertFalse(view.hasChosen());
		assertEquals(Optional.empty(), choice.chosen());
	}

	@Example
	public void shouldSayWhenTheWalletFoundHereIsNeitherAWalletNorADump() throws IOException {
		final Path found = Files.writeString(directory.resolve("wallet.dat"), "this is no wallet");
		final ImportView view = controller.onClickUseFound();

		assertEquals(found + NEITHER + "line 1 holds no private key", view.error());
		assertFalse(view.hasChosen());
		assertEquals(Optional.empty(), choice.chosen());
	}

	@Example
	public void shouldRefuseMumboJumboOfWhateverSizeOrKindAndChooseNothing() throws IOException {
		final byte[] noise = new byte[5000];
		final byte[] text = "lorem ipsum dolor sit amet\n".repeat(200).getBytes(StandardCharsets.UTF_8);

		new Random(7).nextBytes(noise);

		for (final byte[] content : List.of(new byte[0], "x".getBytes(StandardCharsets.UTF_8), noise, text)) {
			final Path file = Files.write(Files.createTempFile("junk", ".bin"), content);
			final ImportView view = controller.onChooseFile(form(file));

			file.toFile().deleteOnExit();
			final String error = view.error();

			assertTrue(error.startsWith(file + " is neither a wallet.dat nor a wallet dump"), error);
			assertTrue(error.contains("does not start like a Berkeley DB file"), error);
			assertFalse(view.hasChosen());
		}

		assertEquals(Optional.empty(), choice.chosen());
		assertEquals(0, Files.list(backups).count());
	}

	@Example
	public void shouldSayWhyAWalletFileThatIsDamagedCannotBeRead() throws IOException {
		final Path folder = Files.createTempDirectory("damaged");
		final Path damaged = WalletFixture.damagedTo(folder.resolve("wallet.dat"));
		final ImportView view = controller.onChooseFile(form(damaged));

		damaged.toFile().deleteOnExit();
		assertEquals(damaged + " is not a wallet.dat Janus can read: it is cut short", view.error());
		assertFalse(view.hasChosen());
		assertEquals(0, Files.list(backups).count());
	}

	@Example
	public void shouldTakeAWalletFileWithoutTryingItAsADump() throws IOException {
		final ImportView view = controller.onChooseFile(form(elsewhere));

		assertNull(view.error());
		assertFalse(view.dumpChosen());
		assertEquals(1, Files.list(backups).count());
	}

	@Example
	public void shouldKeepTheEarlierChoiceWhenADumpIsRefused() throws IOException {
		controller.onChooseFile(form(elsewhere));

		final ImportView view = controller.onChooseFile(form(pickADump("no keys here")));

		assertNotNull(view.error());
		assertEquals(elsewhere, view.chosen());
		assertEquals(Optional.of(elsewhere), choice.chosen());
	}

	@Example
	public void shouldHandOverTheKeysOfTheDumpChosen() throws IOException {
		controller.onChooseFile(form(pickADump(DUMP)));

		final List<LegacyKey> keys = controller.dumpKeys();

		assertEquals(List.of("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH", "HS6ofefYfBjXjqaKM4pb54a1SEmAxvGKTi"),
			keys.stream().map(LegacyKey::address).toList()
		);
	}

	@Example
	public void shouldHaveNoKeysToHandOverWhenNoDumpWasChosen() throws IOException {
		assertThrows(IllegalStateException.class, () -> controller.dumpKeys());

		controller.onChooseFile(form(elsewhere));
		assertThrows(IllegalStateException.class, () -> controller.dumpKeys());
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
