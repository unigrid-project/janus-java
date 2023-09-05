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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import org.unigrid.janus.model.rpc.entity.BudgetGetVote;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.NewBlock;

@ApplicationScoped
public class VoteController {

	@Inject private RPCService rpc;
	
	@FXML Label lblTitle;
	@FXML Label lblSummary;
	@FXML Button btnYes;
	@FXML Button btnNo;
	@FXML Button btnAbstain;
	@FXML Label lblYeas;
	@FXML Label lblNays;
	@FXML ProgressBar pgbRatio;
	@FXML AnchorPane pnlCastVote;

	@FXML
	public void OnVoteNo(ActionEvent e) {
		rpc.call(new BudgetVote.Request(new Object[]{"vote-many", "no"}), BudgetVote.class);
	}

	@FXML
	public void OnVoteYes(ActionEvent e) {
		rpc.call(new BudgetVote.Request(new Object[]{"vote-many", "yes"}), BudgetVote.class);
	}

	@FXML
	public void OnVoteAbstain(ActionEvent e) {
		rpc.call(new BudgetVote.Request(new Object[]{"vote-many", "abstain"}), BudgetVote.class);
	}

	private void onMessage(@Observes NewBlock update) {
		BudgetGetVote budgetInfo = rpc.call(new BudgetGetVote.Request(new Object[]{"show"}), BudgetGetVote.class);

		if(budgetInfo.getResult() == null) {
			System.out.println("result was not null");
			lblTitle.setText(budgetInfo.getResult().getName());
			lblSummary.setText(budgetInfo.getResult().getUrl());
			int yes = budgetInfo.getResult().getYeas();
			int no = budgetInfo.getResult().getNays();
			lblYeas.setText(Integer.toString(yes));
			lblNays.setText(Integer.toString(no));
			float progress = 0.0f;
			progress = (yes / (yes + no));
			pgbRatio.setProgress(progress);
		}
	}
}
