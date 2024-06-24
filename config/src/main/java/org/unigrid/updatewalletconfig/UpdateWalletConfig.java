/*
    The Janus Wallet
    Copyright © 2021-2024 The Unigrid Foundation, UGD Software AB

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
 import java.util.HashSet;
 import java.util.Set;
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
     private String fxLegacyVersion = "";
     private String fxMainnetVersion = "";
     private String fxLegacyVersionWithSnapshot = "";
     private String fxMainnetVersionWithSnapshot = "";
     private List<FileMetadata> allFiles = new ArrayList<>();
     private List<FileMetadata> testingFiles = new ArrayList<>();
 
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
         MavenProject fxLegacyProject = null;
         MavenProject fxMainnetProject = null;
         MavenProject bootstrapProject = null;
 
         System.out.println("Goal: " + mavenSession.getGoals());
 
         for (MavenProject mp : mavenSession.getProjects()) {
             System.out.println("Project: " + mp.getArtifactId());
             if (mp.getArtifactId().equals("fx-legacy")) {
                 fxLegacyProject = mp;
                 System.out.println("Found fx-legacy project");
             } else if (mp.getArtifactId().equals("fx-mainnet")) {
                 fxMainnetProject = mp;
                 System.out.println("Found fx-mainnet project");
             } else if (mp.getArtifactId().equals("bootstrap")) {
                 bootstrapProject = mp;
                 System.out.println("Found bootstrap project");
             }
         }
 
         if (fxLegacyProject != null) {
             processProject(fxLegacyProject, bootstrapProject, "legacy");
         }
 
         if (fxMainnetProject != null) {
             processProject(fxMainnetProject, bootstrapProject, "mainnet");
         }
 
         OS[] os = new OS[]{OS.LINUX, OS.LINUX, OS.MAC, OS.MAC, OS.WINDOWS, OS.WINDOWS};
         for (int i = 0; i < os.length; i++) {
             boolean isTesting = i % 2 != 0;
             generateUpdateConfigFile(fxMainnetProject, bootstrapProject, os[i], isTesting, "mainnet");
             generateUpdateConfigFile(fxLegacyProject, bootstrapProject, os[i], isTesting, "legacy");
         }
     }
 
     private void processProject(MavenProject fxProject, MavenProject bootstrapProject, String chain) throws MavenExecutionException {
         System.out.println("Processing Fx Project for chain: " + chain);
 
         if (fxProject.getDependencies().size() != 0) {
             System.out.println("Fx Project: " + fxProject.getGroupId() + ":" + fxProject.getArtifactId()
                     + ":" + fxProject.getVersion());
 
             if (chain.equals("legacy")) {
                 fxLegacyVersionWithSnapshot = fxProject.getVersion();
                 fxLegacyVersion = fxProject.getVersion().replace("-SNAPSHOT", "");
                 configuration.getProperties().add(new Property("fx.legacy.version", fxLegacyVersion));
             } else if (chain.equals("mainnet")) {
                 fxMainnetVersionWithSnapshot = fxProject.getVersion();
                 fxMainnetVersion = fxProject.getVersion().replace("-SNAPSHOT", "");
                 configuration.getProperties().add(new Property("fx.mainnet.version", fxMainnetVersion));
             }
 
             String bootstrapVersion = bootstrapProject.getVersion().replace("-SNAPSHOT", "");
             configuration.getProperties().add(new Property("bootstrapVersion", bootstrapVersion));
 
             List<FileMetadata> projectFiles = getDependencies(getFxDependencyString(fxProject));
             List<FileMetadata> bootstrapFiles = getDependencies("org.unigrid:bootstrap:" + bootstrapVersion);
             List<FileMetadata> externalFiles = getExternalDependencies(fxProject.getBasedir(), chain, false);
             List<FileMetadata> externalFilesTesting = getExternalDependencies(fxProject.getBasedir(), chain, true);
 
             // Filter out bootstrap files
             projectFiles.removeAll(bootstrapFiles);
 
             projectFiles.addAll(0, externalFiles);
             allFiles.addAll(projectFiles);
             testingFiles.addAll(externalFilesTesting);
         } else {
             throw new MavenExecutionException("Fx Project not found or no local dependencies found!"
                     + " Try mvn clean install or mvn clean package", new IllegalStateException());
         }
     }
 
     public void generateUpdateConfigFile(MavenProject fx, MavenProject bootstrap, OS os, boolean testing, String chain) throws MavenExecutionException {
         final String[] opens = fx.getProperties().getProperty("config.opens").split("\n");
         final String[] exports = fx.getProperties().getProperty("config.exports").split("\n");
 
         Set<String> addedUris = new HashSet<>();
         List<FileMetadata> filesToUse = new ArrayList<>();
 
         List<FileMetadata> selectedFiles = testing ? testingFiles : allFiles;
 
         // Filter out bootstrap files
         List<FileMetadata> bootstrapFiles = getDependencies("org.unigrid:bootstrap:" + bootstrap.getVersion().replace("-SNAPSHOT", ""));
 
         for (FileMetadata file : selectedFiles) {
             if (isValidForChain(file, chain) && addedUris.add(file.getUri()) && !bootstrapFiles.contains(file)) {
                 filesToUse.add(file);
                 List<FileMetadata> dependencies = getDependencies(getFxDependencyString(fx));
                 for (FileMetadata dependency : dependencies) {
                     if (addedUris.add(dependency.getUri()) && !bootstrapFiles.contains(dependency)) {
                         filesToUse.add(dependency);
                     }
                 }
             }
         }
 
         if (testing) {
             addTestingDaemonAndHedgehogFiles(filesToUse, addedUris, os, chain);
         } else {
             addProductionDaemonAndHedgehogFiles(filesToUse, addedUris, os, chain);
         }
 
         for (FileMetadata file : filesToUse) {
             List<Package> opensPackages = getOpensExports(file, opens);
             if (opensPackages != null && !opensPackages.isEmpty()) {
                 file.setOpensPackages(opensPackages);
             }
             List<Package> exportsPackages = getOpensExports(file, exports);
             if (exportsPackages != null && !exportsPackages.isEmpty()) {
                 file.setExportsPackages(exportsPackages);
             }
         }
 
         configuration.setBasePath(new BasePath(getBasePathUrl(os)));
         configuration.setFiles(filesToUse);
 
         List<Property> properties = new ArrayList<>();
         properties.add(new Property("maven.central", "https://repo1.maven.org/maven2"));
         properties.add(new Property("default.launcher.main.class", "org.unigrid.janus.Janus"));
         if (chain.equals("legacy")) {
             properties.add(new Property("fx.legacy.version", fxLegacyVersion));
         } else if (chain.equals("mainnet")) {
             properties.add(new Property("fx.mainnet.version", fxMainnetVersion));
         }
         properties.add(new Property("bootstrapVersion", bootstrap.getVersion().replace("-SNAPSHOT", "")));
         configuration.setProperties(properties);
 
         ConfMarshaller marshaller = new ConfMarshaller();
         marshaller.mashal(configuration, getFileUrl(os, testing, chain));
 
         System.out.println("Config File created: " + getFileUrl(os, testing, chain));
     }
 
     private boolean isValidForChain(FileMetadata file, String chain) {
         String uri = file.getUri();
         if (chain.equals("mainnet")) {
             return uri.contains("fx-mainnet");
         } else if (chain.equals("legacy")) {
             return uri.contains("fx-legacy");
         }
         return false;
     }
 
     private void addTestingDaemonAndHedgehogFiles(List<FileMetadata> filesToUse, Set<String> addedUris, OS os, String chain) {
         String daemonUrl = getDaemonUrl(os, true);
         String hedgehogUrl = getHedgehogUrl(os, true);
 
         if (daemonUrl != null && !daemonUrl.isEmpty() && addedUris.add(daemonUrl) && "legacy".equals(chain)) {
             filesToUse.add(getFileByUrl(daemonUrl));
         }
         if (hedgehogUrl != null && !hedgehogUrl.isEmpty() && addedUris.add(hedgehogUrl)) {
             filesToUse.add(getFileByUrl(hedgehogUrl));
         }
     }
 
     private void addProductionDaemonAndHedgehogFiles(List<FileMetadata> filesToUse, Set<String> addedUris, OS os, String chain) {
         String daemonUrl = getDaemonUrl(os, false);
         String hedgehogUrl = getHedgehogUrl(os, false);
 
         if (daemonUrl != null && !daemonUrl.isEmpty() && addedUris.add(daemonUrl) && "legacy".equals(chain)) {
             filesToUse.add(getFileByUrl(daemonUrl));
         }
         if (hedgehogUrl != null && !hedgehogUrl.isEmpty() && addedUris.add(hedgehogUrl)) {
             filesToUse.add(getFileByUrl(hedgehogUrl));
         }
     }
 
     public List<Package> getOpensExports(FileMetadata file, String[] opensExportsList) {
         List<Package> packages = null;
 
         for (String element : opensExportsList) {
             element = element.trim();
             if (!element.isEmpty()) {
                 String[] elementSplitStrings = element.split("=");
                 String target = elementSplitStrings[0].trim();
                 String dependency[] = elementSplitStrings[1].split("/");
                 String groupId = dependency[0];
                 String arr[] = dependency[1].split("@");
                 String artifactId = arr[0];
                 String moduelPackage = arr[1];
 
                 if (file.getGroupId().equals(groupId) && file.getArtifactId().equals(artifactId)) {
                     if (packages == null) {
                         packages = new ArrayList<>();
                     }
                     packages.add(new Package(groupId, target, moduelPackage));
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
         List<FileMetadata> files = new ArrayList<>();
         DefaultRepositorySystemSession defSession = MavenRepositorySystemUtils.newSession();
         LocalRepository localRepo = new LocalRepository(basedir);
 
         try {
             defSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                     .newInstance(defSession, localRepo));
             Artifact artifact = new DefaultArtifact(currentArtifact);
             CollectRequest collectRequest = new CollectRequest();
             collectRequest.setRoot(new Dependency(artifact, ""));
             CollectResult collectResult = newRepositorySystem().collectDependencies(defSession, collectRequest);
             files = getListByRecursion(collectResult.getRoot().getChildren(), files);
         } catch (DependencyCollectionException | NoLocalRepositoryManagerException ex) {
             java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName())
                     .log(Level.SEVERE, null, ex);
         }
 
         return files;
     }
 
     public List<FileMetadata> getListByRecursion(List<DependencyNode> dependencyNode, List<FileMetadata> files) {
         for (DependencyNode node : dependencyNode) {
             if (node.getArtifact().getExtension().equals("jar") && !node.getDependency().isOptional()) {
                 files.add(getFileMetadata(
                         node.getArtifact().getGroupId(),
                         node.getArtifact().getArtifactId(),
                         node.getArtifact().getVersion(),
                         node.getArtifact().getClassifier()
                 ));
 
                 getListByRecursion(node.getChildren(), files);
             }
         }
 
         return files;
     }
 
     public List<FileMetadata> getExternalDependencies(File baseDir, String chain, boolean isTesting) throws MavenExecutionException {
         List<FileMetadata> list = new ArrayList<>();
 
         try {
             String version = chain.equals("legacy") ? fxLegacyVersion : fxMainnetVersion;
             String versionWithSnapshot = chain.equals("legacy") ? fxLegacyVersionWithSnapshot : fxMainnetVersionWithSnapshot;
             String baseUrl = isTesting ? "https://github.com/unigrid-project/unigrid-update-testing/releases/download/v" : "https://github.com/unigrid-project/unigrid-update/releases/download/v";
             String updateUrl = baseUrl + version + "/fx-" + chain + "-" + versionWithSnapshot + ".jar";
             File localJar = new File(baseDir.getAbsolutePath() + "/target/fx-" + chain + "-" + versionWithSnapshot + ".jar");
 
             if (localJar.exists()) {
                 FileMetadata tempFile = new FileMetadata(updateUrl, localJar.length(), ConfFileUtil.getChecksumString(localJar.toPath()));
                 tempFile.setChain(chain);
                 list.add(tempFile);
             } else {
                 throw new MavenExecutionException("Local jar not found! Try mvn clean install or mvn clean package", new IllegalStateException());
             }
 
             if (chain.equals("legacy")) {
                 list.add(getFileByUrl(getDaemonUrl(OS.CURRENT, isTesting)));
             }
             list.add(getFileByUrl(getHedgehogUrl(OS.CURRENT, isTesting)));
 
         } catch (IOException ex) {
             java.util.logging.Logger.getLogger(UpdateWalletConfig.class.getName()).log(Level.SEVERE, null, ex);
         }
 
         System.out.println("External dependencies for " + (isTesting ? "testing" : "production") + ": " + list);
 
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
                 if (filePath.contains("${maven.central}/org.openjfx/javafx-swing")) {
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
 
     public static String getFileUrl(OS os, boolean testing, String chain) {
         String isTesting = testing ? "-test" : "";
         String osName = os.equals(os.WINDOWS) ? os.name().toLowerCase() : os.getShortName();
         String chainName = chain.equals("legacy") ? "config-" : "mainnet-";
 
         return System.getProperty("user.dir").concat("/config/target/") + chainName + osName + isTesting + ".xml";
     }
 
     public static String getDaemonUrl(OS os, boolean testing) {
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
 
         if (!classifier.isEmpty()) {
             builder.append("-").append(classifier);
         }
 
         builder.append(".jar");
 
         return builder.toString();
     }
 }
 