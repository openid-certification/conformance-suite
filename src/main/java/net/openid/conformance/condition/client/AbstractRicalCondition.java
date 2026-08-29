package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
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

	/** A leniently parsed RICAL, recording whether mis-encoded fields had to be normalized. */
	protected static final class LenientRical {
		public final SignedRical signedRical;
		public final boolean serialNumbersNormalized;

		LenientRical(SignedRical signedRical, boolean serialNumbersNormalized) {
			this.signedRical = signedRical;
			this.serialNumbersNormalized = serialNumbersNormalized;
		}
	}

	/**
	 * Parses a signed RICAL without signature verification, falling back to normalizing
	 * mis-encoded per-entry serialNumber fields when multipaz's strict parser rejects the list.
	 * Real-world RICALs (e.g. the Geneva 2026 interop list) encode a serial number whose first
	 * byte is 0x80 or above with the negative-bignum tag by mistake, and a single such entry
	 * otherwise makes the whole list unusable. Normalization replaces the serialNumber with the
	 * biguint encoding of the embedded certificate's actual serial number; the defect is still
	 * reported by {@link ValidateRicalStructure}.
	 */
	protected LenientRical parseSignedRicalLenient(byte[] ricalBytes) throws InterruptedException {
		try {
			return new LenientRical(parseSignedRical(ricalBytes, true), false);
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception strictFailure) {
			byte[] normalized;
			try {
				normalized = normalizeSerialNumbers(getRicalCoseSign1(ricalBytes));
			} catch (Exception e) {
				// report the strict parser's failure: the normalization attempt failing just
				// means the list has problems beyond the one defect normalization repairs
				throw error("Failed to parse RICAL as a COSE_Sign1-signed RICAL structure", strictFailure);
			}
			try {
				return new LenientRical(parseSignedRical(normalized, true), true);
			} catch (Exception e) {
				throw error("Failed to parse RICAL as a COSE_Sign1-signed RICAL structure", strictFailure);
			}
		}
	}

	/**
	 * Verifies the RICAL COSE_Sign1 signature against the leaf of the embedded signer x5chain.
	 * Used by conditions that must not trust a lenient re-encoding without checking the
	 * original bytes first; {@link ValidateRicalSignature} performs the same check with
	 * finer-grained findings.
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
	 * Re-encodes the RICAL payload with each certificateInfos entry's serialNumber replaced by
	 * the biguint encoding of the embedded certificate's serial number, unless it already is a
	 * biguint. The COSE signature of the returned bytes is NOT valid for the rewritten payload;
	 * callers must verify the signature on the original bytes and parse the result with
	 * signature verification disabled.
	 */
	private byte[] normalizeSerialNumbers(CoseSign1 coseSign1) {
		DataItem rical = Cbor.INSTANCE.decode(coseSign1.getPayload());
		for (DataItem entry : rical.get("certificateInfos").getAsArray()) {
			DataItem serialItem = entry.getOrNull("serialNumber");
			if (serialItem instanceof Tagged && ((Tagged) serialItem).getTagNumber() == Tagged.UNSIGNED_BIGNUM) {
				continue;
			}
			X509Cert cert = X509Cert.Companion.fromDataItem(entry.get("certificate"));
			byte[] serial = cert.getSerialNumber().getValue();
			int offset = 0;
			while (offset < serial.length - 1 && serial[offset] == 0) {
				offset++;
			}
			byte[] unsigned = java.util.Arrays.copyOfRange(serial, offset, serial.length);
			entry.getAsMap().put(new Tstr("serialNumber"), new Tagged(Tagged.UNSIGNED_BIGNUM, new Bstr(unsigned)));
		}
		CoseSign1 rebuilt = new CoseSign1(coseSign1.getProtectedHeaders(), coseSign1.getUnprotectedHeaders(),
			coseSign1.getSignature(), Cbor.INSTANCE.encode(rical));
		return Cbor.INSTANCE.encode(rebuilt.toDataItem());
	}
}
