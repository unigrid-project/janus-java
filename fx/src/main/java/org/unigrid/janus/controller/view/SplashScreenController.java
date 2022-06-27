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

package org.unigrid.janus.controller.view;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.service.WindowService;

@Eager
@ApplicationScoped
public class SplashScreenController implements Initializable, PropertyChangeListener {

	private WindowService window;

	@FXML
	private ProgressBar progBar;
	@FXML
	private FontIcon spinnerPreLoad;
	@FXML
	private Label lblText;
	@FXML
	private Label lblStatus;
	@FXML
	private Label verLbl;

	//@Inject
	//private SplashScreen splashScreen;

	private float ind = (float) 0.6;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		System.out.println("ADDRESS   :" + this);
		window = window.getInstance();
		window.setSplashScreenController(this);

		Platform.runLater(() -> {
			progBar.setVisible(false);
			//Font font = Font.loadFont("fonts/PressStart2P-vaV7.ttf", 10);
			//lblText.setFont(font);
			//lblStatus.setFont(font);
		});
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		//
	}

	public void updateProgress(float prog) {
		//System.out.println("address: " + this);
		//System.out.println("progress: " + prog);
		progBar.setProgress(prog);
	}

	public void showProgressBar() {
		spinnerPreLoad.setVisible(false);
		progBar.setVisible(true);
	}

	public void setText(String s) {
		System.out.println("ADDRESS              :" + this);
		lblText.setText(s);
	}
	
	public void hideProgBar() {
		progBar.setVisible(false);
	}

	public void showSpinner() {
		spinnerPreLoad.setVisible(true);
	}
}
