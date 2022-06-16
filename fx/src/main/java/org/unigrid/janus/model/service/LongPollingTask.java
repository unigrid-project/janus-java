package org.unigrid.janus.model.service;

import java.util.TimerTask;
import javafx.application.Platform;
import org.unigrid.janus.model.Wallet;

public class LongPollingTask extends TimerTask {

	private static DebugService debug = new DebugService();
	private static PollingService polling = new PollingService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	public LongPollingTask() {
		debug.log("Long polling task created!");
	}

	public void run() {
		Platform.runLater(() -> {
			try {
				window.getDocsController().pullNewDocumentaion();
			} catch (Exception e) {
				debug.print("error getting docs: ".concat(e.getMessage()), LongPollingTask.class.getSimpleName());	
				polling.stopPolling();
			}
		});
	}
}
