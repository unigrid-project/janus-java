package org.unigrid.bootstrap.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.unigrid.bootstrap.App;

public class ChainSelectionController {

    @FXML
    private Button mainnetButton;

    @FXML
    private Button legacyButton;

    @FXML
    public void initialize() {
        mainnetButton.setOnAction(event -> handleSelection("mainnet"));
        legacyButton.setOnAction(event -> handleSelection("legacy"));
    }

    private void handleSelection(String chain) {
        // Pass the selected chain to the App class
        try {
            App app = new App();
            app.loadUpdateView(chain);
            // Close the current stage
            Stage stage = (Stage) mainnetButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
