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
package org.unigrid.updatewalletconfig;

import org.unigrid.updatewalletconfig.xml.Feed;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import org.update4j.OS;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import java.io.File;
import java.util.ArrayList;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.graph.DependencyNode;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.project.MavenProject;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "update")
public class UpdateWalletConfig extends AbstractMavenLifecycleParticipant {

	private final Configuration configuration = new Configuration();

	private File basedir = null;

	private String fxVersion = "";

	private String fxVersionWithSnapshot = "";

	@Override
	public void afterSessionEnd(MavenSession mavenSession) throws MavenExecutionException {
		if (!mavenSession.getResult().getExceptions().isEmpty()) {
			return;
		}

		if (!mavenSession.getGoals().contains("install") && !mavenSession.getGoals().contains("package")
			&& !mavenSession.getGoals().contains("validate")) {
			return;
		}

		basedir = mavenSession.getRepositorySession().getLocalRepository().getBasedir();
		MavenProject fxProject = null;
		MavenProject bootstrapProject = null;

		System.out.println("Goal: " + mavenSession.getGoals());

		for (MavenProject mp : mavenSession.getProjects()) {
			if (mp.getArtifactId().equals("fx")) {
				fxProject = mp;
			}
			if (mp.getArtifactId().equals("bootstrap")) {
				bootstrapProject = mp;
			}
		}

		if (fxProject != null && fxProject.getDependencies().size() != 0) {
			System.out.println("Fx Project: " + fxProject.getGroupId() + ":" + fxProject.getArtifactId()
				+ ":" + fxProject.getVersion());

			fxVersionWithSnapshot = fxProject.getVersion();

			if (fxVersion.isEmpty()) {
				fxVersion = fxProject.getVersion().replace("-SNAPSHOT", "");
				configuration.getProperties().add(new Property("fx.version", fxVersion));
				String bootstrapVersion = bootstrapProject.getVersion().replace("-SNAPSHOT", "");
				configuration.getProperties().add(new Property("bootstrapVersion", bootstrapVersion));
			}

			OS[] os = new OS[]{OS.LINUX, OS.LINUX, OS.MAC, OS.MAC, OS.WINDOWS, OS.WINDOWS};

			for (int i = 0; i < os.length; i++) {
				generateUpdateConfigFile(fxProject, bootstrapProject, os[i], i % 2 == 0);
			}
		} else {
			throw new MavenExecutionException("Fx Project not found or no local dependencies found!"
				+ " Try mvn clean install or mvn clean package", new IllegalStateException());
		}
	}

	public void generateUpdateConfigFile(MavenProject fx, MavenProject bootstrap, OS os, boolean testing) throws MavenExecutionException {
		String version = fx.getVersion();
		String bootstrapVersion = bootstrap.getVersion();
		List<FileMetadata> files = getDependencies(getFxDependencyString(fx));
		List<FileMetadata> bootstrapFiles = getDependencies("org.unigrid:bootstrap:" + bootstrapVersion);
		List<FileMetadata> externalFiles = getExternalDependencies(os, fx.getBasedir(), testing);
		files.removeAll(bootstrapFiles);
		files.addAll(0, externalFiles);

		final String[] opens = fx.getProperties().getProperty("config.opens").split("\n");
		final String[] exports = fx.getProperties().getProperty("config.exports").split("\n");

		for (FileMetadata file : files) {
			List<Package> opensPackages = getOpensExports(file, opens);
			if (opensPackages != null && !opensPackages.isEmpty()) {
				file.setOpensPackages(getOpensExports(file, opens));
			}
			List<Package> exportsPackages = getOpensExports(file, exports);
			if (exportsPackages != null && !exportsPackages.isEmpty()) {
				file.setExportsPackages(getOpensExports(file, exports));
			}
		}

		configuration.setBasePath(new BasePath(getBasePathUrl(os)));
		configuration.setFiles(files);

		ConfMarshaller marshaller = new ConfMarshaller();
		marshaller.mashal(configuration, getFileUrl(os, testing));

		System.out.println("Config File created: " + getFileUrl(os, testing));
	}

	public List<Package> getOpensExports(FileMetadata file, String[] opensExportsList) {
		List<Package> packages = null;

		for (String element : opensExportsList) {
			element.trim();
			if (!element.isEmpty()) {
				String[] elementSplitStrings = element.split("=");
				String target = elementSplitStrings[0];
				String dependency[] = elementSplitStrings[1].split("/");
				String groupId = dependency[0];
				String artifactId = dependency[1];

				if (file.getGroupId().equals(groupId) && file.getArtifactId().equals(artifactId)) {
					packages = new ArrayList<Package>();
					packages.add(new Package(groupId, target));
				}
			}
		}

		return packages;
	}

	public RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

		locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
			@Override
			public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
				System.err.println(String.format("Service creation failed for {} with impl {}",
					type, impl, exception));
			}
		});

		return locator.getService(RepositorySystem.class);
	}

	public List<FileMetadata> getDependencies(String currentArtifact) {
		List<FileMetadata> files = new ArrayList();
		DefaultRepositorySystemSession defSession = MavenRepositorySystemUtils.newSession();
		LocalRepository localRepo = new LocalRepository(basedir);

		try {
			defSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
				.newInstance(defSession, localRepo));
			Artifact artifact = new DefaultArtifact(currentArtifact);
			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRoot(new Dependency(artifact, ""));
			CollectResult collectResult = newRepositorySystem().collectDependencies(defSession, collectRequest);
			// Console Dependency Tree Dump
			// collectResult.getRoot().accept(new ConsoleDependencyGraphDumper());

			files = getListByRecursion(collectResult.getRoot().getChildren(), files);
		} catch (DependencyCollectionException | NoLocalRepositoryManagerException e) {
			java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
				.log(Level.SEVERE, null, e);
		}

		System.out.println(currentArtifact + " dependencies: " + files.size());

		return files;
	}

	public List<FileMetadata> getListByRecursion(
		List<DependencyNode> nodes,
		List<FileMetadata> files
	) {
		if (nodes != null && nodes.isEmpty() == false) {
			nodes.forEach((node) -> {
				files.add(getFileMetadata(
					node.getArtifact().getGroupId(),
					node.getArtifact().getArtifactId(),
					node.getArtifact().getVersion(),
					node.getArtifact().getClassifier()
				));

				getListByRecursion(node.getChildren(), files);
			});
		}

		return files;
	}

	public List<FileMetadata> getExternalDependencies(OS os, File baseDir, boolean testing)
		throws MavenExecutionException {
		String isTesting = testing ? "-testing" : "";
		List<FileMetadata> list = new ArrayList();

		try {
			String version = "";
			for (Property property : configuration.getProperties()) {
				if (property.getKey().equals("fx.version")) {
					version = "${fx.version}";
				} else {
					version = fxVersion;
				}
			}
			String updateUrl = "https://github.com/unigrid-project/unigrid-update"
				+ isTesting + "/releases/download/v" + fxVersion + "/fx-" + fxVersionWithSnapshot + ".jar";
			File localJar = new File(baseDir.getAbsolutePath() + "/target/fx-" + fxVersionWithSnapshot + ".jar");

			if (localJar.exists() != false) {
				FileMetadata tempFile = new FileMetadata(updateUrl, localJar.length(),
					ConfFileUtil.getChecksumString(localJar.toPath()));
				list.add(tempFile);
			} else {
				throw new MavenExecutionException("Local jar not found!"
					+ " Try mvn clean install or mvn clean package", new IllegalStateException());
			}

			list.add(getFileByUrl(getDaemonUrl(os, testing)));
			list.add(getFileByUrl(getHedgehogUrl(os, testing)));

		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
				.log(Level.SEVERE, null, ex);
		}

		return list;
	}

	public FileMetadata getFileMetadata(String groupId, String artifactId, String version, String classifier) {
		String localUrl = getLocalUrl(groupId, artifactId, version, classifier);
		File file = new File(localUrl);
		FileMetadata tempFile = null;

		if (file.exists()) {
			try {
				String filePath = file.getAbsolutePath().replace(basedir.getPath(), "${maven.central}");
				String checksum = ConfFileUtil.getChecksumString(file.toPath());
				tempFile = new FileMetadata(
					filePath,
					file.length(),
					checksum,
					groupId,
					artifactId
				);
				if (filePath.contains("${maven.central}/jakarta/inject/jakarta.inject-api")) {
					tempFile.setIgnoreBootConflict(true);
				}
				if (filePath.contains("${maven.central}/org/openjfx/javafx-swing")) {
					tempFile.setIgnoreBootConflict(true);
				}
			} catch (IOException ex) {
				java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
					.log(Level.SEVERE, null, ex);
			}
		} else {
			System.out.println("    !!! Url to file doesn't exist: " + localUrl);
		}

		return tempFile;
	}

	public String getFxDependencyString(MavenProject fxProject) {
		return String.join(":", fxProject.getGroupId(), fxProject.getArtifactId(), fxProject.getVersion());
	}

	public FileMetadata getFileByUrl(String url) {
		try {
			URL tempUrl = new URL(url);

			FileMetadata tempFile = new FileMetadata(
				tempUrl.toString(),
				ConfFileUtil.getFileSize(tempUrl),
				ConfFileUtil.getChecksumStringyByInputStream(tempUrl.openStream())
			);

			tempFile.setModulePath(false);

			return tempFile;
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	public static String getBasePathUrl(OS os) {
		return switch (os) {
			case LINUX -> {
				yield "${user.home}/.unigrid/dependencies/lib/";
			}
			case WINDOWS -> {
				yield "${user.home}/AppData/Roaming/UNIGRID/dependencies/lib/";
			}
			case MAC -> {
				yield "${user.home}/Library/Application Support/UNIGRID/dependencies/lib/";
			}
			default -> {
				yield "${user.home}/.unigrid/dependencies/lib/";
			}
		};
	}

	public static String getFileUrl(OS os, boolean testing) {
		String isTesting = testing ? "-test" : "";
		String osName = os.equals(os.WINDOWS) ? os.name().toLowerCase() : os.getShortName();

		return System.getProperty("user.dir").concat("/config/target/") + "config-"
			+ osName + isTesting + ".xml";
	}

	public static String getDaemonUrl(OS os, boolean testing) {
		//Feed result = new Feed();
		if (testing) {
			String url = "https://github.com/unigrid-project/daemonTesting/releases.atom";
			Client client = ClientBuilder.newBuilder().build();
			Response response = client.target(url).request(MediaType.APPLICATION_XML_TYPE).get();
			Feed result = response.readEntity(Feed.class);
			return getZipUrl(os, result.getEntry().get(0).getLink().getHref(), testing);
		} else {
			String url = "https://github.com/unigrid-project/daemon/releases.atom";
			Client client = ClientBuilder.newBuilder().build();
			Response response = client.target(url).request(MediaType.APPLICATION_XML_TYPE).get();
			Feed result = response.readEntity(Feed.class);
			return getZipUrl(os, result.getEntry().get(0).getLink().getHref(), testing);
		}

	}

	public static String getHedgehogUrl(OS os, boolean testing) {
		Feed result = new Feed();
		if (testing) {
			String url = "https://github.com/unigrid-project/hedgehogTesting/releases.atom";
			Client client = ClientBuilder.newBuilder().build();
			Response response = client.target(url).request(MediaType.APPLICATION_XML_TYPE).get();
			result = response.readEntity(Feed.class);
			if (result.getEntry().get(0).getLink().getHref() == "") {
				return "";
			}
		} else {
			String url = "https://github.com/unigrid-project/hedgehog/releases.atom";
			Client client = ClientBuilder.newBuilder().build();
			Response response = client.target(url).request(MediaType.APPLICATION_XML_TYPE).get();
			result = response.readEntity(Feed.class);
		}

		return getHedgehogGitUrl(os, result.getEntry().get(0).getLink().getHref(), testing);
	}

	public static String getHedgehogGitUrl(OS os, String hedgehogUrl, boolean testing) {
		if (hedgehogUrl.equals("")) {
			return "";
		}
		final String affix = "/hedgehog-";
		String[] split = hedgehogUrl.split("/", 0);
		final String version = split[split.length - 1].replace("v", "");
		if (testing) {
			hedgehogUrl = hedgehogUrl.replace("https://github.com/unigrid-project/hedgehogTesting/releases/tag/",
				"https://github.com/unigrid-project/hedgehogTesting/releases/download/");
		} else {
			hedgehogUrl = hedgehogUrl.replace("https://github.com/unigrid-project/hedgehog/releases/tag/",
				"https://github.com/unigrid-project/hedgehog/releases/download/");
		}

		if (os.equals(OS.LINUX)) {
			return hedgehogUrl + affix + version + "-x86_64-linux-gnu.bin";
		} else if (os.equals(OS.MAC)) {
			return hedgehogUrl + affix + version + "-osx64.bin";
		} else if (os.equals(OS.WINDOWS)) {
			return hedgehogUrl + affix + version + "-win64.exe";
		}

		return hedgehogUrl;
	}

	public static String getZipUrl(OS os, String daemonUrl, boolean testing) {
		final String affix = "/unigrid-";
		String[] split = daemonUrl.split("/", 0);
		final String version = split[split.length - 1].replace("v", "");
		if (testing) {
			daemonUrl = daemonUrl.replace("https://github.com/unigrid-project/daemonTesting/releases/tag/",
				"https://github.com/unigrid-project/daemonTesting/releases/download/");
		} else {
			daemonUrl = daemonUrl.replace("https://github.com/unigrid-project/daemon/releases/tag/",
				"https://github.com/unigrid-project/daemon/releases/download/");
		}

		if (os.equals(OS.LINUX)) {
			return daemonUrl + affix + version + "-x86_64-linux-gnu.tar.gz";
		} else if (os.equals(OS.MAC)) {
			return daemonUrl + affix + version + "-osx64.tar.gz";
		} else if (os.equals(OS.WINDOWS)) {
			return daemonUrl + affix + version + "-win64.zip";
		}

		return daemonUrl + affix + version + "-x86_64-linux-gnu.tar.gz";
	}

	public static String getLocalUrl(String groupId, String artifactId, String version, String classifier) {
		StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("user.home").concat("/.m2/repository/"));
		builder.append(groupId.replace('.', '/')).append("/");
		builder.append(artifactId).append("/");
		builder.append(version).append('/');
		builder.append(artifactId).append("-").append(version);

		if (classifier.isEmpty() != true) {
			builder.append("-").append(classifier);
		}

		builder.append(".jar");

		return builder.toString();
	}
}
