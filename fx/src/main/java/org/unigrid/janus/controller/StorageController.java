package org.unigrid.janus.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.unigrid.janus.model.signal.UsedSpace;

@ApplicationScoped
public class StorageController {

	@FXML
	private Label usedSpace;

	private void eventUsedSpace(@Observes UsedSpace usedSpaceEvent) {
		// the event is triggered before the initialization!
		if (Objects.nonNull(usedSpace)) {
			Platform.runLater(() -> usedSpace.setText(Long.toString(usedSpaceEvent.getSize())));
		}
	}
}
