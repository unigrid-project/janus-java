/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */
package org.unigrid.bootstrap;

import io.sentry.Sentry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.animation.FadeTransition;
import javafx.application.HostServices;
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
import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.UpdateOptions;
import org.update4j.inject.InjectSource;
import org.update4j.inject.Injectable;
import org.update4j.service.UpdateHandler;
import org.update4j.OS;

public class UpdateView implements UpdateHandler, Injectable, Initializable {

	private Configuration config;
	private DoubleProperty primaryPercent;
	private DoubleProperty secondaryPercent;

	private BooleanProperty running;
	private boolean abort;

	@FXML
	private Label status;
	@FXML
	private ProgressBar progress;
	@FXML
	private Button quit;
	@FXML
	private Button launch;

	private Stage primaryStage;

	private Injectable inject;

	private String bootstrapVersion = "";
	private static UpdateView updateView = null;
	private static String startLoacation = getBaseDirectory();

	public static UpdateView getInstance() {
		if (updateView == null) {
			updateView = new UpdateView();
		}
		return updateView;
	}

	public Stage getStage() {
		return primaryStage;
	}

	public void setConfig(Configuration config, Stage primaryStage, Map<String, String> input, HostServices hostServices) {
		this.config = config;
		this.primaryStage = primaryStage;
		final Properties properties = new Properties();

		try {
			properties.load(getClass().getResourceAsStream("application.properties"));
			bootstrapVersion = Objects.requireNonNull(properties.getProperty("proj.ver"));
			System.out.println("bootstrap version: " + bootstrapVersion);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
		}

		inject = new Injectable() {
			@InjectSource
			Map<String, String> inputArgs = input;
			@InjectSource
			HostServices hostService = hostServices;
			@InjectSource
			String bootstrapVer = bootstrapVersion;
		};

		System.out.println(input.get("URL"));
		status = (Label) primaryStage.getScene().lookup("#status");
		progress = (ProgressBar) primaryStage.getScene().lookup("#progress");

		quit = (Button) primaryStage.getScene().lookup("#quit");
		launch = (Button) primaryStage.getScene().lookup("#launch");

		primaryPercent = new SimpleDoubleProperty(this, "primaryPercent");
		secondaryPercent = new SimpleDoubleProperty(this, "secondaryPercent");

		launch.setDisable(true);
		running = new SimpleBooleanProperty(this, "running");

		status.setOpacity(0);
		FadeTransition fade = new FadeTransition(Duration.seconds(5), status);
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
		removeOldJars(config);
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
		final Object launchTrigger = new Object();

		checkUpdates.setOnSucceeded(evt -> {
			if (!checkUpdates.getValue()) {
				if(!daemonDirExists()) {
					extractDaemon();
				}
				progress.setProgress(1);
				status.setText("No updates found");
				running.set(false);
			} else {
				Task<Void> doUpdate = new Task<>() {
					@Override
					protected Void call() throws Exception {
						System.out.println("calling the zip");
						Path zip = Paths.get(getBaseDirectory(), "zip");

						System.out.println("zip location: " + zip.toString());
						System.out.println("depenendencies location: " + getBaseDirectory());

						Throwable s = config.update(UpdateOptions.archive(zip)
							.updateHandler(UpdateView.this)).getException();

						if (Objects.isNull(s)) {
							System.out.println("Do the install");
							Archive.read(zip).install(true);
							System.out.println("Install done!!");
							if(!daemonDirExists()) {
								extractDaemon();
							}
							synchronized (launchTrigger) {
								launchTrigger.notifyAll();
							}
						} else {
							Sentry.captureException(s);
							System.out.println(s);
							System.out.println("updatehandler = null");
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									status.setText("No updates found");
								}
							});

							synchronized (launchTrigger) {
								launchTrigger.notifyAll();
							}
						}

						return null;
					}

				};

				run(doUpdate);

				synchronized (launchTrigger) {
					try {
						launchTrigger.wait();
					} catch (InterruptedException ex) {
						/* Empty on purpose */
					}
				}
			}

			launch();
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

