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

package org.unigrid.janus.controller;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import org.unigrid.janus.model.MnemonicModel;
import org.unigrid.janus.model.signal.ParentRequestSignal;
import org.unigrid.janus.model.signal.ResetTextFieldsSignal;
import org.unigrid.janus.model.signal.TabRequestSignal;

@ApplicationScoped
public class MnemonicController implements Initializable {
	@Inject
	private MnemonicModel mnemonicModel;
	@Inject
	private Event<TabRequestSignal> tabRequestEvent;
	@Inject
	private Event<ParentRequestSignal> parentRequestEvent;

	@FXML
	private TextField wordTwelveOne, wordTwelveTwo, wordTwelveThree, wordTwelveFour,
		wordTwelveFive, wordTwelveSix;
	@FXML
	private TextField wordTwelveSeven, wordTwelveEight, wordTwelveNine, wordTwelveTen,
		wordTwelveEleven, wordTwelveTwelve;
	@FXML
	private TextField wordTwentyFourOne, wordTwentyFourTwo, wordTwentyFourThree,
		wordTwentyFourFour, wordTwentyFourFive;
	@FXML
	private TextField wordTwentyFourSix, wordTwentyFourSeven, wordTwentyFourEight,
		wordTwentyFourNine, wordTwentyFourTen;
	@FXML
	private TextField wordTwentyFourEleven, wordTwentyFourTwelve, wordTwentyFourThirteen,
		wordTwentyFourFourteen, wordTwentyFourFifteen;
	@FXML
	private TextField wordTwentyFourSixteen, wordTwentyFourSeventeen,
		wordTwentyFourEighteen, wordTwentyFourNineteen, wordTwentyFourTwenty;
	@FXML
	private TextField wordTwentyFourTwentyOne, wordTwentyFourTwentyTwo,
		wordTwentyFourTwentyThree, wordTwentyFourTwentyFour;
	@FXML
	private TextField wordTwentyFourImportOne, wordTwentyFourImportTwo, wordTwentyFourImportThree,
		wordTwentyFourImportFour, wordTwentyFourImportFive;
	@FXML
	private TextField wordTwentyFourImportSix, wordTwentyFourImportSeven, wordTwentyFourImportEight,
		wordTwentyFourImportNine, wordTwentyFourImportTen;
	@FXML
	private TextField wordTwentyFourImportEleven, wordTwentyFourImportTwelve, wordTwentyFourImportThirteen,
		wordTwentyFourImportFourteen, wordTwentyFourImportFifteen;
	@FXML
	private TextField wordTwentyFourImportSixteen, wordTwentyFourImportSeventeen, wordTwentyFourImportEighteen,
		wordTwentyFourImportNineteen, wordTwentyFourImportTwenty;
	@FXML
	private TextField wordTwentyFourImportTwentyOne, wordTwentyFourImportTwentyTwo,
		wordTwentyFourImportTwentyThree, wordTwentyFourImportTwentyFour;

	private TextField[] textFields12List;
	private TextField[] textFields24List;
	private TextField[] textFields24ImportList;
	private List<String> mnemonicWordList = new ArrayList<>();
	private final String placeholderText = "•••••";
	private boolean isHandlingFocus = false;

