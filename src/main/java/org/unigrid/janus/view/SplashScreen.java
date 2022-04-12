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

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.NotImplementedException;
import org.unigrid.janus.model.service.WindowService;

@Dependent
public class SplashScreen implements Window {
    
	@Inject
	private Stage stageSplash;
	private WindowService window = new WindowService();
    
	@Override
	public void show() {
	    try {
			window.setStage(stageSplash);
			stageSplash.centerOnScreen();
			stageSplash.initStyle(StageStyle.UNDECORATED);
			stageSplash.setResizable(true);
			stageSplash.show();
		} catch (Exception e) {
			Alert a = new Alert(Alert.AlertType.ERROR,
				  				e.getMessage(),
				  				ButtonType.OK);
			a.showAndWait();
		}
	}
	
	@Override
	public void hide(){
	    
	}
}
