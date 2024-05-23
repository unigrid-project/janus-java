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

package org.unigrid.janus.view.component;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class WindowBarButton extends Region {
	private ScaleTransition transition;

	public WindowBarButton() {
		transition = new ScaleTransition(Duration.millis(150), this);
		transition.setInterpolator(Interpolator.EASE_BOTH);
		transition.setCycleCount(1);

		this.setOnMouseEntered(event -> {
			transition.setFromX(this.getScaleX());
			transition.setFromY(this.getScaleY());
			transition.setToX(1.3f);
			transition.setToY(1.3f);
			transition.playFromStart();
		});

		this.setOnMouseExited(event -> {
			transition.setFromX(this.getScaleX());
			transition.setFromY(this.getScaleY());
			transition.setToX(1f);
			transition.setToY(1f);
			transition.playFromStart();
		});
	}
}
