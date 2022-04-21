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

import com.sun.jna.platform.win32.WinDef;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.event.CloseJanusEvent;
import org.unigrid.janus.model.service.WindowService;

@Dependent
public class SplashScreen implements Window {

	@Inject
	private Stage stageSplash;
	private WindowService window = new WindowService();

	private FontIcon spinnerPreLoad;
	private RotateTransition rt;

	public SplashScreen() {

	}

	@SneakyThrows
	public void show() {
		try {
			window.setStage(stageSplash);
			stageSplash.centerOnScreen();
			stageSplash.initStyle(StageStyle.UNDECORATED);
			stageSplash.setResizable(false);
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
		//spinnerPreLoad.setVisible(true);
		spinnerPreLoad.setFill(Color.ORANGE);
		rt = new RotateTransition(Duration.millis(50000), spinnerPreLoad);
		rt.setRate(1.0);
		rt.setByAngle(180);
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
}