package org.unigrid.janus.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import org.unigrid.janus.model.signal.UsedSpace;

@ApplicationScoped
public class MountedDriveController  implements Initializable, PropertyChangeListener {

	
	@FXML private TextField tf1;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
	
		//checkMountSize();
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		//throw new UnsupportedOperationException("Not supported yet."); 
	}

	private void eventUsedSpace(@Observes UsedSpace usedSpace) {
		Platform.runLater(() -> tf1.setText(Long.toString(usedSpace.getSize())));
	}
	
	private void eventUsedSpaceAsync(@ObservesAsync UsedSpace usedSpace) {
		Platform.runLater(() -> tf1.setText(Long.toString(usedSpace.getSize())));
	}
}
