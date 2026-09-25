package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.rical.SignedRical;
import org.multipaz.crypto.X509CertJvmKt;

import java.time.Instant;
import java.util.Base64;

/**
 * Shared helpers for conditions operating on the signed RICAL registered in the 'rical'
 * environment object (ISO/IEC 18013-5 second edition draft Annex F). Note Annex F is an
 * informative annex of a CD-ballot draft; these checks may need revisiting when the second
 * edition is published.
 */
public abstract class AbstractRicalCondition extends AbstractCondition {

	/** The instant the RICAL and the certificate chains are judged against. */
	protected Instant now() {
		return Instant.now();
	}

	/** Decodes the base64 signed RICAL stored in the 'rical' environment object. */
	protected byte[] getRicalBytes(Environment env) {
		String ricalB64 = env.getString("rical", "value");
		if (ricalB64 == null || ricalB64.isEmpty()) {
			throw error("RICAL value is missing from the environment");
		}
		try {
			return Base64.getDecoder().decode(ricalB64);
		} catch (IllegalArgumentException e) {
			throw error("The 'RICAL' field in the test configuration could not be decoded as base64", e);
		}
	}

	/** Parses the signed RICAL bytes as a COSE_Sign1 structure. */
	protected CoseSign1 getRicalCoseSign1(byte[] ricalBytes) {
		try {
			return Cbor.INSTANCE.decode(ricalBytes).getAsCoseSign1();
		} catch (Exception e) {
			throw error("Failed to parse RICAL as a COSE_Sign1 structure", e);
		}
	}

