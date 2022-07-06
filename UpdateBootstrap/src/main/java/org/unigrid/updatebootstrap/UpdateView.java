/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.unigrid.updatebootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.UpdateOptions;
import org.update4j.inject.InjectSource;
import org.update4j.inject.Injectable;
import org.update4j.service.UpdateHandler;
import org.update4j.OS;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UpdateView implements UpdateHandler, Injectable, Initializable {

	private Configuration config;

	@FXML
	private Label status;

	@FXML
	private ProgressBar progress;

	@FXML
	private Button quit;

	@FXML
	private Button launch;

	private DoubleProperty primaryPercent;
	private DoubleProperty secondaryPercent;

	private BooleanProperty running;
	private boolean abort;

	@InjectSource
	private Stage primaryStage;

	private static UpdateView updateView = null;

	private UpdateView() {

	}

	public static UpdateView getInstance() {
		if (updateView == null) {
			updateView = new UpdateView();
		}
		return updateView;
	}

	public Stage getStage() {
		return primaryStage;
	}

	public void setConfig(Configuration config, Stage primaryStage) {
		this.config = config;
		this.primaryStage = primaryStage;

		status = (Label) primaryStage.getScene().lookup("#status");
		progress = (ProgressBar) primaryStage.getScene().lookup("#progress");

		quit = (Button) primaryStage.getScene().lookup("#quit");
		launch = (Button) primaryStage.getScene().lookup("#launch");

		primaryPercent = new SimpleDoubleProperty(this, "primaryPercent");
		secondaryPercent = new SimpleDoubleProperty(this, "secondaryPercent");

		launch.setDisable(true);

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
		List<FileMetadata> files = config.getFiles();

		for (FileMetadata file : files) {
			System.out.println(file.getUri());
		}
		if (running.get()) {
			abort = true;
			System.out.println("the application is running");
			return;
		}

		System.out.println("the application is not running");
		running.set(true);

		status.setText("Checking for updates...");

		Task<Boolean> checkUpdates = checkUpdates();
		checkUpdates.setOnSucceeded(evt -> {
			if (!checkUpdates.getValue()) {
				progress.setProgress(1);
				status.setText("No updates found");
				running.set(false);
				launch();
			} else {
				Task<Void> doUpdate = new Task<>() {

					@Override
					protected Void call() throws Exception {
						System.out.println("calling the zip");

						Path zip = Paths.get(System.getProperty("user.home"), "unigrid", "temp");
						System.out.println(zip);
						if (config.update(UpdateOptions.archive(zip).updateHandler(UpdateView.this)).getException() == null) {
							System.out.println("Do the install");
							Archive.read(zip).install(true);
							System.out.println("Install done!!");
							if (OS.CURRENT == OS.LINUX || OS.CURRENT == OS.MAC) {
								untarDaemonLinux();
							} else {
								unzipDaemonWindows();
							}
							launch();
						} else {
							Throwable s = config.update(UpdateOptions.archive(zip).updateHandler(UpdateView.this)).getException();
							System.out.println(s);
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

	private void launch() {
		launch.setDisable(false);
	}

	private void untarDaemonLinux() {

		List<FileMetadata> files = config.getFiles();
		String untarName = "";
		for (FileMetadata file : files) {
			if (!file.isModulepath()) {
				String s = file.getUri().toString();
				String[] arr = s.split("/");
				untarName = arr[arr.length - 1];
			}
		}
		System.out.println(untarName);
		String startLoacation = System.getProperty("user.home");
		File archive = new File(startLoacation + "/unigrid/lib/" + untarName);
		File destination = new File(startLoacation + "/unigrid/bin/");

		if (!destination.exists()) {
			destination.mkdirs();
		} else {
			File[] bin = destination.listFiles();
			for (File a : bin) {
				if (a.isDirectory()) {
					File[] unigrid = a.listFiles();
					for (File b : unigrid) {
						File[] bintar = b.listFiles();
						for (File file : bintar) {
							file.delete();
						}
						b.delete();
					}
				}

				a.delete();
			}
		}
		try {
			var pb = new ProcessBuilder("tar", "-xf", archive.toString(), "-C", destination.toString());
			var process = pb.start();
	
			try (var reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {

				String line;
	
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		try {
			var pb = new ProcessBuilder();
			if (OS.CURRENT == OS.MAC) {
				pb.command("find", destination.toString(), "-perm", "+111", "-type", "f", "-name", "unigrid*");
			} else {
				pb.command("find", destination.toString(), "-type", "f", "-name", "unigrid*");
			}

			var process = pb.start();
	
			try (var reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {
	
				String line;
	
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
					var cp = new ProcessBuilder("cp", line, destination.toString());
					cp.start();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	private void unzipDaemonWindows() {
		String[] filesToMove = new String[3];
		List<FileMetadata> files = config.getFiles();
		String untarName = "";
		for (FileMetadata file : files) {
			if (!file.isModulepath()) {
				String s = file.getUri().toString();
				String[] arr = s.split("/");
				untarName = arr[arr.length - 1];
			}
		}
		System.out.println(untarName);
		String startLoacation = System.getProperty("user.home") + "/AppData/Roaming";
		File archive = new File(startLoacation + "/unigrid/lib/" + untarName);
		File destination = new File(startLoacation + "/unigrid/bin/");

		if (!destination.exists()) {
			destination.mkdirs();
		} else {
			File[] bin = destination.listFiles();
			for (File a : bin) {
				if (a.isDirectory()) {
					File[] unigrid = a.listFiles();
					for (File b : unigrid) {
						File[] bintar = b.listFiles();
						for (File file : bintar) {
							file.delete();
						}
						b.delete();
					}
				}

				a.delete();
			}
		}
		try {
			byte[] buffer = new byte[1024];
			ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
			System.out.println(zis.available());
			System.out.println(zis.getNextEntry());
			ZipEntry zipEntry = zis.getNextEntry();
			System.out.println(zipEntry.getSize());
			int counter = 0;
			while (zipEntry != null) {
				File newFile = new File(destination, zipEntry.getName());
				System.out.println("Zipentry is not null");
				if (zipEntry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
					}
				} else {
					File parent = newFile.getParentFile();

					if (!parent.isDirectory() && !parent.mkdirs()) {
					}

					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
					filesToMove[counter] = zipEntry.getName();
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (Exception e) {
			System.err.println("it all whent to shit");
			System.err.println(e.getMessage());
		}
		try {
			for (String s : filesToMove) {
				Runtime.getRuntime().exec("xcopy " + Paths.get(destination.getAbsolutePath() + "/" + s)
					+ " " + destination.getAbsolutePath());
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	public void launchApp() {
		config.launch();
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
