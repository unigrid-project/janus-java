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

package org.unigrid.janus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationAdapter;
import org.testfx.framework.junit5.ApplicationFixture;
import org.unigrid.janus.view.MainWindow;

public class FXApplicationTest extends FxRobot implements ApplicationFixture {

	public static void launch(Class<? extends Application> appClass, String... appArgs) throws Exception {
		FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(appClass, appArgs);
	}

	@BeforeAll
	public static void internalBeforeAll() {
		Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		log.setLevel(ch.qos.logback.classic.Level.WARN);
	}

	@BeforeEach
	public final void internalBefore() throws Exception {
		FxToolkit.registerPrimaryStage();

		Platform.runLater(() -> {
			try {
				FXMLLoader loader = new FXMLLoader();
				loader.setClassLoader(MainWindow.class.getClassLoader());
				loader.setLocation(MainWindow.class.getResource("mainWindow.fxml"));

				FxToolkit.toolkitContext().setRegisteredStage(loader.load());
				FxToolkit.showStage();
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		});

		FxToolkit.setupApplication(() -> new ApplicationAdapter(this));
	}

	@AfterEach
	public final void internalAfter() throws Exception {
		// release all keys
		release(new KeyCode[0]);
		// release all mouse buttons
		release(new MouseButton[0]);
		FxToolkit.cleanupStages();
		FxToolkit.cleanupApplication(new ApplicationAdapter(this));
	}

	/**
	 * Standard values:<br>
	 * java.awt.headless = true<br>
	 * testfx.robot = glass<br>
	 * testfx.headless = true<br>
	 * prism.order = sw<br>
	 * prism.text = t2k<br>
	 */
	public static void headless() {
		System.setProperty("java.awt.headless", "true");
		System.setProperty("testfx.robot", "glass");
		System.setProperty("testfx.headless", "true");
		System.setProperty("prism.order", "sw");
		System.setProperty("prism.text", "t2k");
	}

	/**
	 * Standard values to replace:<br>
	 * java.awt.headless = true<br>
	 * testfx.robot = glass<br>
	 * testfx.headless = true<br>
	 * prism.order = sw<br>
	 * prism.text = t2k<br>
	 */
	public static void headless(Map<String, String> map) {
		headless();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			System.setProperty(entry.getKey(), entry.getValue());

		}
	}

	public static void removeHeadless() {
		System.clearProperty("java.awt.headless");
		System.clearProperty("testfx.headless");
		System.clearProperty("prism.order");
		System.clearProperty("prism.text");
		System.setProperty("testfx.robot", "awt");
	}

	@Override
	public void init() throws Exception {
	}

	@Override
	public void start(Stage stage) throws Exception {
		/* Empty on purpose */
	}

	@Override
	public void stop() throws Exception {
	}
}
