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

package org.unigrid.janus.view;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.InputStream;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Data;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.event.CloseJanusEvent;
import org.unigrid.janus.model.service.WindowService;

@Data
@ApplicationScoped
public class SplashScreen implements Window {

	@Inject
	private Stage stageSplash;

	@Inject
	private WindowService window;

	private FontIcon spinnerPreLoad;
	private RotateTransition rt;
	private Label text;
	private Label status;

	public SplashScreen() {

	}

	@PostConstruct
	private void init() {
		stageSplash.centerOnScreen();
		stageSplash.initStyle(StageStyle.UNDECORATED);
		stageSplash.setResizable(false);
	}

	@SneakyThrows
	public void show() {
		try {
			window.setStage(stageSplash);
			stageSplash.show();
			startSpinner();
		} catch (Exception e) {
			Alert a = new Alert(Alert.AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
	}

	@Override
	public void hide() {
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

	private void onClose(@Observes Event<CloseJanusEvent> event) {
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
			new KeyFrame(Duration.seconds(0.2), evt -> status.setVisible(true)));
		timeline.setCycleCount(Animation.INDEFINITE);

		timeline.play();
	}

	public void setText(String s) {
		text.setText(s);
	}
}
