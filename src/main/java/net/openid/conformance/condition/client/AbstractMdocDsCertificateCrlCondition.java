package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.Arrays;
import java.util.Base64;

/**
 * Base for the conditions that fetch and check the CRL named by the CRL distribution point of
 * an mdoc's document signer certificate (ISO/IEC 18013-5 Table B.3), the revocation information
 * 12.8.3 requires an mdoc reader to have access to.
 */
public abstract class AbstractMdocDsCertificateCrlCondition extends AbstractValidateMdocDsCertificate {

	/** The body the CRL distribution point served, base64 encoded; {@link #parseCrl} checks it is one DER CRL. */
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

	private static final byte[] PEM_PREFIX = "-----BEGIN".getBytes(StandardCharsets.US_ASCII);

	/**
	 * The fetched body as an X.509 CRL. RFC 5280 4.2.1.13 requires an http distribution point
	 * URI to point to a single DER encoded CRL (RFC 2585), so the body is required to be exactly
	 * that: not PEM text, not BER that is not DER, and nothing after the one CRL. The JCA
	 * certificate factory alone would accept PEM and read only the first of several concatenated
	 * CRLs.
	 */
	protected X509CRL parseCrl(Environment env) {
		String crlBase64 = env.getString(ENV_CRL);
		if (crlBase64 == null || crlBase64.isEmpty()) {
			throw error(ENV_CRL + " is missing from the environment");
		}
		byte[] body = Base64.getDecoder().decode(crlBase64);
		String uri = env.getString(ENV_URI);

		if (startsWith(body, PEM_PREFIX)) {
			throw error("The document signer certificate's CRL distribution point served PEM text, but a CRL distribution point"
					+ " must serve a single DER encoded CRL",
				args("uri", uri, "body_start", new String(body, 0, Math.min(body.length, 40), StandardCharsets.US_ASCII)));
		}

		ASN1Primitive asn1;
		try {
			asn1 = ASN1Primitive.fromByteArray(body);
		} catch (IOException e) {
			// trailing bytes after the first complete structure are reported as extra data
			throw error("The document signer certificate's CRL distribution point did not serve a single DER encoded CRL: the body"
					+ " is not one ASN.1 structure, so it is not one CRL or has data after the CRL",
				e, args("uri", uri, "length", body.length));
		}
		try {
			byte[] der = asn1.getEncoded(ASN1Encoding.DER);
			if (!Arrays.equals(der, body)) {
				throw error("The document signer certificate's CRL distribution point served a CRL that is not DER encoded: re-encoding"
						+ " it as DER gives different bytes, so it uses BER encoding options DER forbids, such as indefinite or"
						+ " non-minimal lengths",
					args("uri", uri, "length", body.length, "der_length", der.length));
			}
		} catch (IOException e) {
			throw error("The CRL named by the document signer certificate's CRL distribution point could not be re-encoded as DER",
				e, args("uri", uri));
		}

		try {
			return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(body));
		} catch (Exception e) {
			throw error("The CRL named by the document signer certificate's CRL distribution point could not be parsed as an X.509 CRL",
				e, args("uri", uri));
		}
	}

	private static boolean startsWith(byte[] body, byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
