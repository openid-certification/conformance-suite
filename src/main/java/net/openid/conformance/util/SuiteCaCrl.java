package net.openid.conformance.util;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * The certificate revocation list of one of the suite's CAs ({@link TestKeysAndCerts.SuiteCa}),
 * which the leaf certificates it issues name in their CRL distribution point. It is minted on
 * each request, signed by the CA, and follows the CRL profile in ISO/IEC 18013-5 Table B.10,
 * which is also a plain RFC 5280 CRL: version 2, issuer equal to the CA subject, ECDSA with
 * SHA-256, a non-critical authority key identifier equal to the CA's subject key identifier, a
 * non-critical, monotonically increasing CRL number, no other extensions, and no
 * revokedCertificates list while nothing is revoked (the list "shall not be present if there
 * are no revoked certificates").
 *
 * <p>The CRL number is the issue time in epoch milliseconds, so it increases across restarts and
 * between instances with no shared state and differs between CRLs minted in the same second. nextUpdate is seven days after thisUpdate, well inside
 * the 90 days B.2 allows between reissues; a verifier that caches until then sees nothing stale,
 * since the suite revokes nothing.
 */
public final class SuiteCaCrl {

	private static final Duration VALIDITY = Duration.ofDays(7);

	private SuiteCaCrl() {
	}

	/** The DER encoded CRL of {@code ca} issued now. */
	public static byte[] generate(TestKeysAndCerts.SuiteCa ca) {
		return generate(ca, Instant.now());
	}

	/** The DER encoded CRL of {@code ca} issued at {@code thisUpdate}. */
	public static byte[] generate(TestKeysAndCerts.SuiteCa ca, Instant thisUpdate) {
		try {
			X509CertificateHolder caHolder = ca.getHolder();
			SubjectKeyIdentifier caSki = SubjectKeyIdentifier.fromExtensions(caHolder.getExtensions());

			X509v2CRLBuilder builder = new X509v2CRLBuilder(caHolder.getSubject(), Date.from(thisUpdate));
			builder.setNextUpdate(Date.from(thisUpdate.plus(VALIDITY)));
			builder.addExtension(Extension.authorityKeyIdentifier, false,
				new AuthorityKeyIdentifier(caSki.getKeyIdentifier()));
			builder.addExtension(Extension.cRLNumber, false,
				new CRLNumber(BigInteger.valueOf(thisUpdate.toEpochMilli())));

			ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(ca.getPrivateKey());
			return builder.build(signer).getEncoded();
		} catch (IOException | org.bouncycastle.operator.OperatorCreationException e) {
			throw new IllegalStateException("Failed to mint the CRL of " + ca.getSlug(), e);
		}
	}
}
