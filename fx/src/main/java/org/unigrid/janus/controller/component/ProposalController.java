/*
    The Janus Wallet
    Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.controller.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.ProposalEntry;
import org.unigrid.janus.view.FxUtils;

@Dependent
public class ProposalController implements Initializable {
	@Inject private BrowserService browser;
	@Inject private RPCService rpc;
	@Inject private DebugService debug;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	@FXML
	public void onNo(ActionEvent e) {
		FxUtils.executeParentById("container", (Node) e.getSource(), node -> {
			BudgetVote.Request voteRequest = new BudgetVote.Request(new Object[]{"vote-many",
				(String) node.getUserData(), "no"});

			String rawResponse = rpc.callToJson(voteRequest);
			System.out.println("Raw RPC Response: " + rawResponse);

			BudgetVote.Result voteResult = rpc.call(voteRequest, BudgetVote.Result.class);

			if (voteResult != null) {
				// Log the response for debugging
				System.out.println("Received response: " + voteResult.toString());

				// Access the "overall" field using the getOverall method
				System.out.println("Overall: " + voteResult.getOverall());

				// Further processing of the response
				Notifications.create().title("Vote Results:").text(voteResult.getOverall())
					.showInformation();
				debug.print("no: " + voteResult.getOverall(),
					ProposalController.class.getSimpleName());
			} else {
				// Log an error if the response is null
				System.err.println("Received null response from RPC.");
			}
		});
	}

	@FXML
	public void onYes(ActionEvent e) {
		FxUtils.executeParentById("container", (Node) e.getSource(), node -> {
			BudgetVote.Request voteRequest = new BudgetVote.Request(new Object[]{"vote-many",
				(String) node.getUserData(), "yes"});

			String rawResponse = rpc.callToJson(voteRequest);
			System.out.println("Raw RPC Response: " + rawResponse);

			BudgetVote.Result voteResult = rpc.call(voteRequest, BudgetVote.Result.class);

			if (voteResult != null) {
				System.out.println("Received response: " + voteResult.toString());

				System.out.println("Overall: " + voteResult.getOverall());

				Notifications.create().title("Vote Results:").text(voteResult.getOverall())
					.showInformation();
				debug.print("yes: " + voteResult.getOverall(),
					ProposalController.class.getSimpleName());
			} else {
				// Log an error if the response is null
				System.err.println("Received null response from RPC.");
			}
		});
	}

	private <T> T deserialize(String json, Class<T> clazz) {
		// You would use your JSON library's methods here.
		// For instance, using Jackson:
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, clazz);
		} catch (IOException e) {
			// Handle deserialization errors
			e.printStackTrace();
			return null;
		}
	}

	private void onEntry(@Observes ProposalEntry item) {
		Node container = item.getContainer();
		item.getContainer().setUserData(item.getData().getHash());

		Label lblYes = (Label) container.lookup("#lblYes");
		Label lblNo = (Label) container.lookup("#lblNo");
		Hyperlink proposalTitle = (Hyperlink) container.lookup("#proposalTitle");
		ProgressBar voteProgress = (ProgressBar) container.lookup("#voteProgress");
		Button btnYes = (Button) container.lookup("#btnYes");
		Button btnNo = (Button) container.lookup("#btnNo");
		Label resultLbl = (Label) container.lookup("#resultLbl");

		proposalTitle.setText("Proposal: " + item.getData().getName());
		proposalTitle.setOnAction(e -> {
			System.out.println(item.getData().getUrl());
			browser.navigate(item.getData().getUrl());
		});

		lblYes.setText("Yes: " + item.getData().getYeas());
		lblNo.setText("No: " + item.getData().getNays());
		voteProgress.setProgress(item.getData().getRatio());

		final String toolTip = "Yes = " + item.getData().getYeas() + "    No = " + item.getData().getNays();
		voteProgress.setTooltip(new Tooltip(toolTip));


		if ("Unigrid DAO".equals(item.getData().getName())) {
			btnYes.setVisible(false);
			btnNo.setVisible(false);
			resultLbl.setVisible(true);
			resultLbl.setText("Results: PASSED");
			resultLbl.setStyle("-fx-text-fill: green;");
		}
}

}
