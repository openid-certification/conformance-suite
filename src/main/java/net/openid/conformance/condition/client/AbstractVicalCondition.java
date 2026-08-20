package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.vical.SignedVical;

import java.util.Base64;

/**
 * Shared helpers for conditions operating on the signed VICAL registered in the 'vical'
 * environment object (ISO/IEC 18013-5 Annex C).
 */
public abstract class AbstractVicalCondition extends AbstractCondition {

	/** Decodes the base64 signed VICAL stored in the 'vical' environment object. */
	protected byte[] getVicalBytes(Environment env) {
		String vicalB64 = env.getString("vical", "value");
		if (vicalB64 == null || vicalB64.isEmpty()) {
			throw error("VICAL value is missing from the environment");
		}
		try {
			return Base64.getDecoder().decode(vicalB64);
		} catch (IllegalArgumentException e) {
			throw error("The 'VICAL' field in the test configuration could not be decoded as base64", e);
		}
	}

	/** Parses the signed VICAL bytes as a COSE_Sign1 structure. */
	protected CoseSign1 getVicalCoseSign1(byte[] vicalBytes) {
		try {
			return Cbor.INSTANCE.decode(vicalBytes).getAsCoseSign1();
		} catch (Exception e) {
			throw error("Failed to parse VICAL as a COSE_Sign1 structure", e);
		}
	}

	/**
	 * Extracts the signer certificate x5chain from the VICAL COSE_Sign1 headers. Annex C requires
	 * it as an unprotected header element; with {@code requireUnprotected} a protected-header
	 * placement is an error, otherwise it is tolerated.
	 */
	protected X509CertChain getVicalSignerCertChain(CoseSign1 coseSign1, boolean requireUnprotected) {
		CoseNumberLabel x5chainLabel = new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN);
		DataItem x5chainItem = coseSign1.getUnprotectedHeaders().get(x5chainLabel);
		if (x5chainItem == null) {
			DataItem inProtected = coseSign1.getProtectedHeaders().get(x5chainLabel);
			if (inProtected == null) {
				throw error("VICAL COSE_Sign1 headers do not contain the signer certificate x5chain (label 33)");
			}
			if (requireUnprotected) {
				throw error("The VICAL signer certificate x5chain (label 33) is in the COSE_Sign1 protected headers; ISO/IEC 18013-5 Annex C requires it as an unprotected header element");
			}
			x5chainItem = inProtected;
		}
		X509CertChain certChain;
		try {
			certChain = x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw error("Failed to parse the x5chain from the VICAL COSE_Sign1 headers", e);
		}
		if (certChain.getCertificates().isEmpty()) {
			throw error("The VICAL x5chain certificate chain is empty");
		}
		return certChain;
	}

	/** Parses a signed VICAL, optionally skipping COSE signature verification. */
	protected SignedVical parseSignedVical(byte[] vicalBytes, boolean disableSignatureVerification) throws InterruptedException {
		return kotlinx.coroutines.BuildersKt.runBlocking(
			kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
			(scope, continuation) -> SignedVical.Companion.parse(vicalBytes, disableSignatureVerification, continuation)
		);
	}
}