	/**
	 * Extracts the signer certificate x5chain from the RICAL COSE_Sign1 headers. Annex F requires
	 * it in the <em>protected</em> header (the opposite of the VICAL, whose x5chain is
	 * unprotected); with {@code requireProtected} an unprotected-header placement is an error,
	 * otherwise it is tolerated.
	 */
	protected X509CertChain getRicalSignerCertChain(CoseSign1 coseSign1, boolean requireProtected) {
		CoseNumberLabel x5chainLabel = new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN);
		DataItem x5chainItem = coseSign1.getProtectedHeaders().get(x5chainLabel);
		if (x5chainItem == null) {
			DataItem inUnprotected = coseSign1.getUnprotectedHeaders().get(x5chainLabel);
			if (inUnprotected == null) {
				throw error("RICAL COSE_Sign1 headers do not contain the signer certificate x5chain (label 33)");
			}
			if (requireProtected) {
				throw error("The RICAL signer certificate x5chain (label 33) is in the COSE_Sign1 unprotected headers; ISO/IEC 18013-5 Annex F requires it in the protected header");
			}
			x5chainItem = inUnprotected;
		}
		X509CertChain certChain;
		try {
			certChain = x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw error("Failed to parse the x5chain from the RICAL COSE_Sign1 headers", e);
		}
		if (certChain.getCertificates().isEmpty()) {
			throw error("The RICAL x5chain certificate chain is empty");
		}
		return certChain;
	}

	/** Parses a signed RICAL, optionally skipping COSE signature verification. */
	protected SignedRical parseSignedRical(byte[] ricalBytes, boolean disableSignatureVerification) throws InterruptedException {
		return kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> SignedRical.Companion.parse(ricalBytes, disableSignatureVerification, continuation)
		);
	}

	/**
	 * Verifies the RICAL COSE_Sign1 signature against the leaf of the embedded signer x5chain,
	 * for conditions that read the list's entries; {@link ValidateRicalSignature} performs the
	 * same check with finer-grained findings.
	 */
	protected void verifyRicalCoseSignature(CoseSign1 coseSign1) {
		DataItem algItem = coseSign1.getProtectedHeaders().get(new CoseNumberLabel(Cose.COSE_LABEL_ALG));
		if (algItem == null) {
			throw error("RICAL COSE_Sign1 protected headers do not contain the algorithm (label 1)");
		}
		org.multipaz.crypto.Algorithm algorithm;
		org.multipaz.crypto.EcPublicKey publicKey;
		try {
			algorithm = org.multipaz.crypto.Algorithm.Companion.fromCoseAlgorithmIdentifier((int) algItem.getAsNumber());
			publicKey = getRicalSignerCertChain(coseSign1, false).getCertificates().get(0).getEcPublicKey();
		} catch (Exception e) {
			throw error("Failed to extract the RICAL signing algorithm or signer public key", e);
		}
		try {
			kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Cose.INSTANCE.coseSign1Check(publicKey, null, coseSign1, algorithm, continuation)
			);
		} catch (Exception e) {
			throw error("RICAL COSE_Sign1 signature verification failed", e);
		}
	}

	/**
	 * Evaluates a certificate chain against the RICAL's listed reader CAs via multipaz's
	 * RicalTrustManager (Annex F.3.2.6), validating CA validity intervals.
	 */
	protected org.multipaz.trustmanagement.TrustResult verifyChainAgainstRical(
			SignedRical signedRical, java.util.List<X509Cert> chainCerts) {
		try {
			org.multipaz.trustmanagement.RicalTrustManager trustManager =
				new org.multipaz.trustmanagement.RicalTrustManager(signedRical, "rical");
			kotlin.time.Instant atTime = kotlin.time.Instant.Companion.fromEpochMilliseconds(now().toEpochMilli());
			return kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				// true = also validate the validity intervals of CA certificates in the chain
				(scope, continuation) -> trustManager.verify(chainCerts, atTime, true, continuation)
			);
		} catch (Exception e) {
			throw error("Failed to evaluate the certificate chain against the RICAL", e);
		}
	}

	/**
	 * The reason a chain multipaz reports as trusted is not trusted under Annex F, or null when
	 * the verdict stands. F.3.2.6 uses "the CertificateInfo element with isTrustAnchor set to
	 * true that is highest in the certificate validation path" as the trust anchor, and F.3.2.5
	 * requires the RICAL itself to be valid ("if present, notAfter shall not be in the past")
	 * before its entries are used. RicalTrustManager turns every listed entry into a trust
	 * point whatever its isTrustAnchor, and does not consider the list's own validity, so a
	 * chain reaching only an isTrustAnchor=false Sub CA, or reaching an expired list, comes
	 * back trusted.
	 */
	protected String ricalTrustPathDefect(SignedRical signedRical,
			org.multipaz.trustmanagement.TrustResult trustResult) {
		kotlin.time.Instant notAfter = signedRical.getRical().getNotAfter();
		if (notAfter != null && notAfter.toEpochMilliseconds() < now().toEpochMilli()) {
			return "the RICAL's own 'notAfter' (" + notAfter + ") is in the past, so ISO/IEC 18013-5"
				+ " Annex F.3.2.5 does not allow its entries to be used as trust anchors";
		}
		for (org.multipaz.trustmanagement.TrustPoint trustPoint : trustResult.getTrustPoints()) {
			byte[] trustPointSki = trustPoint.getCertificate().getSubjectKeyIdentifier();
			for (org.multipaz.mdoc.rical.RicalCertificateInfo certInfo : signedRical.getRical().getCertificateInfos()) {
				if (certInfo.getCertificate().equals(trustPoint.getCertificate())
					|| (trustPointSki != null
						&& java.util.Arrays.equals(certInfo.getCertificate().getSubjectKeyIdentifier(), trustPointSki))) {
					if (certInfo.isTrustAnchor()) {
						return null;
					}
					break;
				}
			}
		}
		return "no RICAL entry on the chain's trust path has 'isTrustAnchor' set to true, which"
			+ " ISO/IEC 18013-5 Annex F.3.2.6 requires of the certificate used as the trust anchor";
	}

	/**
	 * F.3.2.6: "The CertificateInfo element of the first certificate (bottom-up) in the
	 * certificate chain included in the RICAL shall be used to determine and apply the
	 * associated Trust Constraints." A chain certificate is listed in its own right when an
	 * entry carries that certificate or its Subject Key Identifier (a renewed CA keeping its
	 * key has a new certificate); only if no entry lists it does the entry for its issuer
	 * govern, which is the usual case since the verifier's x5c typically contains just the
	 * end-entity certificate. The issuer is identified by the chain certificate's Authority Key
	 * Identifier, as multipaz's RicalTrustManager does — a subject name alone can be shared by
	 * an old and a renewed CA, or by an unrelated CA, and would name the wrong entry — and is
	 * accepted only when that entry's key actually verifies the chain certificate's signature,
	 * so a key identifier that collides or was copied cannot select an entry that issued
	 * nothing in this chain.
	 */
	/**
	 * Whether the candidate issuer's public key verifies the chain certificate's signature.
	 */
	private boolean issuedChainCertificate(X509Cert candidateIssuer, X509Cert chainCert) {
		try {
			X509CertJvmKt.getJavaX509Certificate(chainCert)
				.verify(X509CertJvmKt.getJavaX509Certificate(candidateIssuer).getPublicKey());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected org.multipaz.mdoc.rical.RicalCertificateInfo findFirstMatchingRicalEntry(
			SignedRical signedRical, java.util.List<X509Cert> chainCerts) {
		for (X509Cert chainCert : chainCerts) {
			byte[] chainCertSki = chainCert.getSubjectKeyIdentifier();
			for (org.multipaz.mdoc.rical.RicalCertificateInfo certInfo : signedRical.getRical().getCertificateInfos()) {
				X509Cert entryCert = certInfo.getCertificate();
				if (entryCert.equals(chainCert)
					|| (chainCertSki != null
						&& java.util.Arrays.equals(entryCert.getSubjectKeyIdentifier(), chainCertSki))) {
					return certInfo;
				}
			}
			byte[] chainCertAki = chainCert.getAuthorityKeyIdentifier();
			if (chainCertAki == null) {
				continue;
			}
			for (org.multipaz.mdoc.rical.RicalCertificateInfo certInfo : signedRical.getRical().getCertificateInfos()) {
				if (java.util.Arrays.equals(certInfo.getCertificate().getSubjectKeyIdentifier(), chainCertAki)
					&& issuedChainCertificate(certInfo.getCertificate(), chainCert)) {
					return certInfo;
				}
			}
		}
		return null;
	}
}
