package org.unigrid.bootstrap;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import io.sentry.Sentry;
import org.update4j.Configuration;
import org.update4j.OS;
import org.update4j.service.Delegate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application implements Delegate {

    private static Scene scene;
    private static FXMLLoader loader;
    private static Map<String, String> inputArgs = new HashMap<>();
    private HostServices hostServices = getHostServices();
    private Stage classStage;
    final static AtomicBoolean keyPressed = new AtomicBoolean();
    public static state startupState;
    private static String selectedChain;

    public enum state {
        NORMAL, DEBUG, WAIT;
    }

    public static void preStart() throws IOException {
        startupState = state.WAIT;
        Stage debugStage = new Stage();
        Scene debugView;

        debugStage.centerOnScreen();
        debugStage.setResizable(false);
        debugStage.initStyle(StageStyle.UNDECORATED);

        try {
            debugView = new Scene(loadFXML("debugView"));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return;
        }
        debugStage.setScene(debugView);
        scene.setOnKeyPressed(key -> {
            if (key.getCode() == KeyCode.F5 || key.getCode() == KeyCode.F12) {
                System.out.println("Key event triggered!!!!");
                startupState = state.DEBUG;
                debugStage.show();
            }
        });
    }

    @Override
    public void start(Stage stage) throws IOException, InterruptedException, ExecutionException {
        showChainSelectionView(stage);
    }

    public void showChainSelectionView(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("chainSelectionView.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Select Chain");
        stage.show();
    }

    public void loadUpdateView(String chain) throws IOException, InterruptedException, ExecutionException {
        selectedChain = chain;
        Stage stage = new Stage();
        scene = new Scene(loadFXML("updateView"));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        classStage = stage;

        preStart();
        // Set the key event handler on the scene
		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.F5 || event.getCode() == KeyCode.F12) {
				System.out.println("F5 or F12 pressed - Opening Debug View");
				startupState = state.DEBUG;
				Platform.runLater(() -> openDebugView());
			}
		});
        postStart(chain);
    }

    public void postStart(String chain) throws IOException, InterruptedException, ExecutionException {
        URL configUrl = null;
        OS os = OS.CURRENT;
        String baseUrl = "https://raw.githubusercontent.com/unigrid-project/unigrid-update-testing/main/";

        if (chain.equals("mainnet")) {
            if (os.equals(OS.LINUX)) {
                configUrl = new URL(baseUrl + "mainnet-linux-test.xml");
            } else if (os.equals(OS.WINDOWS)) {
                configUrl = new URL(baseUrl + "mainnet-windows-test.xml");
            } else if (os.equals(OS.MAC)) {
                configUrl = new URL(baseUrl + "mainnet-mac-test.xml");
            }
        } else if (chain.equals("legacy")) {
            if (os.equals(OS.LINUX)) {
                configUrl = new URL(baseUrl + "config-linux-test.xml");
            } else if (os.equals(OS.WINDOWS)) {
                configUrl = new URL(baseUrl + "config-windows-test.xml");
            } else if (os.equals(OS.MAC)) {
                configUrl = new URL(baseUrl + "config-mac-test.xml");
            }
        }

        System.out.println(configUrl);

        Configuration config = null;

        try (Reader in = new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8)) {
            System.out.println("are we getting here??????");
            config = Configuration.read(in);
            updateLocalConfigFile(config.toString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Sentry.captureException(e);
            try (Reader in = Files.newBufferedReader(Paths.get(localPath() + "/config.xml"))) {
                System.out.println("reading local config xml");
                config = Configuration.read(in);
            }
        }

        if (inputArgs.get("test") == null) {
            String server = "";
			String fxVersionString = "fx." + chain + ".version";
            final String version = config.getProperties(fxVersionString).get(0).getValue();
            Sentry.init(options -> {
                options.setDsn("https://18a30d2bf41643ce9efe84a451ecef1a@o266736.ingest.sentry.io/6632466");
                options.setServerName(cryptCompName());
                options.setTag("os", OS.CURRENT.getShortName());
                options.setRelease(version);
                options.setEnvironment("production");
                options.setTracesSampleRate(0.1);
                options.setDebug(false);
            });
        }

        config.sync();
        UpdateView.getInstance().setConfig(config, classStage, inputArgs, hostServices);
    }

    static String localPath() {
        final String s = System.getProperty("user.home").concat(
            switch (OS.CURRENT) {
                case LINUX -> "/.unigrid/dependencies";
                case WINDOWS -> "/AppData/Roaming/UNIGRID/dependencies";
                case MAC -> "/Library/Application Support/UNIGRID/dependencies";
                default -> "/UNIGRID/dependencies";
            }
        );
        return s;
    }

    static void updateLocalConfigFile(String in) {
		try {
			File targetFile = new File(localPath() + "/config.xml");
			File parentDir = targetFile.getParentFile();
			if (!parentDir.exists()) {
				parentDir.mkdirs(); // This will create the parent directory if it doesn't exist
			}
			if (targetFile.createNewFile()) { // createNewFile() returns true if the file didn't exist and was successfully created
				System.out.println("Config file created");
			} else {
				System.out.println("Config file already exists or couldn't be created");
			}
	
			try (Writer targetFileWriter = new FileWriter(targetFile)) {
				targetFileWriter.write(in);
			} catch (IOException e) {
				System.err.println("Error writing to config file: " + e.getMessage());
			}
		} catch (IOException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
			loader = fxmlLoader;
			return fxmlLoader.load();
		} catch (IOException e) {
			e.printStackTrace(); // print the full stack trace
			throw e;
		}
	}

    public static void main(String[] args) {
        if (args != null) {
            for (String arg : args) {
                System.out.println(arg);
                if (arg.contains("=")) {
                    String key = arg.split("=")[0];
                    String value = arg.split("=")[1];
                    inputArgs.put(key, value);
                }
            }
        }
        launch();
    }

	@Override
    public void main(List<String> list) throws Throwable {
        launch();
    }

    private String cryptCompName() {
        String s = "";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                System.out.println(ni.getName());
                if (ni != null) {
                    byte[] name = ni.getHardwareAddress();
                    byte[] salt = "31".getBytes();
                    byte[] result = joinBytes(name, salt);
                    UUID uuid = UUID.nameUUIDFromBytes(result);
                    s = uuid.toString();
                    break;
                }
            }
            System.out.println(s);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return s;
    }

    private byte[] joinBytes(byte[] byteArray1, byte[] byteArray2) {
        final int finalLength = byteArray1.length + byteArray2.length;
        final byte[] result = new byte[finalLength];

        System.arraycopy(byteArray1, 0, result, 0, byteArray1.length);
        System.arraycopy(byteArray2, 0, result, byteArray1.length, byteArray2.length);
        return result;
    }

    private void openDebugView() {
		try {
			Stage debugStage = new Stage();
			Scene debugView;
			debugStage.centerOnScreen();
			debugStage.setResizable(false);
			debugStage.initStyle(StageStyle.UNDECORATED);

			debugView = new Scene(loadFXML("debugView"));
			
			debugStage.setScene(debugView);

			debugStage.show(); // Show the debug stage and wait
		} catch (IOException e) {
			System.err.println("Error loading debug view: " + e.getMessage());
		}
	}
}
