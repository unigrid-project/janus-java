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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.PollingService;
import org.unigrid.janus.model.service.WindowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.DocList;
import org.unigrid.janus.model.Documentation;

@ApplicationScoped
public class DocumentationController implements Initializable, PropertyChangeListener {
	private static final String DOCUMENTATION_URL = "https://docs.unigrid.org/docs/data/index.json";
	private static final int SIX_HOURS_IN_MS = 60 * 60 * 6 * 1000;
	@Inject private DebugService debug;
	@Inject private PollingService pollingService;

	private static Wallet wallet;
	private static WindowService window = WindowService.getInstance();
	private static DocList documentationList = new DocList();

	@FXML private TableView tblDocs;
	@FXML private TableColumn colDescription;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		wallet = window.getWallet();
		window.setDocsController(this);
		wallet.addPropertyChangeListener(this);
		documentationList.addPropertyChangeListener(this);
		pollingService = CDI.current().select(PollingService.class).get();
		setupDocList();

		try {
			pullNewDocumentaion();
		} catch (JsonProcessingException ex) {
			Logger.getLogger(DocumentationController.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(DocumentationController.class.getName()).log(Level.SEVERE, null, ex);
		}

		pollingService.poll(SIX_HOURS_IN_MS);
	}

	public void pullNewDocumentaion() throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		try {
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			List<Documentation> docList = Arrays.asList(mapper.readValue(new URL(DOCUMENTATION_URL),
				Documentation[].class)
			);

			documentationList.setDoclist(docList);

		} catch (MalformedURLException ex) {
			Logger.getLogger(DocumentationController.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(documentationList.DOCUMENTATION_LIST)) {
			debug.print("DOCUMENTATION_LIST", DocumentationController.class.getSimpleName());
			tblDocs.setItems(documentationList.getDoclist());
		}
	}

	private void setupDocList() {
		try {
			colDescription.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Documentation,
				Hyperlink>, ObservableValue<Hyperlink>>() {

				public ObservableValue<Hyperlink> call(TableColumn.CellDataFeatures<Documentation,
					Hyperlink> t) {

					Documentation doc = t.getValue();
					String text = doc.getTitle();
					Hyperlink link = new Hyperlink();
					link.setText(text);

					link.setOnAction(e -> {
						if (e.getTarget().equals(link)) {
							window.browseURL(doc.getLink());
						}
					});

					Button btn = new Button();
					FontIcon fontIcon = new FontIcon("far-newspaper");
					fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
					btn.setGraphic(fontIcon);
					link.setGraphic(btn);
					link.setAlignment(Pos.CENTER_RIGHT);

					return new ReadOnlyObjectWrapper(link);
				}
			});
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup node table) %s", e.getMessage()));
		}
	}
}
