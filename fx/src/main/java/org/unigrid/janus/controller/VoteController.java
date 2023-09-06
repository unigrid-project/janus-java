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
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.unigrid.janus.model.rpc.entity.BudgetGetVote;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.NewBlock;
import org.unigrid.janus.model.BudgetInfo;
import org.unigrid.janus.model.service.BrowserService;

@ApplicationScoped
public class VoteController {

	@Inject private RPCService rpc;
	@Inject private BrowserService browser;
	
	@FXML Label lblTitle;
	@FXML Label lblSummary;
	@FXML Button btnYes;
	@FXML Button btnNo;
	@FXML Button btnAbstain;
	@FXML Label lblYeas;
	@FXML Label lblNays;
	@FXML ProgressBar pgbRatio;
	@FXML AnchorPane pnlCastVote;
	@FXML TableView tblGoveData;
	@FXML TableColumn tbcProposal;
	@FXML TableColumn tbcVotes;
	@FXML TableColumn tbcYes;
	@FXML TableColumn tbcNo;
	@FXML TableColumn tbcAbstain;

	private ObservableList<BudgetGetVote.Result> tableList;
	private List<BudgetGetVote.Result> listBudgetInfo;
	
	private void init() {
		listBudgetInfo = new ArrayList<>();
		tableList = FXCollections.observableList(listBudgetInfo);
		tblGoveData.setItems(tableList);

		tbcProposal.setCellValueFactory(cell -> {
			String name = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell).getValue()
				.getName();
			String url = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell).getValue()
				.getUrl();
			Hyperlink link = new Hyperlink();
			
			link.setText(name);
			link.setOnAction(e -> {
				browser.navigate("www");
			});
			
			return link;
		});

		tbcVotes.setCellValueFactory(cell -> {
			ProgressBar bar = new ProgressBar();
			int yes = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getYeas();
			int no = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getNays();

			float progress = 0.0f;
			progress = (yes / (yes + no));
			bar.setProgress(progress);
			bar.getStylesheets().add("src/main/resources/main.css");
			
			bar.getStyleClass().add("vote-progress-bar");

			return bar;
		});
		
		tbcYes.setCellValueFactory(cell -> {
			Button button = new Button("Yes");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "yes"}),
					BudgetVote.class);
			});
			
			return button;
		});
		
		tbcNo.setCellValueFactory(cell -> {
			Button button = new Button("No");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "no"}),
					BudgetVote.class);
			});
			return button;
		});
		
		tbcAbstain.setCellValueFactory(cell -> {
			Button button = new Button("Abstain");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "abstain"}),
					BudgetVote.class);
			});
			return button;
		});
	}

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

		listBudgetInfo = budgetInfo.getResult();
		
		for (int i = 0; i < budgetInfo.getResult().size(); i++) {
			System.out.println(i);
			System.out.println(budgetInfo.getResult().get(i).getName());
		}
	}
}
