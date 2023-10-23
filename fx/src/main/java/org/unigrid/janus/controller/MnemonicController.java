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
	@FXML
	private TextField wordTwentyFourConfirmOne, wordTwentyFourConfirmTwo, wordTwentyFourConfirmThree,
		wordTwentyFourConfirmFour, wordTwentyFourConfirmFive;
	@FXML
	private TextField wordTwentyFourConfirmSix, wordTwentyFourConfirmSeven, wordTwentyFourConfirmEight,
		wordTwentyFourConfirmNine, wordTwentyFourConfirmTen;
	@FXML
	private TextField wordTwentyFourConfirmEleven, wordTwentyFourConfirmTwelve, wordTwentyFourConfirmThirteen,
		wordTwentyFourConfirmFourteen, wordTwentyFourConfirmFifteen;
	@FXML
	private TextField wordTwentyFourConfirmSixteen, wordTwentyFourConfirmSeventeen, wordTwentyFourConfirmEighteen,
		wordTwentyFourConfirmNineteen, wordTwentyFourConfirmTwenty;
	@FXML
	private TextField wordTwentyFourConfirmTwentyOne, wordTwentyFourConfirmTwentyTwo,
		wordTwentyFourConfirmTwentyThree, wordTwentyFourConfirmTwentyFour;

	private TextField[] textFields12List;
	private TextField[] textFields24ImportList;
	private TextField[] textFields24Confirm;
	private List<String> mnemonicWordList = new ArrayList<>();
	private final String placeholderText = "•••••";
	private boolean isHandlingFocus = false;

	@Override
	public void initialize(URL url, ResourceBundle rb) {

		Platform.runLater(() -> {
			System.out.println("Platform.runLater");
			initializeTextFields12();
			//initializeTextFields24();
			initializeTextFields24Import();
			initializeTextFields24Confirm();
			wordTwelveOne.setOnKeyPressed(this::handlePasteEvent);

			//wordTwentyFourOne.setOnKeyPressed(this::handlePasteEvent);
			wordTwentyFourImportOne.setOnKeyPressed(this::handlePasteEvent);

			wordTwentyFourConfirmOne.setOnKeyPressed(this::handlePasteEvent);

			for (TextField textField : textFields12List) {
				handleFocusEventFor12WordMnemonic(textField);
			}

			for (TextField textField : textFields24ImportList) {
				handleFocusEventFor24WordMnemonicImport(textField);
			}

			for (TextField textField : textFields24Confirm) {
				handleFocusEventFor24WordMnemonicConfirm(textField);
			}

		}
		);

	}

	private void initializeTextFields12() {
		textFields12List = new TextField[]{wordTwelveOne, wordTwelveTwo,
			wordTwelveThree, wordTwelveFour, wordTwelveFive, wordTwelveSix,
			wordTwelveSeven, wordTwelveEight, wordTwelveNine, wordTwelveTen,
			wordTwelveEleven, wordTwelveTwelve};
		//setTextFieldEventHandlers(textFields12List);
		System.out.println("initializeTextFields12 " + textFields12List);
	}

	private void initializeTextFields24Import() {

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
		System.out.println("initializeTextFields24Import " + textFields24ImportList);
		//setTextFieldEventHandlers(textFields24ImportList);
	}

	private void initializeTextFields24Confirm() {

		textFields24Confirm = new TextField[]{
			wordTwentyFourConfirmOne, wordTwentyFourConfirmTwo, wordTwentyFourConfirmThree,
			wordTwentyFourConfirmFour, wordTwentyFourConfirmFive, wordTwentyFourConfirmSix,
			wordTwentyFourConfirmSeven, wordTwentyFourConfirmEight, wordTwentyFourConfirmNine,
			wordTwentyFourConfirmTen, wordTwentyFourConfirmEleven, wordTwentyFourConfirmTwelve,
			wordTwentyFourConfirmThirteen, wordTwentyFourConfirmFourteen, wordTwentyFourConfirmFifteen,
			wordTwentyFourConfirmSixteen, wordTwentyFourConfirmSeventeen, wordTwentyFourConfirmEighteen,
			wordTwentyFourConfirmNineteen, wordTwentyFourConfirmTwenty, wordTwentyFourConfirmTwentyOne,
			wordTwentyFourConfirmTwentyTwo, wordTwentyFourConfirmTwentyThree,
			wordTwentyFourConfirmTwentyFour
		};
		System.out.println("initializeTextFields24Confirm " + textFields24Confirm);
	}

	private void handlePasteEvent(KeyEvent event) {
		if (event.getSource() != textFields12List[0] && event.getSource()
			!= textFields24ImportList[0] && event.getSource() != textFields24Confirm[0]) {
			reset();
			return; // Exit if the source is not the first text field
		}
		if (event.isControlDown() && event.getCode() == KeyCode.V) {
			handlePaste();
			event.consume();
		}
	}

	private void handlePaste() {
		System.out.println("handlePaste");
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasString()) {
			String mnemonic = clipboard.getString().trim();  // Trim to remove leading/trailing whitespaces
			String[] words = mnemonic.split("\\s+"); // Split by whitespace

			if (mnemonicModel.getCurrentPane().equals("confirmMnemonic")) {
				if (words.length == 24) {
					mnemonicWordList.clear();
					mnemonicWordList.addAll(Arrays.asList(words));
					mnemonicModel.setMnemonicWordList(mnemonicWordList);
					setPlaceholderText(textFields24Confirm);
				} else {
					showErrorMessage("Invalid number of words. "
						+ "Please enter 24 words for confirmation.");
				}
			} else {
				if (words.length != 12 && words.length != 24) {
					TabRequestSignal signal = TabRequestSignal.builder()
						.action("invalid")
						.wordListLength(words.length)
						.build();
					tabRequestEvent.fire(signal);
					reset();
					return;
				} else {
					mnemonicWordList.clear();
					mnemonicWordList.addAll(Arrays.asList(words));
					mnemonicModel.setMnemonicWordList(mnemonicWordList);
					String action = words.length == 12 ? "select12" : "select";
					handleTabSwitch(action, words);
				}
			}
		} else {
			showErrorMessage("Clipboard is empty. Please copy your mnemonic phrase first.");
		}
	}

	private void handleTabSwitch(String action, String[] words) {
		TabRequestSignal signal = TabRequestSignal.builder()
			.action(action)
			.wordListLength(words.length)
			.build();
		signal.setCallback(shouldProceed -> {
			if (shouldProceed) {
				mnemonicWordList.clear();
				mnemonicWordList.addAll(Arrays.asList(words));
				mnemonicModel.setMnemonicWordList(mnemonicWordList);

				if ("select".equals(action)) {
					clearTextFields(textFields12List);
					setPlaceholderText(textFields24ImportList);
				} else if ("select12".equals(action)) {
					clearTextFields(textFields24ImportList);
					setPlaceholderText(textFields12List);
				}
			}
		});
		tabRequestEvent.fire(signal);
	}

	private void showErrorMessage(String message) {
		// Implement this method to show an error message to the user
		System.err.println(message);
	}

	private void setPlaceholderText(TextField[] textFields) {
		for (int i = 0; i < textFields.length; i++) {
			if (textFields[i] != null) {
				textFields[i].setText(placeholderText);
			}
		}
	}

	private void clearTextFields(TextField[] textFields) {
		for (TextField textField : textFields) {
			if (textField != null) {
				textField.setText("");
				textField.setPromptText("");
			}
		}
	}

	@FXML
	private void handleTextFieldClick12(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields12List);
	}

	@FXML
	private void handleTextFieldClick24Import(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields24ImportList);
	}

	@FXML
	private void handleTextFieldClick24Confirm(MouseEvent event) {
		handleTextFieldClickCommon(event, textFields24Confirm);
	}

	private void handleTextFieldClickCommon(MouseEvent event, TextField[] currentList) {
		if (mnemonicWordList.isEmpty()) {
			System.out.println("Mnemonic word list is empty. Ignoring click event.");
			return;
		}

		TextField clickedField = (TextField) event.getSource();
		int index = Arrays.asList(currentList).indexOf(clickedField);

		System.out.println("Clicked field index: " + index);
		System.out.println("Mnemonic word list: " + mnemonicWordList);

		if (index != -1 && index < mnemonicWordList.size()) {
			clickedField.setText(mnemonicWordList.get(index));
		} else {
			clickedField.clear();
		}
	}

	private void handleResetTextFields(@Observes ResetTextFieldsSignal signal) {
		reset();
	}

	/* HANDLE FOCUS */
	private void handleFocusEventFor12WordMnemonic(TextField textField) {
		System.out.println("Entering 12-word Mnemonic block");
		handleFocusEventCommon(textField, Arrays.asList(textFields12List), placeholderText);
		System.out.println("Exiting 12-word Mnemonic block");
	}

	private void handleFocusEventFor24WordMnemonicImport(TextField textField) {
		System.out.println("Entering 24-word Mnemonic Import block");
		handleFocusEventCommon(textField, Arrays.asList(textFields24ImportList), placeholderText);
		System.out.println("Exiting 24-word Mnemonic Import block");
	}

	private void handleFocusEventFor24WordMnemonicConfirm(TextField textField) {
		System.out.println("Entering 24-word Mnemonic Confirm block");
		handleFocusEventCommon(textField, Arrays.asList(textFields24Confirm), placeholderText);
		System.out.println("Exiting 24-word Mnemonic Confirm block");
	}

	private void handleFocusEventCommon(TextField textField, List<TextField> textFieldList, String placeholderText) {
		textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			int idx = textFieldList.indexOf(textField);
			if (idx == -1 || idx >= mnemonicWordList.size()) {
				System.err.println("Index out of bounds. Exiting focus event handling.");
				return;
			}

			if (isHandlingFocus) {
				return;
			}
			isHandlingFocus = true;

			if (newValue) { // if focus is gained
				System.out.println("Focus gained. Text: " + textField.getText());
				if (textField.getText().equals(placeholderText)) {
					textField.clear();
				}
			} else { // if focus is lost
				System.out.println("Focus lost. Text: " + textField.getText());
				textField.setText(placeholderText);
			}
			isHandlingFocus = false;
		});
	}

	public void reset() {
		// Clear all text fields
		for (TextField textField : textFields12List) {
			textField.clear();
			//clickedField.setPromptText("•••••");
		}

		for (TextField textField : textFields24ImportList) {
			textField.clear();
		}
		for (TextField textField : textFields24Confirm) {
			textField.clear();
		}

		// Clear the mnemonic word list
		mnemonicWordList.clear();

		// Reset the models
		mnemonicModel.reset();  // Assuming you have a reset method in your model class
	}

}
