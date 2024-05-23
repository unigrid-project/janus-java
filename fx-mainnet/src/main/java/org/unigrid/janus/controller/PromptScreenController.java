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
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.unigrid.janus.model.cdi.Eager;

import org.unigrid.janus.model.signal.PromptRequest;
import org.unigrid.janus.view.PromptScreen;

@Eager
@ApplicationScoped
public class PromptScreenController {
	@Inject private PromptScreen promptScreen;

	@FXML private Label promptLabel;

	private PromptRequest currentPromptRequest;

	public void setText(String s) {
		if (promptLabel == null) {
			// Log the error or handle it as appropriate
			System.out.println("promptLabel is null setText. Skipping setText.");
			return;
		}

		Platform.runLater(() -> promptLabel.setText(s));
	}

	private void onPromptRequest(@Observes PromptRequest request) {
		Platform.runLater(() -> {
			if (promptLabel != null) {
				promptLabel.setText(request.getType().getLabelText());
			} else {
				// Handle null case, possibly log an error
				System.out.println("promptLabel is null.");
			}

			currentPromptRequest = request;
			promptScreen.show();
		});
	}

	public void onBtnPrimaryClicked(MouseEvent event) {
		currentPromptRequest.getOnPrimary().run();
	}

	public void onBtnSecondaryClicked(MouseEvent event) {
		currentPromptRequest.getOnSecondary().run();
	}
}
