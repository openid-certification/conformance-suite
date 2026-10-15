package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.Base64;

/**
 * Base for the conditions that fetch and check the CRL named by the CRL distribution point of
 * an mdoc's document signer certificate (ISO/IEC 18013-5 Table B.3), the revocation information
 * 12.8.3 requires an mdoc reader to have access to.
 */
public abstract class AbstractMdocDsCertificateCrlCondition extends AbstractValidateMdocDsCertificate {

	/** The fetched CRL, base64 encoded DER. */
	public static final String ENV_CRL = "mdoc_ds_certificate_crl";

	/** The distribution point URI the CRL was fetched from. */
	public static final String ENV_URI = "mdoc_ds_certificate_crl_uri";

	/**
	 * Set when the fetched CRL complies with the profile and its signature verified with the
	 * issuing IACA certificate: only then does the CRL say anything about revocation.
	 */
	public static final String ENV_AUTHENTICATED = "mdoc_ds_certificate_crl_authenticated";

	/** The HTTP response that carried the CRL, as {@code convertBinaryResponseForEnvironment} records it. */
	public static final String ENV_RESPONSE = "mdoc_ds_certificate_crl_endpoint_response";

	/**
	 * The document signer certificates whose CRL this test has already fetched and checked, keyed
	 * by the SHA-256 fingerprint of the certificate: the checks depend on the certificate alone, so
	 * a batch of credentials signed by one certificate is checked once.
	 */
	public static final String ENV_CHECKED = "mdoc_ds_certificate_crl_checked";

	protected X509CRL parseCrl(Environment env) {
		String crlBase64 = env.getString(ENV_CRL);
		if (crlBase64 == null || crlBase64.isEmpty()) {
			throw error(ENV_CRL + " is missing from the environment");
		}
		try {
			return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(
				new ByteArrayInputStream(Base64.getDecoder().decode(crlBase64)));
		} catch (Exception e) {
			throw error("The CRL named by the document signer certificate's CRL distribution point could not be parsed as an X.509 CRL",
				e, args("uri", env.getString(ENV_URI)));
		}
	}
}