	@Override
	public void initialize(URL url, ResourceBundle rb) {

		Platform.runLater(() -> {
			wordTwelveOne.setOnKeyPressed(this::handlePasteEvent);

			textFields12List = new TextField[]{wordTwelveOne, wordTwelveTwo,
				wordTwelveThree, wordTwelveFour, wordTwelveFive, wordTwelveSix,
				wordTwelveSeven, wordTwelveEight, wordTwelveNine, wordTwelveTen,
				wordTwelveEleven, wordTwelveTwelve};
			wordTwentyFourOne.setOnKeyPressed(this::handlePasteEvent);

			textFields24List = new TextField[]{wordTwentyFourOne, wordTwentyFourTwo,
				wordTwentyFourThree, wordTwentyFourFour, wordTwentyFourFive,
				wordTwentyFourSix, wordTwentyFourSeven, wordTwentyFourEight,
				wordTwentyFourNine, wordTwentyFourTen, wordTwentyFourEleven,
				wordTwentyFourTwelve, wordTwentyFourThirteen, wordTwentyFourFourteen,
				wordTwentyFourFifteen, wordTwentyFourSixteen, wordTwentyFourSeventeen,
				wordTwentyFourEighteen, wordTwentyFourNineteen, wordTwentyFourTwenty,
				wordTwentyFourTwentyOne, wordTwentyFourTwentyTwo,
				wordTwentyFourTwentyThree, wordTwentyFourTwentyFour};

			wordTwentyFourImportOne.setOnKeyPressed(this::handlePasteEvent);
			textFields24ImportList = new TextField[]{
				wordTwentyFourImportOne, wordTwentyFourImportTwo, wordTwentyFourImportThree,
				wordTwentyFourImportFour, wordTwentyFourImportFive, wordTwentyFourImportSix,
				wordTwentyFourImportSeven, wordTwentyFourImportEight, wordTwentyFourImportNine,
				wordTwentyFourImportTen, wordTwentyFourImportEleven, wordTwentyFourImportTwelve,
				wordTwentyFourImportThirteen, wordTwentyFourImportFourteen, wordTwentyFourImportFifteen,
				wordTwentyFourImportSixteen, wordTwentyFourImportSeventeen, wordTwentyFourImportEighteen,
				wordTwentyFourImportNineteen, wordTwentyFourImportTwenty, wordTwentyFourImportTwentyOne,
				wordTwentyFourImportTwentyTwo, wordTwentyFourImportTwentyThree,
				wordTwentyFourImportTwentyFour
			};

			for (TextField textField : textFields12List) {
				textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
					System.out.println("observable: " + observable);
					System.out.println("oldValue: " + oldValue);
					System.out.println("newValue: " + newValue);
					System.out.println("THIS SHOULD NOT BE TRIGGERED textFields12List: "
						+ mnemonicWordList.get(0));
					if (isHandlingFocus) {
						return;
					}
					isHandlingFocus = true;

					int idx = Arrays.asList(textFields12List).indexOf(textField);
					if (newValue) { // if focus is gained
						// Only set the text if the mnemonicWordList size is 12
						if (idx != -1 && idx < mnemonicWordList.size()
							&& mnemonicWordList.size() == 12) {
							textField.setText(mnemonicWordList.get(idx));
						}
					} else { // if focus is lost
						if (idx != -1 && idx < mnemonicWordList.size()) {
							if (!textField.getText().equals(placeholderText)) {
								mnemonicWordList.set(idx, textField.getText());
							}
						}
						if (textField.getText().isEmpty() || textField.getText()
							.equals(mnemonicWordList.get(idx))) {
							textField.setText(placeholderText);
						}
					}
					isHandlingFocus = false;
				});
			}

			for (TextField textField : textFields24List) {
				textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
					System.out.println("THIS SHOULD NOT BE TRIGGERED textFields24List: "
						+ mnemonicWordList.get(0));
					if (isHandlingFocus) {
						return;
					}
					isHandlingFocus = true;
					int idx = Arrays.asList(textFields24List).indexOf(textField);
					if (newValue) { // if focus is gained
						if (idx != -1 && idx < mnemonicWordList.size()) {
							textField.setText(mnemonicWordList.get(idx));
						}
					} else { // if focus is lost
						if (idx != -1 && idx < mnemonicWordList.size()) {
							if (!textField.getText().equals(placeholderText)) {
								mnemonicWordList.set(idx, textField.getText());
							}
						}
						if (textField.getText().isEmpty() || textField.getText()
							.equals(mnemonicWordList.get(idx))) {
							if (idx != 0 || !textField.getText().isEmpty()) {
								textField.setText(placeholderText);
							}
						}
					}
					isHandlingFocus = false;
				});
			}

			for (TextField textField : textFields24ImportList) {
				textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
					System.out.println("mnemonicWordList.get(0): "
						+ mnemonicWordList.get(0));
					if (isHandlingFocus) {
						return;
					}
					isHandlingFocus = true;

					int idx = Arrays.asList(textFields24ImportList).indexOf(textField);
					if (newValue) { // if focus is gained

						if (idx != -1 && idx < mnemonicWordList.size()) {
							textField.setText(mnemonicWordList.get(idx));
						}
					} else { // if focus is lost
						if (idx != -1 && idx < mnemonicWordList.size()) {
							if (!textField.getText().equals(placeholderText)) {
								mnemonicWordList.set(idx, textField.getText());
							}
						}
						if (textField.getText().isEmpty() || textField.getText()
							.equals(mnemonicWordList.get(idx))) {
							if (idx != 0 || !textField.getText().isEmpty()) {
								textField.setText(placeholderText);
							}
						}
					}
					isHandlingFocus = false;
				});
			}

		}
		);

	}

	private void handlePasteEvent(KeyEvent event) {
		if (event.getSource() != textFields12List[0] && event.getSource() != textFields24ImportList[0]) {
			return; // Exit if the source is not the first text field
		}
		if (event.isControlDown() && event.getCode() == KeyCode.V) {
			handlePaste();
			event.consume();
		}
	}

	private void handlePaste() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasString()) {
			String mnemonic = clipboard.getString();
			String[] words = mnemonic.split("\\s+"); // Split by whitespace
			mnemonicWordList.clear();
			mnemonicWordList.addAll(Arrays.asList(words));
			mnemonicModel.setMnemonicWordList(mnemonicWordList);
			System.out.println("Words array: " + Arrays.toString(words));
			System.out.println("mnemonicWordList after paste: " + mnemonicWordList);
			if (mnemonicModel.getCurrentPane().equals("importPane")) {
				tabRequestEvent.fire(TabRequestSignal.builder()
					.action("select").build());
			}

			if (words.length == 12) {
				// Remove focus from the first text field
				for (int i = 0; i < words.length; i++) {
					if (textFields12List[i] != null) {
						textFields12List[i].setText(placeholderText);
					}
				}
			} else if (words.length == 24) {
				if (mnemonicModel.getCurrentPane().equals("importPane")) {

					for (TextField textField : textFields12List) {
						textField.setText("");
					}

					for (int i = 0; i < words.length; i++) {
						if (textFields24ImportList[i] != null) {
							textFields24ImportList[i].setText(placeholderText);
							System.out.println("mnemonicWordList.get(i): "
								+ mnemonicWordList.get(i));

						}
					}
				} else {
					for (int i = 0; i < words.length; i++) {
						if (textFields24List[i] != null) {
							textFields24List[i].setText(placeholderText);
						}
					}
				}
			}
		}
	}

	@FXML
	private void handleTextFieldClick12(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields12List);
	}

	@FXML
	private void handleTextFieldClick24(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields24List);
	}

	@FXML
	private void handleTextFieldClick24Import(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields24ImportList);
	}

	private void handleTextFieldClickCommon(MouseEvent event, TextField[] currentList) {
		TextField clickedField = (TextField) event.getSource();

		int index = -1;
		for (int i = 0; i < currentList.length; i++) {
			if (currentList[i] == clickedField) {
				index = i;
				break;
			}
		}

		if (index != -1 && index < mnemonicWordList.size()) {
			clickedField.setPromptText("•••••");
			clickedField.setText(mnemonicWordList.get(index));
		}
	}

	private void handleResetTextFields(@Observes ResetTextFieldsSignal signal) {
		resetTextFields();
	}

	private void resetTextFields() {
		for (TextField textField : textFields12List) {
			textField.setText("");
		}

		for (TextField textField : textFields24List) {
			textField.setText("");
		}

		for (TextField textField : textFields24ImportList) {
			textField.setText("");
		}
		mnemonicWordList.clear();
	}
}
