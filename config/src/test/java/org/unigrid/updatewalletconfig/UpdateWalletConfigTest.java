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

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.repository.LocalRepository;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.unigrid.jqwik.BaseMockedWeldTest;
import org.unigrid.jqwik.WeldSetup;
import org.update4j.OS;

@WeldSetup(UpdateWalletConfig.class)
public class UpdateWalletConfigTest extends BaseMockedWeldTest {
	@Inject
	private UpdateWalletConfig config;

	private org.eclipse.aether.graph.Dependency createDependency(String name) {
		final DefaultArtifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(name);
		return new org.eclipse.aether.graph.Dependency(artifact, "");
	}

	private void setupRootArtifact(String name, CollectRequest request) {
		final DefaultArtifact artifact = new DefaultArtifact(name);

		request.setRootArtifact(artifact);
		request.setRoot(new org.eclipse.aether.graph.Dependency(artifact, ""));
	}

	@Example @Disabled
	public <T extends RepositorySystem> void shouldBeAbleToHandleSingleDependency()
		throws PlexusContainerException, MavenExecutionException, IOException {

		new MockUp<UpdateWalletConfig>() {
			@Mock
			public List<FileMetadata> getDependencies(Invocation inv, String currentArtifact) {
				currentArtifact = currentArtifact.contains("org.unigrid:bootstrap")
					? "org.unigrid.test:bootstrap:1.0.9-SNAPSHOT" : currentArtifact;
				return inv.proceed(currentArtifact);
			}

			@Mock
			public List<FileMetadata> getExternalDependencies() {
				return new ArrayList<FileMetadata>();
			}

			@Mock
			public String getFileUrl(OS os, boolean testing) {
				String isTesting = testing ? "-test" : "";

				return System.getProperty("user.dir").concat("/target/") + "config-"
					+ os.getShortName() + isTesting + ".xml";
			}
		};

		new MockUp<ConfMarshaller>() {
			@Mock
			public void mashal(Invocation inv, Configuration configuration, String destination) {
				configuration.setTimestamp("2022-10-07T12:33:24.850Z");
				inv.proceed();
			}
		};

		new MockUp<MavenSession>() {
			@Mock
			public RepositorySystemSession getRepositorySession() {
				return MavenRepositorySystemUtils.newSession();
			}
		};

		new MockUp<MavenProject>() {
			@Mock
			public File getBasedir() {
				return new File(System.getProperty("user.dir").concat("/src/test/resources"));
			}
		};

		new MockUp<DefaultRepositorySystemSession>() {
			@Mock
			public LocalRepository getLocalRepository() {
				return new LocalRepository(System.getProperty("user.home")
					.concat("/.m2/repository"));
			}
		};

		new MockUp<T>() {
			@Mock
			public CollectResult collectDependencies(Invocation inv, RepositorySystemSession defSession,
				CollectRequest collectRequest) {

				if (collectRequest.getRoot().getArtifact().getGroupId().equals("org.unigrid.test")) {
					if (collectRequest.getRoot().getArtifact().getArtifactId().equals("fx")) {
						setupRootArtifact("org.unigrid.test:fx:1.0.9-SNAPSHOT", collectRequest);

						final org.eclipse.aether.graph.Dependency depOne =
							createDependency("com.google.inject:guice:5.1.0");

						final org.eclipse.aether.graph.Dependency depTwo =
							createDependency("org.openjfx:javafx-controls:jar:17-ea+7");

						final org.eclipse.aether.graph.Dependency depThree =
							createDependency("org.slf4j:slf4j-api:2.0.0-alpha7");

						collectRequest.setDependencies(Arrays.asList(depOne, depTwo, depThree));
					} else if (collectRequest.getRoot().getArtifact().getArtifactId().equals("bootstrap")) {
						org.eclipse.aether.artifact.DefaultArtifact artifact = new org.eclipse.aether.artifact.DefaultArtifact("org.unigrid.test:bootstrap:1.0.9-SNAPSHOT");
						collectRequest.setRootArtifact(artifact);
						collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact, ""));
						org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(new org.eclipse.aether.artifact.DefaultArtifact("org.openjfx:javafx-controls:jar:17-ea+7"), "");
						collectRequest.setDependencies(Arrays.asList(dependency));
					}
				}

				return inv.proceed();
			}
		};

		config.afterSessionEnd(setupMavenSession());

		System.out.println(UpdateWalletConfig.class.getResource("config-linux-test.xml"));
		System.out.println(UpdateWalletConfigTest.class.getResource("config-linux-test.xml"));
		System.out.println(getClass().getResource("config-linux-test.xml"));
		//assertConfigFileIsEquals();
	}

	public MavenSession setupMavenSession() throws PlexusContainerException {
		Model projectModel = getModels("fx");
		Model bootstrapProjectModel = getModels("bootstrap");

		Dependency dependencyOne = new Dependency();
		dependencyOne.setGroupId("com.google.inject");
		dependencyOne.setArtifactId("guice");
		dependencyOne.setVersion("5.1.0");

		projectModel.addDependency(dependencyOne);

		Properties properties = new Properties();
		properties.setProperty("config.opens", "fx=com.google.guava/guava");
		properties.setProperty("config.exports", "fx=org.openjfx/javafx-controls");
		projectModel.setProperties(properties);

		PlexusContainer container = new DefaultPlexusContainer();
		MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		MavenExecutionResult result = new DefaultMavenExecutionResult();

		request.setGoals(Arrays.asList("install"));

		MavenSession session = new MavenSession(container, MavenRepositorySystemUtils.newSession(), request, result);

		List list = new ArrayList();
		list.add(new MavenProject(projectModel));
		list.add(new MavenProject(bootstrapProjectModel));
		session.setProjects(list);

		return session;
	}

	public Model getModels(String artifactId) {
		Model projectModel = new Model();

		projectModel.setModelVersion("4.0.0");
		projectModel.setGroupId("org.unigrid.test");
		projectModel.setArtifactId(artifactId);
		projectModel.setVersion("1.0.9-SNAPSHOT");

		return projectModel;
	}

	public void assertConfigFileIsEquals() throws IOException {
		File FileTarget = new File(System.getProperty("user.dir").concat("/src/test/resources/config-linux-test.xml"));
		File FileResource = new File(System.getProperty("user.dir").concat("/target/config-linux-test.xml"));

		boolean fileSizeIsEqual = FileTarget.length() == FileResource.length();
		long mismatch = Files.mismatch(FileTarget.toPath(), FileResource.toPath());

		assertThat(fileSizeIsEqual, equalTo(true));
		assertThat(mismatch, equalTo(-1L));
	}
}
