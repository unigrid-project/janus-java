package org.unigrid.janus.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.signal.UsedSpace;
import org.w3c.dom.Document;
import netscape.javascript.JSObject;
import org.unigrid.janus.model.service.PaymentResponse;

@ApplicationScoped
public class StorageController implements Initializable, PropertyChangeListener {

	@Inject private DebugService debug;
	@FXML
	private Label usedSpace;
	@FXML
	private StackPane webViewPane;
	@FXML
	private StackPane paymentView;
	private WebView webView = new WebView();

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		debug.log("Initializing transactions");
		setupWebViews();
	}

	private void eventUsedSpace(@Observes UsedSpace usedSpaceEvent) {
		// the event is triggered before the initialization!
		if (Objects.nonNull(usedSpace)) {
			Platform.runLater(() -> usedSpace.setText(Long.toString(usedSpaceEvent.getSize())));
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

	private void setupWebViews() {
		// TODO Auto-generated method stub

		webViewPane.setVisible(false);
		paymentView.setVisible(true);

	}

	@FXML
	public void onPayWithFiatClicked(MouseEvent event) {
		//webView.getEngine().load("https://google.com");

		WebEngine webEngine = webView.getEngine();
		webViewPane.getChildren().add(webView);
		Worker<Void> worker = webEngine.getLoadWorker();
//		worker.stateProperty().addListener((obs, oldValue, newValue) -> {
//			if (newValue == Worker.State.SUCCEEDED) {
//				// The request has completed successfully
//				// Call a JavaScript function
//				String response = (String) webEngine.executeScript("exampleFunction('janus call')");
//				// the response variable will contain the value returned by the javascript function
//				// you can cast it to the appropriate type and use it as needed
//				System.out.println(response);
//			}
//			System.out.println(newValue);
//		});

		webEngine.getLoadWorker().stateProperty().addListener(
			new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				System.out.println("oldValue: " + oldValue);
				System.out.println("newValue: " + newValue);

				if (newValue != Worker.State.SUCCEEDED) {
					return;
				}
				System.out.println("Succeeded!");
				Document document = webEngine.getDocument();
//				String hello = (String) webEngine.executeScript("bullshit()");
//				String hello = (String) webEngine.executeScript("exampleFunction()");
				System.out.println("after exec call!");
				JSObject window = (JSObject) webEngine.executeScript("window");
				window.setMember("payResponse", new PaymentResponse());
//				System.out.println("hello: " + hello); 

			}
		});

		webView.getEngine().load("https://unigrid.ong/");
		webViewPane.setVisible(true);
		paymentView.setVisible(false);
	}

	@FXML
	public void onPayWithCryptoClicked(MouseEvent event) {
		//webView.getEngine().load("https://google.com");

		WebEngine webEngine = webView.getEngine();
		webViewPane.getChildren().add(webView);
		Worker<Void> worker = webEngine.getLoadWorker();
//		worker.stateProperty().addListener((obs, oldValue, newValue) -> {
//			if (newValue == Worker.State.SUCCEEDED) {
//				// The request has completed successfully
//				// Call a JavaScript function
//				String response = (String) webEngine.executeScript("exampleFunction('janus call')");
//				// the response variable will contain the value returned by the javascript function
//				// you can cast it to the appropriate type and use it as needed
//				System.out.println(response);
//			}
//			System.out.println(newValue);
//		});

		webEngine.getLoadWorker().stateProperty().addListener(
			new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				System.out.println("oldValue: " + oldValue);
				System.out.println("newValue: " + newValue);

				if (newValue != Worker.State.SUCCEEDED) {
					return;
				}
				System.out.println("Succeeded!");
				Document document = webEngine.getDocument();
//				String hello = (String) webEngine.executeScript("bullshit()");
//				String hello = (String) webEngine.executeScript("exampleFunction()");
				System.out.println("after exec call!");
				JSObject window = (JSObject) webEngine.executeScript("window");
				window.setMember("payResponse", new PaymentResponse());
//				System.out.println("hello: " + hello); 

			}
		});

		webView.getEngine().load("https://unigrid.ong/coinbase");
		webViewPane.setVisible(true);
		paymentView.setVisible(false);
	}
}
