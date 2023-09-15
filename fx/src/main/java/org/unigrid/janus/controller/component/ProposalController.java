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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.ProposalEntry;
import org.unigrid.janus.view.FxUtils;

@Dependent
public class ProposalController implements Initializable {
	@Inject private BrowserService browser;
	@Inject private RPCService rpc;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	@FXML
	public void onNo(ActionEvent e) {
		FxUtils.executeParentById("container", (Node) e.getSource(), node -> {
			rpc.call(
				new BudgetVote.Request(new Object[]{"vote-many", (String) node.getUserData(), "no"}),
				BudgetVote.class);
		});
	}

	@FXML
	public void onYes(ActionEvent e) {
		FxUtils.executeParentById("container", (Node) e.getSource(), node -> {
			rpc.call(
				new BudgetVote.Request(new Object[]{"vote-many", (String) node.getUserData(), "yes"}),
				BudgetVote.class);
		});
	}

	@FXML
	public void onAbstain(ActionEvent e) {
		FxUtils.executeParentById("container", (Node) e.getSource(), node -> {
			rpc.call(
				new BudgetVote.Request(new Object[]{"vote-many", (String) node.getUserData(), "abstain"}),
				BudgetVote.class);
		});
	}

	private void onEntry(@Observes ProposalEntry item) {
		Node container = item.getContainer();
		item.getContainer().setUserData(item.getData().getHash());

		Label lblYes = (Label) container.lookup("#lblYes");
		Label lblNo = (Label) container.lookup("#lblNo");
		Label lblAbstain = (Label) container.lookup("#lblAbstain");
		Hyperlink proposalTitle = (Hyperlink) container.lookup("#proposalTitle");
		ProgressBar voteProgress = (ProgressBar) container.lookup("#voteProgress");

		proposalTitle.setText(item.getData().getName());
		proposalTitle.setOnAction(e -> {
			System.out.println(item.getData().getUrl());
			browser.navigate(item.getData().getUrl());
		});

		lblYes.setText("Yes: " + item.getData().getYeas());
		lblNo.setText("No: " + item.getData().getNays());
		lblAbstain.setText("Abstain: " + item.getData().getAbstains());
		voteProgress.setProgress(item.getData().getRatio());

		final String toolTip = "Yes = " + item.getData().getYeas() + "    No = " + item.getData().getNays();
		voteProgress.setTooltip(new Tooltip(toolTip));
	}
}
