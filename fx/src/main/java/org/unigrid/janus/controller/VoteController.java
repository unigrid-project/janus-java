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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.unigrid.janus.model.rpc.entity.BudgetGetVote;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.NewBlock;
import org.unigrid.janus.model.BudgetInfo;
import org.unigrid.janus.model.service.BrowserService;

@ApplicationScoped
public class VoteController implements Initializable{

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
	private List<BudgetGetVote.Result> listBudgetInfo = new ArrayList<>();;
	
	@PostConstruct
	private void testmethod() {
		tableList = FXCollections.observableList(listBudgetInfo);
	}
	
	@Override
	public void initialize(URL u, ResourceBundle rb) {
		//URL cssPath = VoteController.class.getResource("resources/main.css");
		//tblGoveData.setItems(tableList);
		tbcProposal.prefWidthProperty().bind(tblGoveData.widthProperty().multiply(0.25));
		tbcVotes.prefWidthProperty().bind(tblGoveData.widthProperty().multiply(0.3));
		tbcVotes.setStyle("-fx-alignment: CENTER;");
		tbcProposal.setCellValueFactory(cell -> {
			System.out.println("Name");
			String name = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell).getValue()
				.getName();
			String url = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell).getValue()
				.getUrl();
			Hyperlink link = new Hyperlink();
			System.out.println(link.toString());

			link.setText(name);
			link.setOnAction(e -> {
				browser.navigate(url);
			});
			System.out.println(link.toString());

			return new ReadOnlyObjectWrapper<Hyperlink>(link);
		});

		tbcVotes.setCellValueFactory(cell -> {
			System.out.println("Doing a progbar");
			ProgressBar bar = new ProgressBar();
			double progress = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getRatio();
			int no = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getNays();
			int yes = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getYeas();
			String toolTip = "Yes = " + yes + "         No = " + no;
			bar.setTooltip(new Tooltip(toolTip));
			//float progress = 0.0f;
			//progress = (yes / (yes + no));
			bar.setProgress(progress);
			bar.setPrefWidth(tblGoveData.widthProperty().multiply(0.3).doubleValue());
			//bar.getStylesheets().add(cssPath.toString());
			
			//bar.getStyleClass().add("vote-progress-bar");

			return new ReadOnlyObjectWrapper<ProgressBar>(bar);
		});
		
		tbcYes.setCellValueFactory(cell -> {
			Button button = new Button("Yes");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "yes"}),
					BudgetVote.class);
			});
			
			return new ReadOnlyObjectWrapper<Button>(button);
		});
		
		tbcNo.setCellValueFactory(cell -> {
			Button button = new Button("No");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "no"}),
					BudgetVote.class);
			});
			return new ReadOnlyObjectWrapper<Button>(button);
		});
		
		tbcAbstain.setCellValueFactory(cell -> {
			Button button = new Button("Abstain");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell).getValue()
				.getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "abstain"}),
					BudgetVote.class);
			});
			return new ReadOnlyObjectWrapper<Button>(button);
		});
	}

	private void onMessage(@Observes NewBlock update) {
		BudgetGetVote budgetInfo = rpc.call(new BudgetGetVote.Request(new Object[]{"show"}), BudgetGetVote.class);

		listBudgetInfo.clear();
		listBudgetInfo.addAll(budgetInfo.getResult());
		tblGoveData.setItems(tableList);
		

		for (int i = 0; i < budgetInfo.getResult().size(); i++) {
			
			lblTitle.setText(budgetInfo.getResult().get(i).getName());
			pgbRatio.setProgress(budgetInfo.getResult().get(i).getRatio());
		}
	}
}
