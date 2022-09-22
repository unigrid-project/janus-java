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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data()
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class Configuration {

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private String timestamp;

	@Getter(AccessLevel.PROTECTED)
	@XmlElement(name = "base")
	private BasePath basePath;

	@Getter(AccessLevel.PROTECTED)
	@XmlElementWrapper(name = "properties")
	@XmlElement(name = "property")
	private List<Property> properties = new ArrayList();

	@Getter(AccessLevel.PROTECTED)
	@XmlElementWrapper(name = "files")
	@XmlElement(name = "file")
	private List<FileMetadata> files;

	public Configuration() {
		timestamp = new Date().toInstant().toString();
		basePath = new BasePath();
		properties.add(new Property("maven.central", "https://repo1.maven.org/maven2"));
		properties.add(new Property("default.launcher.main.class", "org.unigrid.janus.Janus"));
	}
}
