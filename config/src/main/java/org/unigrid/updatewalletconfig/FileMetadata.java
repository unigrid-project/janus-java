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

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FileMetadata {

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private String uri;

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private long size;

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private String checksum;

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private boolean modulePath;

	@Getter(AccessLevel.PROTECTED)
	@XmlAttribute
	private boolean ignoreBootConflict;

	@Getter(AccessLevel.PROTECTED)
	@XmlElementWrapper(name = "addOpens")
	@XmlElement(name = "opens", required = false, nillable = true)
	private List<Package> opensPackages = null;

	@Getter(AccessLevel.PROTECTED)
	@XmlElementWrapper(name = "addExports")
	@XmlElement(name = "exports", required = false, nillable = true)
	private List<Package> exportsPackages = null;

	@Getter(AccessLevel.PROTECTED)
	@XmlTransient
	private String groupId;

	@Getter(AccessLevel.PROTECTED)
	@XmlTransient
	private String artifactId;

	public FileMetadata(String uri, long size, String checksum) {
		this(uri, size, checksum, "", "");
	}

	public FileMetadata(String uri, long size, String checksum, String groupId, String artifactId) {
		this.uri = uri;
		this.size = size;
		this.checksum = checksum;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.modulePath = true;
	}
}
