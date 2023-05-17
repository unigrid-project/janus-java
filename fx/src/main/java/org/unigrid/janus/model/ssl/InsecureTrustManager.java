package org.unigrid.janus.model.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InsecureTrustManager {
	private static final TrustManager tm = new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String s) {
			log.debug("Accepting a client certificate: " + chain[0].getSubjectX500Principal());
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String s) {
			log.debug("Accepting a server certificate: " + chain[0].getSubjectX500Principal());
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	};

	public static TrustManager[] create() {
		return new TrustManager[] { tm };
	}
}
