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

package org.unigrid.janus.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.SplashModel;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.WindowService;

@Eager
@ApplicationScoped
public class SplashScreenController implements Initializable, PropertyChangeListener {
	private WindowService window;

	@Inject private DebugService debug;

	private SplashModel splashModel = new SplashModel();
	@FXML private ProgressBar progBar;
	@FXML private FontIcon spinnerPreLoad;
	@FXML private Label lblText;
	@FXML private Label lblStatus;
	@FXML private Label verLbl;
	@FXML private TextArea debugTxt;
	@FXML private GridPane splashGrid;
	@FXML private Tooltip bugTooltip;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		window = window.getInstance();
		window.setSplashScreenController(this);

		Platform.runLater(() -> {
			progBar.setVisible(false);
			// Font font = Font.loadFont("fonts/PressStart2P-vaV7.ttf", 10);
			// lblText.setFont(font);
			// lblStatus.setFont(font);
			debugTxt.textProperty().addListener(new ChangeListener() {
				public void changed(ObservableValue ov, Object oldValue, Object newValue) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException ex) {
						// don't care
					}
					debugTxt.setScrollTop(Double.MAX_VALUE);    //top
					//vpsOutput.setScrollTop(Double.MIN_VALUE);   //down
				}
			});
		});
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		/* Empty on purpose */
	}

	public void updateProgress(float prog) {
		// System.out.println("address: " + this);
		// System.out.println("progress: " + prog);
		progBar.setProgress(prog);
	}

	public void showProgressBar() {
		spinnerPreLoad.setVisible(false);
		progBar.setVisible(true);
	}

	public void setText(String s) {
		lblText.setText(s);
	}

	public void hideProgBar() {
		progBar.setVisible(false);
	}

	public void showSpinner() {
		spinnerPreLoad.setVisible(true);
	}

	public void setDebugText(String value) {
		debugTxt.setText(value);
		debugTxt.appendText("");
	}

	@FXML
	public void onShowDebug(MouseEvent event) throws Exception {
		//System.out.println(window.getStage().getHeight());
		//System.out.println(splashGrid.getCellBounds(0, 6));
		File debugLog = DataDirectory.getDebugLog();
		try {
			// Open debug.log file
			window.getHostServices().showDocument(debugLog.getAbsolutePath());
			// disable showing log on load as it runs very slow with large debugs
			/*
			if (!splashModel.getDebug()) {
				window.getSplashScreen().startMonitor();
				debugTxt.setVisible(true);
				splashModel.setDebug(true);
				debugTxt.setScrollTop(Double.MAX_VALUE);
				window.getStage().setHeight(400);
				bugTooltip.setText("Hide debug log");
			} else {
				window.getSplashScreen().stopMonitor();
				debugTxt.setVisible(false);
				splashModel.setDebug(false);
				window.getStage().setHeight(220);
				bugTooltip.setText("Show debug log");
			}
			 */
		} catch (Exception e) {
			debug.print(e.getMessage(), SplashScreenController.class.getSimpleName());
		}
	}
}
