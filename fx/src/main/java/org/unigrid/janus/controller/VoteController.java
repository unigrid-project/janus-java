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
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.unigrid.janus.model.rpc.entity.BudgetGetVote;
import org.unigrid.janus.model.rpc.entity.BudgetVote;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.NewBlock;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.service.DebugService;

@ApplicationScoped
public class VoteController implements Initializable {
	@Inject
	private DebugService debug;
	@Inject
	private RPCService rpc;
	@Inject
	private BrowserService browser;

	@FXML
	private Label lblTitle;
	@FXML
	private Label lblSummary;
	@FXML
	private Button btnYes;
	@FXML
	private Button btnNo;
	@FXML
	private Button btnAbstain;
	@FXML
	private Label lblYeas;
	@FXML
	private Label lblNays;
	@FXML
	private ProgressBar pgbRatio;
	@FXML
	private AnchorPane pnlCastVote;
	@FXML
	private TableView tblGoveData;
	@FXML
	private TableColumn colProposal;
	@FXML
	private TableColumn colVotes;
	@FXML
	private TableColumn colYes;
	@FXML
	private TableColumn colNo;
	@FXML
	private TableColumn colAbstain;
	@FXML
	private TableColumn colNumVotes;
	@FXML
	private Label lblNoVotes;

	private ObservableList<BudgetGetVote.Result> tableList;
	private List<BudgetGetVote.Result> listBudgetInfo = new ArrayList<>();

	@PostConstruct
	private void postConstruct() {
		tableList = FXCollections.observableList(listBudgetInfo);
	}

	@Override
	public void initialize(URL u, ResourceBundle rb) {
		String cssPath = VoteController.class
			.getResource("/org/unigrid/janus/view/main.css").toExternalForm();
		String style = "-fx-background-radius: 50;" + "-fx-border-color: #e72;"
			+ "-fx-border-radius: 50;" + "-fx-border-width: 2;" + "-fx-cursor: hand;";
		// tblGoveData.setItems(tableList);
		colProposal.prefWidthProperty().bind(tblGoveData.widthProperty().multiply(0.25));
		colVotes.prefWidthProperty().bind(tblGoveData.widthProperty().multiply(0.3));
		colAbstain.prefWidthProperty().bind(tblGoveData.widthProperty().multiply(0.15));
		colVotes.setStyle("-fx-alignment: CENTER;");
		colProposal.setCellValueFactory(cell -> {
			System.out.println("Name");
			String name = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell)
				.getValue().getName();
			String url = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, String>) cell)
				.getValue().getUrl();
			Hyperlink link = new Hyperlink();
			System.out.println(link.toString());

			link.setText(name);
			link.setOnAction(e -> {
				browser.navigate(url);
			});
			System.out.println(link.toString());

			return new ReadOnlyObjectWrapper<Hyperlink>(link);
		});

		colNumVotes.setCellValueFactory(cell -> {
			int yes = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getYeas();
			int no = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getNays();
			int abstain = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getAbstains();
			String output = yes + "/" + no + "/" + abstain;
			return new ReadOnlyStringWrapper(output);
		});

		colVotes.setCellValueFactory(cell -> {
			System.out.println("Doing a progbar");
			ProgressBar bar = new ProgressBar();
			double progress = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getRatio();
			int no = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getNays();
			int yes = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getYeas();
			String toolTip = "Yes = " + yes + "         No = " + no;
			bar.setTooltip(new Tooltip(toolTip));
			// float progress = 0.0f;
			// progress = (yes / (yes + no));
			bar.setProgress(progress);
			bar.setPrefWidth(tblGoveData.widthProperty().multiply(0.3).doubleValue());
			bar.getStylesheets().addAll(cssPath);

			bar.getStyleClass().add("vote-progress-bar");

			return new ReadOnlyObjectWrapper<ProgressBar>(bar);
		});

		colYes.setCellValueFactory(cell -> {
			Button button = new Button("Yes");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getHash();
			button.setOnAction(e -> {
				rpc.call(
					new BudgetVote.Request(new Object[]{"vote-many", hash, "yes"}),
					BudgetVote.class);
			});
			button.getStylesheets().addAll(cssPath);
			button.setStyle(style);
			return new ReadOnlyObjectWrapper<Button>(button);
		});

		colNo.setCellValueFactory(cell -> {
			Button button = new Button("No");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getHash();
			button.setOnAction(e -> {
				rpc.call(new BudgetVote.Request(new Object[]{"vote-many", hash, "no"}),
					BudgetVote.class);
			});
			button.getStylesheets().addAll(cssPath);
			button.setStyle(style);
			return new ReadOnlyObjectWrapper<Button>(button);
		});

		colAbstain.setCellValueFactory(cell -> {
			Button button = new Button("Abstain");
			String hash = ((TableColumn.CellDataFeatures<BudgetGetVote.Result, Integer>) cell)
				.getValue().getHash();
			button.setOnAction(e -> {
				rpc.call(
					new BudgetVote.Request(
						new Object[]{"vote-many", hash, "abstain"}),
					BudgetVote.class);
			});
			button.getStylesheets().addAll(cssPath);
			button.setStyle(style);
			return new ReadOnlyObjectWrapper<Button>(button);
		});
	}

	private void onMessage(@Observes NewBlock update) {
		BudgetGetVote budgetInfo = rpc.call(
			new BudgetGetVote.Request(new Object[]{"show"}), BudgetGetVote.class);
		List<BudgetGetVote.Result> result = budgetInfo.getResult();

		if (result == null) {
			// Handle the case when result is null, maybe log an error or return from the
			// method
			return;
		}
		listBudgetInfo.clear();
		listBudgetInfo.addAll(result);
		// Filter out "Pickle-DAO" using Java Streams
		listBudgetInfo = result.stream()
			.filter(item -> !item.getName().equals("Pickle-DAO"))
			.collect(Collectors.toList());

		if (listBudgetInfo.isEmpty()) {
			tblGoveData.setVisible(false);
			lblNoVotes.setVisible(true);
		} else {
			tblGoveData.setVisible(true);
			lblNoVotes.setVisible(false);
			tblGoveData.setItems(FXCollections.observableList(listBudgetInfo));
		}
	}

}
