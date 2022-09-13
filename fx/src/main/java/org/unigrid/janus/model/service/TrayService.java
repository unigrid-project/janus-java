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
import jakarta.enterprise.context.ApplicationScoped;
import java.awt.SystemTray;
import java.io.InputStream;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.apache.commons.lang3.NotImplementedException;

@ApplicationScoped
public class TrayService {

	private FXTrayIcon tray = null;

	public void initTrayService(Stage stage) {
		System.out.println("Is systemTray supported");
		if (SystemTray.isSupported()) {
			System.out.println("Init tray icon");
			tray = new FXTrayIcon(stage,
				getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
			tray.show();
			tray.addExitItem("Exit");
			tray.setTooltip("Unigrid");
		}
	}

	//TODO: Implement the blend of the images
	public void updateNewEventImage() throws NotImplementedException {
		Image image = manipulateImage(getClass()
			.getResourceAsStream("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
		if (image != null) {
			tray.setGraphic(image);
		} else {
			System.exit(1);
		}
	}

	private void updateStandardImage() {
		tray.setGraphic(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
	}

	private Image manipulateImage(InputStream path) {
		try {
			Image base = new Image(path);
			Image redDot = new Image(getClass()
				.getResourceAsStream("/org/unigrid/janus/view/images/red-dot.png"));
			ImageView bottom = new ImageView(base);
			ImageView top = new ImageView(redDot);
			top.setBlendMode(BlendMode.OVERLAY);

			Group blend = new Group(
				bottom,
				top
			);
			return blend.snapshot(new SnapshotParameters(), null);
			/*System.out.println(path);
			System.out.println(getClass()
				.getResource("/org/unigrid/janus/view/images/red-dot.png").toString());
			BufferedImage base = ImageIO.read(path);
			//BufferedImage img2 = ImageIO.read(new File(getClass()
			//	.getResource("/org/unigrid/janus/view/images/red-dot.png").toString()));
			BufferedImage img2 = ImageIO.read(getClass()
				.getResourceAsStream("/org/unigrid/janus/view/images/red-dot.png"));
			int offset = 2;
			int width = base.getWidth() + img2.getWidth() + offset;
			int heigth = Math.max(base.getHeight(), img2.getHeight()) + offset;
			BufferedImage newImage = new BufferedImage(width, heigth, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = newImage.createGraphics();
			Color oldColor = g2.getColor();
			g2.setPaint(Color.BLACK);
			g2.fillRect(0, 0, width, heigth);
			g2.setColor(oldColor);
			g2.drawI
			g2.drawImage(base, null, 0, 0);
			g2.drawImage(img2, null, base.getWidth() + offset, 0);
			g2.dispose();
			return newImage;*/
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