	private void extractDaemon() {
		final Object obj = new Object();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (OS.CURRENT == OS.LINUX
					|| OS.CURRENT == OS.MAC) {
					untarDaemonLinux();
				} else {
					unzipDaemonWindows();
				}
				synchronized (obj) {
					obj.notifyAll();
				}
			}
		});
		t.start();
		synchronized (obj) {
			try {
				System.out.println("we are waiting");
				obj.wait();
				System.out.println("we are done waiting!!!!!!");
			} catch (InterruptedException ex) {
				/* Empty on purpose */
			}
		}
	}

	private void launch() {
		Stage stage = getStage();
		stage.hide();
		launchApp();
		//launch.setDisable(false);
	}

	private boolean daemonDirExists() {
		List<FileMetadata> files = config.getFiles();
		String onlineFileName = "";
		System.out.println("is this working!!!!");
		for (FileMetadata file : files) {
			if (!file.getPath().getFileName().toString().contains("unigrid")) {
				String s = file.getUri().toString();
				String[] arr = s.split("/");
				onlineFileName = arr[arr.length - 1];
			}
		}
		String[] arr = onlineFileName.split("-");
		if (arr.length < 1) {
			return false;
		}
		File dir = new File(startLoacation + "/bin/unigrid-" + arr[1]);
		System.out.println("daemon lib exits = " + dir.exists());
		return dir.exists();
	}

	private void untarDaemonLinux() {
		List<FileMetadata> files = config.getFiles();
		String untarName = "";

		for (FileMetadata file : files) {
			if (file.getPath().toString().contains(".tar.gz")) {
				System.out.println(file.getPath().toString());
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
						if (b == null || !b.exists()) {
							break;
						}
						File[] bintar = b.listFiles();
						if (bintar == null || bintar.length < 1) {
							break;
						}
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
			final ProcessBuilder pb = new ProcessBuilder("tar", "-xf", archive.toString(),
				"-C", destination.toString()
			);

			final Process process = pb.start();

			try ( var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;

				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}
			process.waitFor();

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		try {
			final ProcessBuilder pb = new ProcessBuilder();

			if (OS.CURRENT == OS.MAC) {
				pb.command("find", destination.toString(), "-perm", "+111",
					"-type", "f", "-name", "unigrid*"
				);
			} else {
				pb.command("find", destination.toString(), "-type", "f",
					"-name", "unigrid*"
				);
			}
			final Process process = pb.start();
			try ( var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;

				while ((line = reader.readLine()) != null) {
					System.out.println(line);
					var cp = new ProcessBuilder("cp", line, destination.toString());
					cp.start();
				}
			}
			process.waitFor();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private void unzipDaemonWindows() {
		String[] filesToMove = new String[3];
		List<FileMetadata> files = config.getFiles();
		String untarName = "";

		for (FileMetadata file : files) {
			if (file.getPath().toString().contains(".zip")) {
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

		try ( ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {
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

					final Path moveName = Paths.get(endDir + "/"
						+ new File(zipEntry.getName()).getName()
					);

					System.out.println("moveName: " + moveName.toString());
					Files.copy(newPath, moveName, StandardCopyOption.REPLACE_EXISTING);
				}

				zipEntry = zis.getNextEntry();

			}

			zis.closeEntry();
		}
	}

	public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
		Path targetDirResolved = targetDir.resolve(zipEntry.getName());
		Path normalizePath = targetDirResolved.normalize();

		if (!normalizePath.startsWith(targetDir)) {
			throw new IOException("Bad zip entry: " + zipEntry.getName());
		}

		return normalizePath;
	}

	public void launchApp() {
		config.launch(inject);
	}

	private void run(Runnable runnable) {
		Thread runner = new Thread(runnable);
		runner.setDaemon(true);
		runner.start();
	}

	public static String getBaseDirectory() {
		final String blockRoot = System.getProperty("user.home").concat(
			switch (OS.CURRENT) {
			case LINUX ->
				"/.unigrid/dependencies";
			case WINDOWS ->
				"/AppData/Roaming/UNIGRID/dependencies";
			case MAC ->
				"/Library/Application Support/UNIGRID/dependencies";
			default ->
				"/UNIGRID/dependencies";
		}
		);

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

	public void removeOldJars(Configuration config) {
		File baseDir = new File(getBaseDirectory().concat("/lib"));
		List<FileMetadata> onlineFiles = config.getFiles();
		List<String> fileNames = new ArrayList<String>();
		if (fileNames.size() == 0) {
			return;
		}

		for (FileMetadata onlineFile : onlineFiles) {
			fileNames.add(new File(onlineFile.getPath().getFileName().toString()).getName());
		}
		for (File file : baseDir.listFiles()) {
			if (!fileNames.contains(file.getName())) {
				file.delete();
			}
		}
	}

	private String getFileName(String file) {
		String fileName = "";
		String[] strings = file.split("/");
		fileName = strings[strings.length - 1];
		System.out.println(fileName);
		return fileName;
	}
}
