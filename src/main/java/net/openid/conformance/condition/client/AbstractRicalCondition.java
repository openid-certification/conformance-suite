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

import java.util.Base64;

/**
 * Shared helpers for conditions operating on the signed RICAL registered in the 'rical'
 * environment object (ISO/IEC 18013-5 second edition draft Annex F). Note Annex F is an
 * informative annex of a CD-ballot draft; these checks may need revisiting when the second
 * edition is published.
 */
public abstract class AbstractRicalCondition extends AbstractCondition {

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
}
