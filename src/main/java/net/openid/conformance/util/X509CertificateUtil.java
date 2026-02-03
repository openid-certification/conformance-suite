package net.openid.conformance.util;

import java.security.cert.X509Certificate;

/**
 * Utility class for X.509 certificate operations.
 */
public class X509CertificateUtil {

	/**
	 * Checks if the given certificate is self-signed.
	 *
	 * A certificate is considered self-signed if it can be verified using its own public key.
	 *
	 * @param certificate the X.509 certificate to check
	 * @return true if the certificate is self-signed, false otherwise
	 */
	public static boolean isSelfSigned(X509Certificate certificate) {
		try {
			certificate.verify(certificate.getPublicKey());
			return true;
		} catch (Exception e) {
			// Verification failed - certificate is NOT self-signed
			return false;
		}
	}

}
