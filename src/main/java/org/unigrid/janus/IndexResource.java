package org.unigrid.janus;

//import cloud.piranha.resource.api.Resource;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.List;
//import java.util.jar.JarFile;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//import java.util.zip.ZipEntry;
import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;

@RequiredArgsConstructor
public class IndexResource { // implements Resource {
	//private final String resource;

	/*@Override @SneakyThrows
	public URL getResource(String location) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String location) {
		return null;
	}*/

	/*@Override @SneakyThrows
	public Stream<String> getAllLocations() {
		final String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm();
		final String[] segments = path.split("!");

		if (segments.length > 0) {
			List<String> names = null;

			try (JarFile jar = new JarFile(segments[0].replace("jar:file:", ""))) {
				names = jar.stream()
					.map(ZipEntry::getName)
					.map(f -> "/" + f)
					.filter(f -> f.startsWith(resource) && f.endsWith(".class"))
					.collect(Collectors.toList());
			}

			return names.stream();
		}

		return null;
	}*/
}
