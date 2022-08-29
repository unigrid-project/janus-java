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
		String fxJarUrl = "https://github.com/unigrid-project/unigrid-update/releases/download/v1.0.6/fx-1.0.6-SNAPSHOT.jar";
		String linuxDaemon = "unigrid-2.9.2-x86_64-linux-gnu.tar.gz";
		String windowsDaemon = "unigrid-2.9.2-win64.zip";
		String osxDaemon = "unigrid-2.9.2-osx64.tar.gz";

		Configuration config = Configuration.builder()
			//.baseUri("https://drive.google.com/file/d/1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay/view?usp=sharing")
			//.basePath("../../desktop/target/dist/Unigrid/lib/app/mods/")
			.basePath("${user.home}/.unigrid/dependencies/lib/")
			.file(FileMetadata.readFrom("../../fx/target/fx-1.0.6-SNAPSHOT.jar")
				//.uri("https://drive.google.com/uc?export=download&id=1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay").modulepath())
				.uri(fxJarUrl).modulepath())
				//.path("../../fx/target/fx-1.0.6-SNAPSHOT.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/accessors-smart-1.2.jar")
				.uri(mavenUrl("net.minidev","accessors-smart","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/aopalliance-repackaged-3.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2.external","aopalliance-repackaged","3.0.3")).modulepath())
			//.file(FileMetadata.readFrom("../../fx/target/jlink/cp/asm-5.0.4.jar")
			//	.uri(mavenUrl("org.ow2.asm","asm","5.0.4")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-beanutils-1.9.4.jar")
				.uri(mavenUrl("commons-beanutils","commons-beanutils","1.9.4")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-collections-3.2.2.jar")
				.uri(mavenUrl("commons-collections","commons-collections","3.2.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-configuration2-2.7.jar")
				.uri(mavenUrl("org.apache.commons","commons-configuration2","2.7")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-io-2.8.0.jar")
				.uri("https://repo1.maven.org/maven2/commons-io/commons-io/2.8.0/commons-io-2.8.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-lang3-3.8.1.jar")
				.uri(mavenUrl("org.apache.commons","commons-lang3","3.8.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-logging-1.2.jar")
				.uri(mavenUrl("commons-logging","commons-logging","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-text-1.8.jar")
				.uri(mavenUrl("org.apache.commons","commons-text","1.8")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/controlsfx-11.1.1.jar")
				.uri(mavenUrl("org.controlsfx","controlsfx","11.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-api-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/3.0.3/hk2-api-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-locator-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/3.0.3/hk2-locator-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-utils-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/3.0.3/hk2-utils-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/FastInfoset-2.1.0.jar")
				.uri(mavenUrl("com.sun.xml.fastinfoset","FastInfoset","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-core-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-core","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-javafx-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-javafx","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-fontawesome5-pack-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-fontawesome5-pack","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-annotations-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.3/jackson-annotations-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-core-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.3/jackson-core-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-databind-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.3/jackson-databind-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-2.0.0.jar")
				.uri("https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.0/jakarta.activation-2.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-api-2.1.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.0/jakarta.activation-api-2.1.0.jar").modulepath())
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
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.xml.bind-api-3.0.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/3.0.0/jakarta.xml.bind-api-3.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javax.annotation-api-1.3.2.jar")
				.uri("https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javassist-3.28.0-GA.jar")
				.uri(mavenUrl("org.javassist","javassist","3.28.0-GA")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-core-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-core","3.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-impl-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-impl","3.0.0")).modulepath())
			/*.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-runtime-3.0.0.jar")
				.uri(mavenUrl("org.glassfish.jaxb","jaxb-runtime","3.0.0")).modulepath())*/
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-classfilewriter-1.2.5.Final.jar")
				.uri(mavenUrl("org.jboss.classfilewriter","jboss-classfilewriter","1.2.5.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-logging-3.4.3.Final.jar")
				.uri(mavenUrl("org.jboss.logging","jboss-logging","3.4.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-hk2-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.inject","jersey-hk2","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-client-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-client","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-common-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-common","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-json-binding-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-json-binding","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-jaxb-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jipsy-annotations-1.1.1.jar")
				.uri(mavenUrl("org.kordamp.jipsy","jipsy-annotations","1.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-platform-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna-platform","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jsch-0.1.55.jar")
				.uri(mavenUrl("com.jcraft","jsch","0.1.55")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-smart-2.3.jar")
				.uri(mavenUrl("net.minidev","json-smart","2.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/lombok-1.18.24.jar")
				.uri(mavenUrl("org.projectlombok","lombok","1.18.24")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/osgi-resource-locator-1.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2","osgi-resource-locator","1.0.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/parsson-1.0.0.jar")
				.uri(mavenUrl("org.eclipse.parsson","parsson","1.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/slf4j-api-1.7.25.jar")
				.uri(mavenUrl("org.slf4j","slf4j-api","1.7.25")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/stax-ex-2.1.0.jar")
				.uri(mavenUrl("org.jvnet.staxex","stax-ex","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-api-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-api","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-core-impl-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld","weld-core-impl","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-environment-common-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.environment","weld-environment-common","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-probe-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.probe","weld-probe-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-se-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.se","weld-se-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-spi-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-spi","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/yasson-2.0.0-M1.jar")
				.uri(mavenUrl("org.eclipse","yasson","2.0.0-M1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-jaxb-3.0.6.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.6")).modulepath())
			.file(FileMetadata.readFrom(homeDir + "/Downloads/" + linuxDaemon).uri(getDaemonUrl(OS.LINUX)))
			.property("maven.central", MAVEN_BASE)
			.property("default.launcher.main.class", "org.unigrid.janus.Janus")
			.build();
		
		try (Writer out = Files.newBufferedWriter(Paths.get(dir + "/config-linux.xml"))){
			config.write(out);
			System.out.println(dir);
		}
		
		Configuration configWindows = Configuration.builder()
			//.baseUri("https://drive.google.com/file/d/1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay/view?usp=sharing")
			//.basePath("../../desktop/target/dist/Unigrid/lib/app/mods/")
			.basePath("${user.home}/AppData/Roaming/UNIGRID/dependencies/lib/")
			.file(FileMetadata.readFrom("../../fx/target/fx-1.0.6-SNAPSHOT.jar")
				//.uri("https://drive.google.com/uc?export=download&id=1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay").modulepath())
				.uri(fxJarUrl).modulepath())
				//.path("../../fx/target/fx-1.0.6-SNAPSHOT.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/accessors-smart-1.2.jar")
				.uri(mavenUrl("net.minidev","accessors-smart","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/aopalliance-repackaged-3.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2.external","aopalliance-repackaged","3.0.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-beanutils-1.9.4.jar")
				.uri(mavenUrl("commons-beanutils","commons-beanutils","1.9.4")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-collections-3.2.2.jar")
				.uri(mavenUrl("commons-collections","commons-collections","3.2.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-configuration2-2.7.jar")
				.uri(mavenUrl("org.apache.commons","commons-configuration2","2.7")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-io-2.8.0.jar")
				.uri("https://repo1.maven.org/maven2/commons-io/commons-io/2.8.0/commons-io-2.8.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-lang3-3.8.1.jar")
				.uri(mavenUrl("org.apache.commons","commons-lang3","3.8.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-logging-1.2.jar")
				.uri(mavenUrl("commons-logging","commons-logging","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-text-1.8.jar")
				.uri(mavenUrl("org.apache.commons","commons-text","1.8")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/controlsfx-11.1.1.jar")
				.uri(mavenUrl("org.controlsfx","controlsfx","11.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-api-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/3.0.3/hk2-api-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-locator-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/3.0.3/hk2-locator-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-utils-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/3.0.3/hk2-utils-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/FastInfoset-2.1.0.jar")
				.uri(mavenUrl("com.sun.xml.fastinfoset","FastInfoset","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-core-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-core","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-javafx-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-javafx","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-fontawesome5-pack-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-fontawesome5-pack","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-annotations-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.3/jackson-annotations-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-core-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.3/jackson-core-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-databind-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.3/jackson-databind-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-2.0.0.jar")
				.uri("https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.0/jakarta.activation-2.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-api-2.1.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.0/jakarta.activation-api-2.1.0.jar").modulepath())
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
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.xml.bind-api-3.0.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/3.0.0/jakarta.xml.bind-api-3.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javax.annotation-api-1.3.2.jar")
				.uri("https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javassist-3.28.0-GA.jar")
				.uri(mavenUrl("org.javassist","javassist","3.28.0-GA")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-core-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-core","3.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-impl-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-impl","3.0.0")).modulepath())
			/*.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-runtime-3.0.0.jar")
				.uri(mavenUrl("org.glassfish.jaxb","jaxb-runtime","3.0.0")).modulepath())*/
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-classfilewriter-1.2.5.Final.jar")
				.uri(mavenUrl("org.jboss.classfilewriter","jboss-classfilewriter","1.2.5.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-logging-3.4.3.Final.jar")
				.uri(mavenUrl("org.jboss.logging","jboss-logging","3.4.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-hk2-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.inject","jersey-hk2","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-client-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-client","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-common-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-common","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-json-binding-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-json-binding","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-jaxb-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jipsy-annotations-1.1.1.jar")
				.uri(mavenUrl("org.kordamp.jipsy","jipsy-annotations","1.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-platform-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna-platform","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jsch-0.1.55.jar")
				.uri(mavenUrl("com.jcraft","jsch","0.1.55")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-smart-2.3.jar")
				.uri(mavenUrl("net.minidev","json-smart","2.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/lombok-1.18.24.jar")
				.uri(mavenUrl("org.projectlombok","lombok","1.18.24")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/osgi-resource-locator-1.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2","osgi-resource-locator","1.0.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/parsson-1.0.0.jar")
				.uri(mavenUrl("org.eclipse.parsson","parsson","1.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/slf4j-api-1.7.25.jar")
				.uri(mavenUrl("org.slf4j","slf4j-api","1.7.25")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/stax-ex-2.1.0.jar")
				.uri(mavenUrl("org.jvnet.staxex","stax-ex","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-api-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-api","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-core-impl-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld","weld-core-impl","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-environment-common-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.environment","weld-environment-common","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-probe-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.probe","weld-probe-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-se-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.se","weld-se-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-spi-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-spi","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/yasson-2.0.0-M1.jar")
				.uri(mavenUrl("org.eclipse","yasson","2.0.0-M1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.5")).modulepath())
			.file(FileMetadata.readFrom(homeDir + "/Downloads/" + windowsDaemon).uri(getDaemonUrl(OS.WINDOWS)))
			.property("maven.central", MAVEN_BASE)
			.property("default.launcher.main.class", "org.unigrid.janus.Janus")
			.build();
		
		try (Writer out = Files.newBufferedWriter(Paths.get(dir + "/config-windows.xml"))){
			configWindows.write(out);
		}

		Configuration configMac = Configuration.builder()
			//.baseUri("https://drive.google.com/file/d/1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay/view?usp=sharing")
			//.basePath("../../desktop/target/dist/Unigrid/lib/app/mods/")
			.basePath("${user.home}/Library/Application Support/UNIGRID/dependencies/lib/")
			.file(FileMetadata.readFrom("../../fx/target/fx-1.0.6-SNAPSHOT.jar")
				//.uri("https://drive.google.com/uc?export=download&id=1IhV5soH9Kvt7zZjlBkWAikLyyv1DY6Ay").modulepath())
				.uri(fxJarUrl).modulepath())
				//.path("../../fx/target/fx-1.0.6-SNAPSHOT.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/accessors-smart-1.2.jar")
				.uri(mavenUrl("net.minidev","accessors-smart","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/aopalliance-repackaged-3.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2.external","aopalliance-repackaged","3.0.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-beanutils-1.9.4.jar")
				.uri(mavenUrl("commons-beanutils","commons-beanutils","1.9.4")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-collections-3.2.2.jar")
				.uri(mavenUrl("commons-collections","commons-collections","3.2.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-configuration2-2.7.jar")
				.uri(mavenUrl("org.apache.commons","commons-configuration2","2.7")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-io-2.8.0.jar")
				.uri("https://repo1.maven.org/maven2/commons-io/commons-io/2.8.0/commons-io-2.8.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-lang3-3.8.1.jar")
				.uri(mavenUrl("org.apache.commons","commons-lang3","3.8.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-logging-1.2.jar")
				.uri(mavenUrl("commons-logging","commons-logging","1.2")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/commons-text-1.8.jar")
				.uri(mavenUrl("org.apache.commons","commons-text","1.8")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/controlsfx-11.1.1.jar")
				.uri(mavenUrl("org.controlsfx","controlsfx","11.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-api-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/3.0.3/hk2-api-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-locator-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/3.0.3/hk2-locator-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/hk2-utils-3.0.3.jar")
				.uri("https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/3.0.3/hk2-utils-3.0.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/FastInfoset-2.1.0.jar")
				.uri(mavenUrl("com.sun.xml.fastinfoset","FastInfoset","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-core-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-core","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-javafx-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-javafx","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/ikonli-fontawesome5-pack-12.3.0.jar")
				.uri(mavenUrl("org.kordamp.ikonli","ikonli-fontawesome5-pack","12.3.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-annotations-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.3/jackson-annotations-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-core-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.3/jackson-core-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jackson-databind-2.13.3.jar")
				.uri("https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.3/jackson-databind-2.13.3.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-2.0.0.jar")
				.uri("https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.0/jakarta.activation-2.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.activation-api-2.1.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.0/jakarta.activation-api-2.1.0.jar").modulepath())
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
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jakarta.xml.bind-api-3.0.0.jar")
				.uri("https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/3.0.0/jakarta.xml.bind-api-3.0.0.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javax.annotation-api-1.3.2.jar")
				.uri("https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar").modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/javassist-3.28.0-GA.jar")
				.uri(mavenUrl("org.javassist","javassist","3.28.0-GA")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-core-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-core","3.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-impl-3.0.0.jar")
				.uri(mavenUrl("com.sun.xml.bind","jaxb-impl","3.0.0")).modulepath())
			/*.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jaxb-runtime-3.0.0.jar")
				.uri(mavenUrl("org.glassfish.jaxb","jaxb-runtime","3.0.0")).modulepath())*/
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-classfilewriter-1.2.5.Final.jar")
				.uri(mavenUrl("org.jboss.classfilewriter","jboss-classfilewriter","1.2.5.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jboss-logging-3.4.3.Final.jar")
				.uri(mavenUrl("org.jboss.logging","jboss-logging","3.4.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-hk2-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.inject","jersey-hk2","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-client-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-client","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-common-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.core","jersey-common","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-json-binding-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-json-binding","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jersey-media-jaxb-3.0.5.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.5")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jipsy-annotations-1.1.1.jar")
				.uri(mavenUrl("org.kordamp.jipsy","jipsy-annotations","1.1.1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jna-platform-5.5.0.jar")
				.uri(mavenUrl("net.java.dev.jna","jna-platform","5.5.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/jsch-0.1.55.jar")
				.uri(mavenUrl("com.jcraft","jsch","0.1.55")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-smart-2.3.jar")
				.uri(mavenUrl("net.minidev","json-smart","2.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/lombok-1.18.24.jar")
				.uri(mavenUrl("org.projectlombok","lombok","1.18.24")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/osgi-resource-locator-1.0.3.jar")
				.uri(mavenUrl("org.glassfish.hk2","osgi-resource-locator","1.0.3")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/parsson-1.0.0.jar")
				.uri(mavenUrl("org.eclipse.parsson","parsson","1.0.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/slf4j-api-1.7.25.jar")
				.uri(mavenUrl("org.slf4j","slf4j-api","1.7.25")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/stax-ex-2.1.0.jar")
				.uri(mavenUrl("org.jvnet.staxex","stax-ex","2.1.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-api-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-api","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-core-impl-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld","weld-core-impl","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-environment-common-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.environment","weld-environment-common","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-probe-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.probe","weld-probe-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-se-core-4.0.3.Final.jar")
				.uri(mavenUrl("org.jboss.weld.se","weld-se-core","4.0.3.Final")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/weld-spi-4.0.SP1.jar")
				.uri(mavenUrl("org.jboss.weld","weld-spi","4.0.SP1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/yasson-2.0.0-M1.jar")
				.uri(mavenUrl("org.eclipse","yasson","2.0.0-M1")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("com.jayway.jsonpath","json-path","2.4.0")).modulepath())
			.file(FileMetadata.readFrom("../../fx/target/jlink/cp/json-path-2.4.0.jar")
				.uri(mavenUrl("org.glassfish.jersey.media","jersey-media-jaxb","3.0.5")).modulepath())
			.file(FileMetadata.readFrom(homeDir + "/Downloads/" + osxDaemon).uri(getDaemonUrl(OS.MAC)))
			.property("maven.central", MAVEN_BASE)
			.property("default.launcher.main.class", "org.unigrid.janus.Janus")
			.build();

		try (Writer out = Files.newBufferedWriter(Paths.get(dir + "/config-mac.xml"))){
			configMac.write(out);
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
			
		}
		catch(Exception e) {
			
		}
		List<String> githubUrls = jsonPath.read(jsonSearch);
		
		if(os.equals(OS.LINUX)) {
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
		}
		else if(os.equals(OS.MAC)) {
			s = githubUrls.get(0);
			System.out.println(s);
		}
		else if(os.equals(OS.WINDOWS)) {
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
