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
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
// import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import org.unigrid.janus.model.rpc.entity.BudgetGetVote;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.signal.NewBlock;
import org.unigrid.janus.model.signal.ProposalEntry;
import org.unigrid.janus.view.component.Proposal;

@ApplicationScoped
public class VoteController implements Initializable {
	@Inject private RPCService rpc;
	@Inject private Event<ProposalEntry> proposalEntryEvent;

	@FXML private StackPane stackNoProposals;
	@FXML private ListView<BudgetGetVote.Result> dataList;

	private ObservableList<BudgetGetVote.Result> tableList;
	private List<BudgetGetVote.Result> listBudgetInfo = new ArrayList<>();

	@PostConstruct
	private void postConstruct() {
		tableList = FXCollections.observableList(listBudgetInfo);
	}

	@Override
	public void initialize(URL u, ResourceBundle rb) {
//		dataList.setItems(tableList);
		dataList.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(BudgetGetVote.Result item, boolean empty) {
				super.updateItem(item, empty);

				if (empty) {
					return;
				}

				try {
					final Proposal proposal = CDI.current().select(Proposal.class).get();
					proposalEntryEvent.fire(
						ProposalEntry.builder()
							.data(item)
							.container(proposal.getContainer())
							.build()
					);
					setGraphic(proposal.getContainer());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		dataList.setMinHeight(0);
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
		// listBudgetInfo = result.stream()
		// 	.filter(item -> !item.getName().equals("Pickle-DAO"))
		// 	.collect(Collectors.toList());

		if (listBudgetInfo.isEmpty()) {
			dataList.setVisible(false);
			stackNoProposals.setVisible(true);
		} else {
			dataList.setVisible(true);
			stackNoProposals.setVisible(false);
			dataList.setItems(FXCollections.observableList(listBudgetInfo));
		}
	}

}
