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

package org.unigrid.janus.view;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.controller.SplashScreenController;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.signal.CloseJanus;
import org.unigrid.janus.model.service.BrowserService;

@Eager
@Data
@ApplicationScoped
public class SplashScreen implements Window {
	@Inject private JanusModel janusModel;
	@Inject private SplashScreenController splashScreenController; // TODO: Big no no. View should not see this.
	@Inject private Stage stageSplash;
	@Inject private BrowserService window;

	private FontIcon spinnerPreLoad;
	private RotateTransition rt;
	private Label text;
	private Label status;
	private Label lbl;
	private FileAlterationMonitor monitor = new FileAlterationMonitor(2000);

	@PostConstruct
	private void init() {
		stageSplash.centerOnScreen();
		stageSplash.initStyle(StageStyle.UNDECORATED);
		stageSplash.setResizable(false);
	}

	@SneakyThrows
	public void show() {
		lbl = (Label) stageSplash.getScene().lookup("#verLbl");
		lbl.setText("version: ".concat(janusModel.getVersion()));

		try {
			stageSplash.show();
			startSpinner();
		} catch (Exception e) {
			AlertDialog.open(Alert.AlertType.ERROR, e.getMessage());
		}
	}

	@Override
	public void hide() {
		//stopMonitor();
		stageSplash.close();
	}

	public void startSpinner() {
		spinnerPreLoad = (FontIcon) stageSplash.getScene().lookup("#spinnerPreLoad");
		spinnerPreLoad.setVisible(true);

		rt = new RotateTransition(Duration.millis(50000), spinnerPreLoad);
		//rt.setRate(1.0);
		rt.setByAngle(20000);
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setAutoReverse(true);
		rt.setInterpolator(Interpolator.LINEAR);

		rt.play();
	}

	public void stopSpinner() {
		rt.stop();
		//spinnerPreLoad.setVisible(false);
	}

	private void onClose(@Observes Event<CloseJanus> closeJanus) {
		this.stageSplash.close();
	}

	public void initText() {
		text = (Label) stageSplash.getScene().lookup("#lblText");
		status = (Label) stageSplash.getScene().lookup("#lblStatus");

		InputStream in = getClass().getResourceAsStream("fonts/PressStart2P-vaV7.ttf");
		Font font = Font.loadFont(in, 10);

		text.setFont(font);
		text.setAlignment(Pos.CENTER);
		text.setText("Starting unigrid backend");
		status.setFont(font);
		status.setText("...");

		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.7), evt -> status.setVisible(false)),
			new KeyFrame(Duration.seconds(0.2), evt -> status.setVisible(true))
		);

		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	public void setText(String s) {
		text.setText(s);
	}

	private void readDebug() throws IOException, Exception {
		File debug = DataDirectory.getBackendLog();
		FileAlterationObserver observer = new FileAlterationObserver(debug.getParent());

		observer.addListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileChange(File file) {
				super.onFileChange(file);

				if (file.equals(debug)) {
					System.out.println("DEBUG UPDATED: " + file);

					try {
						updateDebug();
					} catch (IOException ex) {
						Logger.getLogger(SplashScreen.class.getName())
							.log(Level.SEVERE, null, ex);
					}
				}

				//System.out.println("File updated: " + file);
			}

			@Override
			public void onStart(FileAlterationObserver observer) {
				//System.out.println("observer start: " + observer);
			}

			@Override
			public void onStop(FileAlterationObserver observer) {
				//System.out.println("observer stop: " + observer);
			}
		});

		monitor.addObserver(observer);
		try {
			monitor.start();
			updateDebug();
		} catch (Exception e) {
			System.out.println("error: " + e.getMessage());
		}

	}

	private void updateDebug() throws FileNotFoundException, IOException {
		File debug = DataDirectory.getBackendLog();
		StringBuffer sbuffer = new StringBuffer();
		FileReader fileReader = new FileReader(debug);
		BufferedReader br = new BufferedReader(fileReader);
		String line;

		while ((line = br.readLine()) != null) {
			sbuffer.append(line + "\n");
		}

		splashScreenController.setDebugText(sbuffer.toString()); // TODO: Big no no. View should not see this.
	}

	public void startMonitor() throws Exception {
		readDebug();
	}

	public void stopMonitor() {
		try {
			monitor.stop();
		} catch (Exception e) {
			System.out.println("error: " + e.getMessage());
		}
	}

}
