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
import lombok.Setter;

public class FileMetadata {

	@Setter
	@XmlAttribute
	private String uri;

	@Setter
	@XmlAttribute
	private long size;

	@Setter
	@XmlAttribute
	private String checksum;

	@Setter
	@XmlAttribute
	private boolean modulePath;

	@Setter
	@XmlAttribute
	private boolean ignoreBootConflict;

	public FileMetadata(String uri, long size, String checksum) {
		this(uri, size, checksum, true, false);
	}

	public FileMetadata(String uri, long size, String checksum, boolean modulePath) {
		this(uri, size, checksum, modulePath, false);
	}

	public FileMetadata(String uri, long size, String checksum, boolean modulePath, boolean ignoreBootConflict) {
		this.uri = uri;
		this.size = size;
		this.checksum = checksum;
		this.modulePath = modulePath;
		this.ignoreBootConflict = ignoreBootConflict;
	}

	public String getStringUri() {
		return uri;
	}

	@Override
	public boolean equals(Object anObject) {
		if (!(anObject instanceof FileMetadata)) {
			return false;
		}
		FileMetadata other = (FileMetadata) anObject;
		return other.getStringUri().equals(getStringUri());
	}
}
