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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import org.update4j.OS;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
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
import java.util.StringJoiner;
import org.apache.maven.project.MavenProject;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "update")
public class UpdateWalletConfig extends AbstractMavenLifecycleParticipant {

	private final Configuration configuration = new Configuration();

	private File basedir = null;

	private String fxVersion = "";

	@Requirement
	private Logger logger;

	@Override
	public void afterProjectsRead(MavenSession mavenSession
	) {
		basedir = mavenSession.getRepositorySession().getLocalRepository().getBasedir();
		MavenProject fxProject = null;
		if (mavenSession.getGoals().contains("install")) {
			logger.info("Goal: " + mavenSession.getGoals());
			for (MavenProject msp : mavenSession.getProjects()) {
				if (msp.getArtifactId().equals("fx")) {
					fxProject = msp;
				}
			}

			if (fxProject != null && fxProject.getDependencies().size() != 0) {
				logger.info("Fx Project: " + fxProject.getGroupId() + ":" + fxProject.getArtifactId()
					+ ":" + fxProject.getVersion());
				if (fxVersion.isEmpty()) {
					fxVersion = fxProject.getVersion().replace("-SNAPSHOT", "");
					configuration.setFxVersionToProperty(fxVersion);
				}
				OS[] os = new OS[]{OS.LINUX, OS.LINUX, OS.MAC, OS.MAC, OS.WINDOWS, OS.WINDOWS};
				for (int i = 0; i < os.length; i++) {
					generateUpdateConfigFile(fxProject, os[i], i % 2 == 0);
				}
			} else {
				logger.info("Fx Project not found or no local dependencies found! Need to build once");
			}
		}
	}

	public void generateUpdateConfigFile(MavenProject fxProject, OS os, boolean isForTesting) {
		String version = fxProject.getVersion();
		List<FileMetadata> files = getDependencies(getFxDependencyString(fxProject));
		List<FileMetadata> bootstrapFiles = getDependencies("org.unigrid:bootstrap:" + version);
		List<FileMetadata> external = getExternalDependencies(fxProject.getBasedir(), isForTesting);
		files.removeAll(bootstrapFiles);
		files.addAll(0, external);
		configuration.setBasePath(new BasePath(getBasePathUrl(os)));
		configuration.setFiles(files);

		ConfMarshaller marshaller = new ConfMarshaller();
		marshaller.mashal(configuration, getFileUrl(os, isForTesting));
		logger.info("Config File created: " + getFileUrl(os, isForTesting));
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
			RepositorySystem system = newRepositorySystem();
			CollectResult collectResult = system.collectDependencies(defSession, collectRequest);
			// Console Dependency Tree Dump
			// collectResult.getRoot().accept(new ConsoleDependencyGraphDumper());
			files = getListByRecursion(collectResult.getRoot().getChildren(), files);

		} catch (DependencyCollectionException | NoLocalRepositoryManagerException e) {
			java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
				.log(Level.SEVERE, null, e);
		}
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

	public List<FileMetadata> getExternalDependencies(File baseDir, boolean isForTesting) {
		String testing = isForTesting ? "-testing" : "";
		List<FileMetadata> list = new ArrayList();
		try {
			String updateUrl = "https://github.com/unigrid-project/unigrid-update"
				+ testing + "/releases/download/v" + fxVersion + "/fx-" + fxVersion + "-SNAPSHOT.jar";
			File localJar = new File(baseDir.getAbsolutePath() + "/target/fx-" + fxVersion
				+ "-SNAPSHOT.jar");
			if (localJar.exists() != false) {
				list.add(new FileMetadata(updateUrl,
					localJar.length(),
					ConfFileUtil.getChecksumString(localJar.toPath())));
			} else {
				logger.info("local jar not found! Parent or fx need to build once to create jar file");
			}
			list.add(getFileByUrl(getDaemonUrl(OS.CURRENT)));
			// list.add(getFileByUrl(getHedgehogUrl(OS.CURRENT)));
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
				.log(Level.SEVERE, null, ex);
		}
		return list;
	}

	public FileMetadata getFileMetadata(String gId, String aId, String version, String classifier) {
		String localUrl = getLocalUrl(gId, aId, version, classifier);
		File file = new File(localUrl);
		FileMetadata tempFile = null;
		if (file.exists()) {
			try {
				String checksum = ConfFileUtil.getChecksumString(file.toPath());
				tempFile = new FileMetadata(
					file.getAbsolutePath().replace(basedir.getPath(), "${maven.central}"),
					file.length(),
					checksum
				);
			} catch (IOException ex) {
				java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
					.log(Level.SEVERE, null, ex);
			}
		} else {
			logger.info("    !!! Url to file doesn't exist: " + localUrl);
		}
		return tempFile;
	}

	public String getFxDependencyString(MavenProject fxProject) {
		return new StringJoiner(":").add(fxProject.getGroupId()).add(fxProject.getArtifactId())
			.add(fxProject.getVersion()).toString();
	}

	public FileMetadata getFileByUrl(String url) {
		try {
			URL tempUrl = new URL(url);
			return new FileMetadata(
				tempUrl.toString(),
				ConfFileUtil.getFileSize(tempUrl),
				ConfFileUtil.getChecksumStringyByInputStream(tempUrl.openStream())
			);
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

	public static String getFileUrl(OS os, boolean isForTesting) {
		String testing = isForTesting ? "-test" : "";
		return System.getProperty("user.home") + "/Downloads/config-" + os.getShortName() + testing + ".xml";
	}

	public static String getDaemonUrl(OS os) {
		String s = "";

		String jsonSearch = "$['assets'][*]['browser_download_url']";
		DocumentContext jsonPath = null;
		try {
			jsonPath = JsonPath
				.parse(new URL("https://api.github.com/repos/unigrid-project/daemon/releases/latest"));
			List<String> githubUrls = jsonPath.read(jsonSearch);

			if (os.equals(OS.LINUX)) {
				/*List<Map<String, Object>> data = jsonPath
				.read("$['assets'][*][?('linux' in @['browser_download_url'])]");
			s = data.get(0).toString();
			String arg = "linux-gnu.tar.gz";
			for (int i = 0; i < githubUrls.size(); i++) {
				if(Pattern.matches(githubUrls.get(i), arg)){
					s = githubUrls.get(i);
				}
			}*/
				s = githubUrls.get(2);
				System.out.println(s);
			} else if (os.equals(OS.MAC)) {
				s = githubUrls.get(0);
				System.out.println(s);
			} else if (os.equals(OS.WINDOWS)) {
				s = githubUrls.get(1);
				System.out.println(s);
			}
			return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String getHedgehogUrl(OS os) {
		String s = "";

		String jsonSearch = "$['assets'][*]['browser_download_url']";
		DocumentContext jsonPath = null;
		try {
			jsonPath = JsonPath
				.parse(new URL("https://api.github.com/repos/unigrid-project/hedgehog/releases/latest"));
			List<String> githubUrls = jsonPath.read(jsonSearch);

			if (os.equals(OS.LINUX)) {
				s = githubUrls.get(2);
				System.out.println(s);
			} else if (os.equals(OS.MAC)) {
				s = githubUrls.get(0);
				System.out.println(s);
			} else if (os.equals(OS.WINDOWS)) {
				s = githubUrls.get(1);
				System.out.println(s);
			}
			return s;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
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
