package net.openid.conformance.settings;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * A client certificate from the server settings, ready for mTLS: the key matches {@link #leaf()}.
 * The private key belongs to the suite operator and must not be put in a test's environment or log.
 *
 * @param certificateChain leaf first, never empty
 */
public record ClientCertificate(String label, List<X509Certificate> certificateChain, PrivateKey privateKey) {

	public ClientCertificate {
		certificateChain = List.copyOf(certificateChain);
	}

	public X509Certificate leaf() {
		return certificateChain.get(0);
	}

	@Override
	public String toString() {
		// the generated toString would describe the private key
		return "ClientCertificate[label=" + label + ", subject=" + leaf().getSubjectX500Principal().getName() + "]";
	}
}
