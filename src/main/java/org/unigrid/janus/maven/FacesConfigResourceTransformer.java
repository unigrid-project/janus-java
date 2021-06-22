/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
*/

package org.unigrid.janus.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;
import org.apache.maven.shared.utils.io.IOUtil;
import org.w3c.dom.Document;

public class FacesConfigResourceTransformer implements ReproducibleResourceTransformer {
	private static final String[] DIRECTORIES = {"META-INF"};
	private static final String OLD_FACES_CONFIG_FILE = "/faces-config.xml";
	private static final String NEW_FACES_CONFIG_PATH = "META-INF/resources/config/faces-config-%s.xml";

	private Optional<String> currentResourceName;
	private final List<Resource> resourceEntries = new ArrayList<>();
	private long youngestTime = 0;

	@Data @AllArgsConstructor
	private static class Resource {
		private String name;
		private String content;
	}

	@Override
	public boolean canTransformResource(String resource) {
		for (String directory : DIRECTORIES) {
			if (resource.startsWith(directory) && resource.endsWith(OLD_FACES_CONFIG_FILE)) {
				currentResourceName = Optional.of(NEW_FACES_CONFIG_PATH);
				return true;
			}
		}

		currentResourceName = Optional.empty();
		return false;
	}

	@Override @Deprecated
	public void processResource(String resource, InputStream resourceStream, List<Relocator> relocators)
		throws IOException {
		/* Deprecated as of 3.2.4 of the shader plugin */
	}

	@Override @SneakyThrows
	public void processResource(String resource, InputStream resourceStream, List<Relocator> relocators, long time)
		throws IOException {

		final String content = new String(resourceStream.readAllBytes());
		resourceStream.close();

		final InputStream is = IOUtils.toInputStream(content, Charset.defaultCharset());
		final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		final XPath xPath = XPathFactory.newInstance().newXPath();

		try {
			String name = xPath.compile("faces-config/name").evaluate(document).toLowerCase();

			/* If we can't determine a name, we just give it a number based on how many we have found so far */
			if (name.isEmpty()) {
				name = String.valueOf(resourceEntries.size());
			}

			currentResourceName = Optional.of(String.format(currentResourceName.get(), name));
			resourceEntries.add(new Resource(currentResourceName.get(), content));

		} catch (XPathExpressionException ex) {
			throw new IOException("A faces-config.xml is missing a name tag.", ex);
		} finally {
			is.close();
		}

		youngestTime = Math.max(youngestTime, time);
	}

	@Override
	public boolean hasTransformedResource() {
		return !resourceEntries.isEmpty();
	}

	@Override
	public void modifyOutputStream(JarOutputStream stream) throws IOException {
		for (Resource e : resourceEntries) {
			final JarEntry jarEntry = new JarEntry(e.getName());

			jarEntry.setTime(youngestTime);
			stream.putNextEntry(jarEntry);
			IOUtil.copy(e.getContent(), stream);
		}
	}
}
