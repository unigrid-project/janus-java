/*
	The Janus Wallet
	Copyright © 2021 The Unigrid Foundation

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.bootstrap;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import net.jqwik.api.Example;
import org.awaitility.Awaitility;
import org.unigrid.bootstrap.jqwik.BaseMockedWeldTest;
import org.unigrid.bootstrap.jqwik.WeldSetup;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

@WeldSetup(UpdateView.class)
public class UpdateViewTest extends BaseMockedWeldTest {

	@Inject
	private UpdateView updateView;

	@Mocked
	Stage primaryStage;

	@Example
	public boolean shouldDownloadDependencies() throws IOException {
		Platform.startup(() -> { /* Empty on purpose */ });
		URL configUrl = UpdateViewTest.class.getResource("config.xml");
		String libFolder = System.getProperty("user.dir").concat("/target/update-dependencies/temp/lib");
		String binFolder = System.getProperty("user.dir").concat("/target/update-dependencies/temp/bin");
		File libMkDirFile = new File(libFolder);
		File binMkDirFile = new File(binFolder);
		libMkDirFile.mkdirs();
		binMkDirFile.mkdirs();
		Configuration config = Configuration
			.read(new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8));

		new MockUp<FileMetadata>() {
			@Mock
			boolean requiresUpdate() {
				return true;
			}
		};

		new MockUp<Configuration>() {
			@Mock
			Configuration read() throws IOException {
				return Configuration
					.read(new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8));

			}
		};

		new MockUp<UpdateView>() {
			@Mock
			String getBaseDirectory() {
				return System.getProperty("user.dir").concat("/target/update-dependencies/temp");
			}
		};

		new MockUp<UpdateView>() {
			@Mock
			void removeOldJars() {
				updateView.removeOldJars(config);
			}
		};

		new MockUp<UpdateView>() {
			@Mock
			void launchApp() {
			}
		};

		new Expectations() {
			{
				primaryStage.getScene().lookup("#status");
				result = new Label();
				primaryStage.getScene().lookup("#progress");
				result = new ProgressBar();
				primaryStage.getScene().lookup("#quit");
				result = new Button();
				primaryStage.getScene().lookup("#launch");
				result = new Button();
			}
		};

		updateView.setConfig(config, primaryStage, new HashMap<>(), null);
		updateView.update();

		long dependencyFileSize = 60713;
		File file = new File(System.getProperty("user.dir")
			.concat("/target/update-dependencies/temp/lib/slf4j-api-2.0.0-alpha7.jar"));

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return file.length() == dependencyFileSize;
		});

		return file.length() == dependencyFileSize;
	}
}
