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

package org.unigrid.janus.model.service;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import java.awt.SystemTray;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

public class TrayService {

	private FXTrayIcon tray = null;

	public void initTrayService(Stage stage) {
		System.out.println("Is systemTray supported");
		if (SystemTray.isSupported()) {
			System.out.println("Init tray icon");
			tray = new FXTrayIcon(stage,
				getClass().getResource("/org/unigrid/janus/view/images/unigrid-round.png"));
			tray.show();
			tray.addExitItem("Exit");
			tray.setTooltip("Unigrid");	
		}
	}

	public void updateNewEventImage() {
		tray.setGraphic(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round.png"));
	}

	private void updateStandardImage() {
		tray.setGraphic(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round.png"));
	}
	
	private Image manipulateImage(String path) {
		try {
			BufferedImage base = ImageIO.read(new File(path));
			BufferedImage img2 = ImageIO.read(new File(getClass()
				.getResource("/org/unigrid/janus/view/images/unigrid-round-reddot.png").toString()));

			int offset = 2;
			int width = base.getWidth() + img2.getWidth() + offset;
			int heigth = Math.max(base.getHeight(), img2.getHeight()) + offset;
		} catch(Exception e) {
			
		}
	}
}
