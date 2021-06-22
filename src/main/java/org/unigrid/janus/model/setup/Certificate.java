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

package org.unigrid.janus.model.setup;

import org.unigrid.janus.model.Preferences;
import io.github.stephenc.crypto.sscg.SelfSignedCertificate;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Locale;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public class Certificate {
	private static final String CERTIFICATE_SETTINGS_KEY = "certificate";
	private static final String CERTIFICATE_TYPE_SETTINGS_KEY = "certificate.type";
	private static final String CERTIFICATE_SIZE_SETTINGS_KEY = "certificate.size";
	private static final String KEYSTORE_TYPE = "PKCS12";
	private static final String PROPERTY_KEY_TYPE = Preferences.PROPS.getString("janus.key.type", "RSA");
	private static final int    PROPERTY_KEY_SIZE = Preferences.PROPS.getInteger("janus.key.size", 2048);
	private static final String TEMPFILE_SUFFIX = "-janus.p12";

	@Getter
	private final File current;

	@SneakyThrows
	private byte[] getSelfSignedCertificateBytes() {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance(PROPERTY_KEY_TYPE);

		generator.initialize(PROPERTY_KEY_SIZE);

		return SelfSignedCertificate.forKeyPair(generator.genKeyPair())
			.cn(RandomStringUtils.randomAscii(PROPERTY_KEY_SIZE))
			.c(Locale.getDefault().getDisplayCountry())
			.generate().getEncoded();
	}

	private boolean isCertificateSettingUnchanged() {
		final String previousType = Preferences.get().get(CERTIFICATE_TYPE_SETTINGS_KEY, null);
		final int previousSize = Preferences.get().getInt(CERTIFICATE_SIZE_SETTINGS_KEY, RandomUtils.nextInt());

		return PROPERTY_KEY_TYPE.equals(current) && PROPERTY_KEY_SIZE == previousSize;
	}

	@SneakyThrows
	private void writeKeyStoreFile(byte[] cert) {
		final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

		keyStore.load(null, null);

		// TODO: Add ability to add multiple domains to the keystore

		keyStore.setCertificateEntry("localhost",
			certFactory.generateCertificate(new ByteArrayInputStream(cert))
		);

		keyStore.store(new FileOutputStream(current), new char[0]);
	}

	public Certificate() throws IOException {
		byte[] cert = Preferences.get().getByteArray(CERTIFICATE_SETTINGS_KEY, null);

		if (cert == null || !isCertificateSettingUnchanged()) {
			cert = getSelfSignedCertificateBytes();
			Preferences.get().putByteArray(CERTIFICATE_SETTINGS_KEY, cert);
		}

		// We write the certificate to a temporary file - spring boot can then directly reference that
		// temporary file for the HTTPS connection of the server. Whenever the application starts, we always
		// create a fresh copy.

		current = File.createTempFile(RandomStringUtils.randomAlphanumeric(12), TEMPFILE_SUFFIX);
		writeKeyStoreFile(cert);
	}
}
