/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.core;

import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;

/** A signing key made up for one test, so signatures exist without the foundation's secret key. */
public final class SigningKey {
	private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

	private final PGPKeyPair pair;

	@SneakyThrows
	public SigningKey() {
		pair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.EDDSA_LEGACY,
			KeyPairGenerator.getInstance("Ed25519", PROVIDER).generateKeyPair(), new Date());
	}

	@SneakyThrows
	public PGPPublicKeyRingCollection ring() {
		return new PGPPublicKeyRingCollection(List.of(new PGPPublicKeyRing(List.of(pair.getPublicKey()))));
	}

	@SneakyThrows
	public byte[] sign(final byte[] data) {
		final PGPSignatureGenerator generator = new PGPSignatureGenerator(
			new JcaPGPContentSignerBuilder(pair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256)
				.setProvider(PROVIDER), pair.getPublicKey());
		final ByteArrayOutputStream armored = new ByteArrayOutputStream();

		generator.init(PGPSignature.BINARY_DOCUMENT, pair.getPrivateKey());
		generator.update(data);

		try (ArmoredOutputStream armor = new ArmoredOutputStream(armored)) {
			generator.generate().encode(armor);
		}

		return armored.toByteArray();
	}
}
