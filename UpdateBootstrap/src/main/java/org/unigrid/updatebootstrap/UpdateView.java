/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.unigrid.updatebootstrap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
	private static String startLoacation = getBaseDirectory();

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

						Path zip = Paths.get(getBaseDirectory(), "temp");
						System.out.println("zip location: " + zip.toString());

						System.out.println("depenendencies location: " + getBaseDirectory().toString());

						if (config.update(UpdateOptions.archive(zip).updateHandler(UpdateView.this))
								.getException() == null) {
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
							Throwable s = config.update(UpdateOptions.archive(zip).updateHandler(UpdateView.this))
									.getException();
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

		File archive = new File(startLoacation + "/lib/" + untarName);
		File destination = new File(startLoacation + "/bin/");

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

		Path source = Paths.get(startLoacation + "/lib/" + untarName);
		Path target = Paths.get(startLoacation + "/bin/");
		try {
			unzipFolder(source, target);
			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void unzipFolder(Path source, Path target) throws IOException {
		final Path endDir = Paths.get(startLoacation + "/bin");
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

			// list files in zip
			ZipEntry zipEntry = zis.getNextEntry();

			while (zipEntry != null) {

				boolean isDirectory = false;
				// detect directories
				if (zipEntry.getName().endsWith(File.separator)) {
					isDirectory = true;
				}

				Path newPath = zipSlipProtect(zipEntry, target);

				if (isDirectory) {
					Files.createDirectories(newPath);
					System.out.println("isDirectory");
				} else {
					if (newPath.getParent() != null) {
						if (Files.notExists(newPath.getParent())) {
							Files.createDirectories(newPath.getParent());
						}
					}

					Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
					final Path moveName = Paths.get(endDir + "/" + new File(zipEntry.getName()).getName().toString());
					System.out.println("moveName: " + moveName.toString());
					// copy daemons to bin directory
					Files.copy(newPath, moveName, StandardCopyOption.REPLACE_EXISTING);					
				}

				zipEntry = zis.getNextEntry();

			}
			zis.closeEntry();

		}

	}

	public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
			throws IOException {

		Path targetDirResolved = targetDir.resolve(zipEntry.getName());

		Path normalizePath = targetDirResolved.normalize();
		if (!normalizePath.startsWith(targetDir)) {
			throw new IOException("Bad zip entry: " + zipEntry.getName());
		}

		return normalizePath;
	}

	public void launchApp() {
		config.launch();
	}

	private void run(Runnable runnable) {
		Thread runner = new Thread(runnable);
		runner.setDaemon(true);
		runner.start();
	}

	private static String getBaseDirectory() {
		String blockRoot = "";
		switch (OS.CURRENT) {
			case LINUX:
				blockRoot = System.getProperty("user.home").concat("/.unigrid/dependencies");
				break;
			case WINDOWS:
				blockRoot = System.getProperty("user.home").concat("/AppData/Roaming/unigrid/dependencies");
				break;
			case MAC:
				blockRoot = System.getProperty("user.home").concat("/Library/Application Support/unigrid/dependencies");
				break;
			default:
				blockRoot = System.getProperty("user.home").concat("/unigrid/dependencies");
				break;
		}

		File depenendencies = new File(blockRoot);
		if (!depenendencies.exists()) {
			depenendencies.mkdirs();
		}
		return blockRoot;
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
			// progress.setProgress(frac);
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
