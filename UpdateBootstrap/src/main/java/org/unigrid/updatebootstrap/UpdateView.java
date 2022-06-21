/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.unigrid.updatebootstrap;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.UpdateOptions;
import org.update4j.inject.InjectSource;
import org.update4j.inject.Injectable;
import org.update4j.service.UpdateHandler;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UpdateView implements UpdateHandler, Injectable, Initializable {

	private Configuration config;

	@FXML
	private Label status;

	@FXML
	private Label output;

	@FXML
	private ProgressBar progress;

	private DoubleProperty primaryPercent;
	private DoubleProperty secondaryPercent;

	private BooleanProperty running;
	private boolean abort;

	@InjectSource
	private Stage primaryStage;

	public UpdateView(Configuration config, Stage primaryStage) {
		this.config = config;
		this.primaryStage = primaryStage;

		status = (Label) primaryStage.getScene().lookup("#status");
		output = (Label) primaryStage.getScene().lookup("#output");
		progress = (ProgressBar) primaryStage.getScene().lookup("#progress");

		primaryPercent = new SimpleDoubleProperty(this, "primaryPercent");
		secondaryPercent = new SimpleDoubleProperty(this, "secondaryPercent");

		running = new SimpleBooleanProperty(this, "running");

		status.setOpacity(0);
		FadeTransition fade = new FadeTransition(Duration.seconds(1.5), status);
		fade.setToValue(0);

		running.addListener((obs, ov, nv) -> {
			if (nv) {
				fade.stop();
				status.setOpacity(1);
			} else {
				fade.playFromStart();
			}
		});
		System.out.println("before update");
		update();
	}

	void update() {
		if (running.get()) {
			abort = true;
			System.out.println("the applicatoin is running");
			output.setText("the application is running;");
			return;
		}
		
		System.out.println("the application is not running");
		running.set(true);

		status.setText("Checking for updates...");

		Task<Boolean> checkUpdates = checkUpdates();
		checkUpdates.setOnSucceeded(evt -> {
			if (!checkUpdates.getValue()) {
				status.setText("No updates found");
				running.set(false);
				launch();
			} else {
				Task<Void> doUpdate = new Task<>() {

					@Override
					protected Void call() throws Exception {
						System.out.println("calling the zip");
						Path zip = Paths.get("/home/marcus/Documents/unigrid/wallet-update.zip");
						
						if (config.update(UpdateOptions.archive(zip).updateHandler(UpdateView.this)).getException() == null) {
							System.out.println("Do the install");
							Archive.read(zip).install();
							System.out.println("Install done!!");
							launch();
						}
						else {
							System.out.println("updatehandler = null");
							launch();
						}

						return null;
					}

				};

				run(doUpdate);
			}
		});

		run(checkUpdates);
	}
	
	private Task<Boolean> checkUpdates() {
		return new Task<>() {

			@Override
			protected Boolean call() throws Exception {
				System.out.println("update required: " + config.requiresUpdate());
				return config.requiresUpdate();
			}

		};
	}
	
	private void launch(){
		Thread run = new Thread(() -> {
			config.launch(this);
		});
		run.start();
		
	}

	private void run(Runnable runnable) {
		Thread runner = new Thread(runnable);
		runner.setDaemon(true);
		runner.start();
	}

	@Override
	public void updateDownloadFileProgress(FileMetadata file, float frac) {
		Platform.runLater(() -> {
			status.setText("Downloading " + file.getPath().getFileName() + " (" + ((int) (100 * frac)) + "%)");
			secondaryPercent.set(frac);
			progress.setProgress(frac);
		});
	}

	@Override
	public void updateDownloadProgress(float frac) {
		Platform.runLater(() -> {
			primaryPercent.set(frac);
			//progress.setProgress(frac);
			});
	}

	@Override
	public void succeeded() {
		Platform.runLater(() -> status.setText("Download complete"));
	}

	@Override
	public void stop() {
		Platform.runLater(() -> running.set(false));
		abort = false;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {

	}
	
}
