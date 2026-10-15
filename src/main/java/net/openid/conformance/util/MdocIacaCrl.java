package net.openid.conformance.util;

import java.time.Instant;

/**
 * The certificate revocation list of the suite's mdoc IACA ({@link TestKeysAndCerts#getIacaRootCert()}),
 * which the document signer certificates it issues name in their CRL distribution point. See
 * {@link SuiteCaCrl} for the profile; the IACA keeps its own path because implementations under
 * test that pin the IACA have the path in their configuration.
 */
public final class MdocIacaCrl {

	/** The path the suite serves the CRL at, under its base URLs. */
	public static final String PATH = "/mdoc-iaca.crl";

	/** The media type of a DER encoded CRL, RFC 2585 section 4.2: served here, expected by the validator. */
	public static final String CONTENT_TYPE = "application/pkix-crl";

	private MdocIacaCrl() {
	}

	/** The DER encoded CRL issued now. */
	public static byte[] generate() {
		return SuiteCaCrl.generate(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT);
	}

	/** The DER encoded CRL issued at {@code thisUpdate}. */
	public static byte[] generate(Instant thisUpdate) {
		return SuiteCaCrl.generate(TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT, thisUpdate);
	}
}
