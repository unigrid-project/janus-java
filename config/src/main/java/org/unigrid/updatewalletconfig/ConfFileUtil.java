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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Adler32;

public class ConfFileUtil {

	public static long getChecksum(Path path) throws IOException {
		try ( InputStream input = Files.newInputStream(path)) {
			Adler32 checksum = new Adler32();
			byte[] buf = new byte[1024 * 8];

			int read;
			while ((read = input.read(buf, 0, buf.length)) > -1) {
				checksum.update(buf, 0, read);
			}

			return checksum.getValue();
		}
	}

	public static long getChecksumByInputStream(InputStream is) throws IOException {
		Adler32 checksum = new Adler32();
		byte[] buf = new byte[1024 * 8];

		int read;
		while ((read = is.read(buf, 0, buf.length)) > -1) {
			checksum.update(buf, 0, read);
		}

		return checksum.getValue();
	}

	public static String getChecksumString(Path path) throws IOException {
		return Long.toHexString(getChecksum(path));
	}

	public static String getChecksumStringyByInputStream(InputStream is) throws IOException {
		return Long.toHexString(getChecksumByInputStream(is));
	}

	public static int getFileSize(URL url) {
		URLConnection conn = null;
		try {
			conn = url.openConnection();
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).setRequestMethod("HEAD");
			}
			conn.getInputStream();
			return conn.getContentLength();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).disconnect();
			}
		}
	}
}
