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
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.OS;

public class UpdateWalletConfig {

	private static final String MAVEN_BASE = "https://repo1.maven.org/maven2";

	public static void main(String[] args) throws IOException {

		String configLocation = System.getProperty("user.dir");
		String homeDir = System.getProperty("user.home");
		String dir = configLocation;
		String fxJarUrl = "https://github.com/unigrid-project/unigrid-update/releases/download/v1.0.8/fx-1.0.8-SNAPSHOT.jar";
		String linuxDaemon = "unigrid-2.9.2-x86_64-linux-gnu.tar.gz";
		String windowsDaemon = "unigrid-2.9.2-win64.zip";
		String osxDaemon = "unigrid-2.9.2-osx64.tar.gz";
		String fxVersion = "1.0.6";
		String[] filePath = new String[]{
			"/config-linux.xml",
			"/config-linux-test.xml",
			"/config-windows.xml",
			"/config-windows-test.xml",
			"/config-mac.xml",
			"/config-mac-test.xml"
		};
		String daemonPath = "";
		String url = "";
		String basePath = "";

		for (int i = 0; i < filePath.length; i++) {
			if ((i%2) == 0) {
				fxJarUrl = "https://github.com/unigrid-project/unigrid-update/releases/download/v1.0.8/fx-1.0.8-SNAPSHOT.jar";
			} else {
				fxJarUrl = "https://github.com/unigrid-project/unigrid-update-testing/releases/download/v1.0.8/fx-1.0.8-SNAPSHOT.jar";
			}

			if (filePath[i].contains("linux")) {
				daemonPath = homeDir + "/Downloads/" + linuxDaemon;
				url = getDaemonUrl(OS.LINUX);
				basePath = "${user.home}/.unigrid/dependencies/lib/";
			} else if (filePath[i].contains("mac")) {
				daemonPath = homeDir + "/Downloads/" + osxDaemon;
				url = getDaemonUrl(OS.MAC);
				basePath = "${user.home}/Library/Application Support/UNIGRID/dependencies/lib/";
			} else if (filePath[i].contains("windows")) {
				daemonPath = homeDir + "/Downloads/" + windowsDaemon;
				url = getDaemonUrl(OS.WINDOWS);
				basePath = "${user.home}/AppData/Roaming/UNIGRID/dependencies/lib/";
			}

			Configuration config = Configuration.builder()
				.basePath(basePath)
				.file(FileMetadata.readFrom("../../fx/target/fx-1.0.8-SNAPSHOT.jar")
					.uri(fxJarUrl).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/aopalliance-repackaged-3.0.3.jar")
					.uri(mavenUrl("org.glassfish.hk2.external", "aopalliance-repackaged", "3.0.3")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-beanutils-1.9.4.jar")
					.uri(mavenUrl("commons-beanutils", "commons-beanutils", "1.9.4")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-collections-3.2.2.jar")
					.uri(mavenUrl("commons-collections", "commons-collections", "3.2.2")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-configuration2-2.7.jar")
					.uri(mavenUrl("org.apache.commons", "commons-configuration2", "2.7")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-io-2.8.0.jar")
					.uri("https://repo1.maven.org/maven2/commons-io/commons-io/2.8.0/commons-io-2.8.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-lang3-3.8.1.jar")
					.uri(mavenUrl("org.apache.commons", "commons-lang3", "3.8.1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-logging-1.2.jar")
					.uri(mavenUrl("commons-logging", "commons-logging", "1.2")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-text-1.8.jar")
					.uri(mavenUrl("org.apache.commons", "commons-text", "1.8")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/controlsfx-11.1.1.jar")
					.uri(mavenUrl("org.controlsfx", "controlsfx", "11.1.1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-api-3.0.3.jar")
					.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/3.0.3/hk2-api-3.0.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-locator-3.0.3.jar")
					.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/3.0.3/hk2-locator-3.0.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-utils-3.0.3.jar")
					.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/3.0.3/hk2-utils-3.0.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/FastInfoset-2.1.0.jar")
					.uri(mavenUrl("com.sun.xml.fastinfoset", "FastInfoset", "2.1.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-core-12.3.0.jar")
					.uri(mavenUrl("org.kordamp.ikonli", "ikonli-core", "12.3.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-javafx-12.3.0.jar")
					.uri(mavenUrl("org.kordamp.ikonli", "ikonli-javafx", "12.3.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-fontawesome5-pack-12.3.0.jar")
					.uri(mavenUrl("org.kordamp.ikonli", "ikonli-fontawesome5-pack", "12.3.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-annotations-2.13.3.jar")
					.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.3/jackson-annotations-2.13.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-core-2.13.3.jar")
					.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.3/jackson-core-2.13.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-databind-2.13.3.jar")
					.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.3/jackson-databind-2.13.3.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.annotation-api-2.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/annotation/jakarta.annotation-api/2.0.0/jakarta.annotation-api-2.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.el-api-4.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/el/jakarta.el-api/4.0.0/jakarta.el-api-4.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.enterprise.cdi-api-3.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/enterprise/jakarta.enterprise.cdi-api/3.0.0/jakarta.enterprise.cdi-api-3.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.inject-api-2.0.1.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.interceptor-api-2.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/interceptor/jakarta.interceptor-api/2.0.0/jakarta.interceptor-api-2.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.json.bind-api-2.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/json/bind/jakarta.json.bind-api/2.0.0/jakarta.json.bind-api-2.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.json-2.0.0-RC1-module.jar")
					.uri("https://repo1.maven.org/maven2/org/glassfish/jakarta.json/2.0.0-RC1/jakarta.json-2.0.0-RC1-module.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.json-api-2.0.2.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/json/jakarta.json-api/2.0.2/jakarta.json-api-2.0.2.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.ws.rs-api-3.0.0.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/ws/rs/jakarta.ws.rs-api/3.0.0/jakarta.ws.rs-api-3.0.0.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.xml.bind-api-3.0.1.jar")
					.uri("https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/3.0.1/jakarta.xml.bind-api-3.0.1.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javassist-3.28.0-GA.jar")
					.uri(mavenUrl("org.javassist", "javassist", "3.28.0-GA")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-core-3.0.2.jar")
					.uri(mavenUrl("org.glassfish.jaxb", "jaxb-core", "3.0.2")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/istack-commons-runtime-4.0.1.jar")
					.uri(mavenUrl("com.sun.istack", "istack-commons-runtime", "4.0.1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/txw2-3.0.2.jar")
					.uri(mavenUrl("org.glassfish.jaxb", "txw2", "3.0.2")).modulepath())
				/*.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-impl-4.0.0.jar")
					.uri(mavenUrl("com.sun.xml.bind", "jaxb-impl", "4.0.0")).modulepath())*/
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-runtime-3.0.2.jar")
					.uri(mavenUrl("org.glassfish.jaxb","jaxb-runtime","3.0.2")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-2.0.1.jar")
					.uri("https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.1/jakarta.activation-2.0.1.jar").modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-classfilewriter-1.2.5.Final.jar")
					.uri(mavenUrl("org.jboss.classfilewriter", "jboss-classfilewriter", "1.2.5.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-logging-3.4.3.Final.jar")
					.uri(mavenUrl("org.jboss.logging", "jboss-logging", "3.4.3.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-hk2-3.0.5.jar")
					.uri(mavenUrl("org.glassfish.jersey.inject", "jersey-hk2", "3.0.5")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-client-3.0.5.jar")
					.uri(mavenUrl("org.glassfish.jersey.core", "jersey-client", "3.0.5")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-common-3.0.5.jar")
					.uri(mavenUrl("org.glassfish.jersey.core", "jersey-common", "3.0.5")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-json-binding-3.0.5.jar")
					.uri(mavenUrl("org.glassfish.jersey.media", "jersey-media-json-binding", "3.0.5")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-jaxb-3.0.5.jar")
					.uri(mavenUrl("org.glassfish.jersey.media", "jersey-media-jaxb", "3.0.5")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jipsy-annotations-1.1.1.jar")
					.uri(mavenUrl("org.kordamp.jipsy", "jipsy-annotations", "1.1.1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-5.5.0.jar")
					.uri(mavenUrl("net.java.dev.jna", "jna", "5.5.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-platform-5.5.0.jar")
					.uri(mavenUrl("net.java.dev.jna", "jna-platform", "5.5.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jsch-0.1.55.jar")
					.uri(mavenUrl("com.jcraft", "jsch", "0.1.55")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/logback-classic-1.3.0-alpha16.jar")
					.uri(mavenUrl("ch.qos.logback", "logback-classic", "1.3.0-alpha16")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/logback-core-1.3.0-alpha16.jar")
					.uri(mavenUrl("ch.qos.logback", "logback-core", "1.3.0-alpha16")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/lombok-1.18.24.jar")
					.uri(mavenUrl("org.projectlombok", "lombok", "1.18.24")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/osgi-resource-locator-1.0.3.jar")
					.uri(mavenUrl("org.glassfish.hk2", "osgi-resource-locator", "1.0.3")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/parsson-1.0.0.jar")
					.uri(mavenUrl("org.eclipse.parsson", "parsson", "1.0.0")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/slf4j-api-2.0.0-alpha7.jar")
					.uri(mavenUrl("org.slf4j", "slf4j-api", "2.0.0-alpha7")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-api-4.0.SP1.jar")
					.uri(mavenUrl("org.jboss.weld", "weld-api", "4.0.SP1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-core-impl-4.0.3.Final.jar")
					.uri(mavenUrl("org.jboss.weld", "weld-core-impl", "4.0.3.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-environment-common-4.0.3.Final.jar")
					.uri(mavenUrl("org.jboss.weld.environment", "weld-environment-common", "4.0.3.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-probe-core-4.0.3.Final.jar")
					.uri(mavenUrl("org.jboss.weld.probe", "weld-probe-core", "4.0.3.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-se-core-4.0.3.Final.jar")
					.uri(mavenUrl("org.jboss.weld.se", "weld-se-core", "4.0.3.Final")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-spi-4.0.SP1.jar")
					.uri(mavenUrl("org.jboss.weld", "weld-spi", "4.0.SP1")).modulepath())
				.file(FileMetadata.readFrom("../../fx/target/jlink/cp/yasson-2.0.0-M1.jar")
					.uri(mavenUrl("org.eclipse", "yasson", "2.0.0-M1")).modulepath())
				.file(FileMetadata.readFrom(daemonPath).uri(url))
				.property("maven.central", MAVEN_BASE)
				.property("fx.version", fxVersion)
				.property("default.launcher.main.class", "org.unigrid.janus.Janus")
				.build();

			try ( Writer out = Files.newBufferedWriter(Paths.get(dir + filePath[i]))) {
				config.write(out);
				System.out.println(dir);
			}

		}

		/*
		String cacheLoc = System.getProperty("maven.dir") + "/fxcache";
		dir = configLocation + "/unigrid";
		
		cacheJavafx();
		
		config = Configuration.builder()
			.basePath("${user.dir}/unigrid")
			
			.property("maven.central", MAVEN_BASE)
			.property("maven.central.javafx", "${maven.central}/org/openjfx/")
			.build();
		
		try(Writer out = Files.newBufferedWriter(Paths.get(configLocation + "/setup.xml"))) {
			config.write(out);
		}*/
	}

	private String getDaemonPath() {
		String s = "";

		return s;
	}

	private static String getDaemonUrl(OS os) {
		String s = "";

		String jsonSearch = "$['assets'][*]['browser_download_url']";
		DocumentContext jsonPath = null;
		try {
			jsonPath = JsonPath.parse(new URL("https://api.github.com/repos/unigrid-project/daemon/releases/latest"));

		} catch (Exception e) {

		}
		List<String> githubUrls = jsonPath.read(jsonSearch);

		if (os.equals(OS.LINUX)) {
			/*List<Map<String, Object>> data = jsonPath.read("$['assets'][*][?('linux' in @['browser_download_url'])]");
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
	}

	private static String mavenUrl(String groupId, String artifactId, String version, OS os) {
		StringBuilder builder = new StringBuilder();
		builder.append(MAVEN_BASE + "/");
		builder.append(groupId.replace('.', '/') + "/");
		builder.append(artifactId.replace('.', '-') + "/");
		builder.append(version + '/');
		builder.append(artifactId.replace('.', '-') + "-" + version);

		if (os != null) {
			builder.append("-" + os.getShortName());
		}

		builder.append(".jar");

		return builder.toString();
	}

	private static String mavenUrl(String groupId, String artifactId, String version) {
		return mavenUrl(groupId, artifactId, version, null);
	}

	private static String extractJavafxURL(Path path, OS os) {
		Pattern regex = Pattern.compile("javafx-([a-z]+)-([0-9.]+)(?:-(win|mac|linux))?\\.jar");
		Matcher match = regex.matcher(path.getFileName().toString());

		if (!match.find()) {
			return null;
		}

		String module = match.group(1);
		String version = match.group(2);
		if (os == null && match.groupCount() > 2) {
			os = OS.fromShortName(match.group(3));
		}

		return mavenUrl("org.openjfx", "javafx." + module, version, os);
	}

	private static String injectOs(String file, OS os) {
		return file.replaceAll("(.+)\\.jar", "$1-" + os.getShortName() + ".jar");
	}

	private static void cacheJavafx() throws IOException {
		String names = System.getProperty("target") + "/javafx";
		Path cacheDir = Paths.get(System.getProperty("maven.dir"), "fxcache");

		try ( Stream<Path> files = Files.list(Paths.get(names))) {
			files.forEach(f -> {
				try {

					if (!Files.isDirectory(cacheDir)) {
						Files.createDirectory(cacheDir);
					}

					for (OS os : EnumSet.of(OS.WINDOWS, OS.MAC, OS.LINUX)) {
						Path file = cacheDir.resolve(injectOs(f.getFileName().toString(), os));

						if (Files.notExists(file)) {
							String download = extractJavafxURL(f, os);
							URI uri = URI.create(download);
							try ( InputStream in = uri.toURL().openStream()) {
								Files.copy(in, file);
							}
						}
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
