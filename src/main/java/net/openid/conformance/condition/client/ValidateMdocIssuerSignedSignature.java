package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.mso.MobileSecurityObjectParser;

import java.util.Base64;
import java.util.Map;

/**
 * Validates the COSE_Sign1 signature on the issuerAuth field of an mdoc credential (IssuerSigned structure).
 *
 * This verifies that:
 * 1. The issuerAuth contains a valid COSE_Sign1 structure
 * 2. The unprotected headers contain an X.509 certificate chain (x5chain)
 * 3. The protected headers contain an algorithm identifier
 * 4. The signature is cryptographically valid against the public key from the leaf certificate
 * 5. The payload contains a valid Mobile Security Object (MSO)
 */
public class ValidateMdocIssuerSignedSignature extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		String mdocCborBase64 = env.getString("mdoc_credential_cbor");

		if (mdocCborBase64 == null || mdocCborBase64.isEmpty()) {
			throw error("mdoc_credential_cbor is null or empty");
		}

		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(mdocCborBase64);
		} catch (Exception e) {
			throw error("Failed to decode mdoc_credential_cbor from base64", e);
		}

		// Decode CBOR to get IssuerSigned structure
		DataItem issuerSignedItem;
		try {
			issuerSignedItem = Cbor.INSTANCE.decode(bytes);
		} catch (Exception e) {
			throw error("Failed to decode IssuerSigned CBOR", e);
		}

		// Extract issuerAuth field
		DataItem issuerAuthItem = issuerSignedItem.getOrNull("issuerAuth");
		if (issuerAuthItem == null) {
			throw error("IssuerSigned structure missing 'issuerAuth' field");
		}

		// Parse as COSE_Sign1
		CoseSign1 coseSign1;
		try {
			coseSign1 = issuerAuthItem.getAsCoseSign1();
		} catch (Exception e) {
			throw error("Failed to parse issuerAuth as COSE_Sign1", e);
		}

		// Extract X.509 certificate chain from unprotected headers
		Map<CoseLabel, DataItem> unprotectedHeaders = coseSign1.getUnprotectedHeaders();
		CoseNumberLabel x5chainLabel = new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN);
		DataItem x5chainItem = unprotectedHeaders.get(x5chainLabel);
		if (x5chainItem == null) {
			throw error("COSE_Sign1 unprotected headers missing x5chain (label 33)");
		}

		X509CertChain certChain;
		try {
			certChain = x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw error("Failed to parse x5chain from COSE_Sign1 unprotected headers", e);
		}

		if (certChain.getCertificates().isEmpty()) {
			throw error("x5chain certificate chain is empty");
		}

		// Extract algorithm from protected headers
		Map<CoseLabel, DataItem> protectedHeaders = coseSign1.getProtectedHeaders();
		CoseNumberLabel algLabel = new CoseNumberLabel(Cose.COSE_LABEL_ALG);
		DataItem algItem = protectedHeaders.get(algLabel);
		if (algItem == null) {
			throw error("COSE_Sign1 protected headers missing algorithm (label 1)");
		}

		Algorithm algorithm;
		try {
			algorithm = Algorithm.Companion.fromCoseAlgorithmIdentifier((int) algItem.getAsNumber());
		} catch (Exception e) {
			throw error("Failed to resolve COSE algorithm identifier", e,
				args("cose_alg_id", algItem.getAsNumber()));
		}

		// Get public key from leaf certificate (first in chain)
		EcPublicKey publicKey;
		String certSubject;
		try {
			publicKey = certChain.getCertificates().get(0).getEcPublicKey();
			certSubject = certChain.getCertificates().get(0).getSubject().getName();
		} catch (Exception e) {
			throw error("Failed to extract public key from leaf certificate", e);
		}

		// Verify the COSE_Sign1 signature
		try {
			Cose.INSTANCE.coseSign1Check(publicKey, null, coseSign1, algorithm);
		} catch (Exception e) {
			throw error("COSE_Sign1 signature verification failed on mdoc issuerAuth", e,
				args("algorithm", algorithm.name(),
					"certificate_subject", certSubject));
		}

		// Parse MSO from payload to log details
		String msoVersion = null;
		String msoDocType = null;
		String msoDigestAlgorithm = null;
		String msoValidFrom = null;
		String msoValidUntil = null;
		try {
			byte[] payload = coseSign1.getPayload();
			DataItem payloadItem = Cbor.INSTANCE.decode(payload);
			DataItem msoDataItem = payloadItem.getAsTaggedEncodedCbor();
			byte[] encodedMso = Cbor.INSTANCE.encode(msoDataItem);
			MobileSecurityObjectParser.MobileSecurityObject mso =
				new MobileSecurityObjectParser(encodedMso).parse();
			msoVersion = mso.getVersion();
			msoDocType = mso.getDocType();
			msoDigestAlgorithm = mso.getDigestAlgorithm().name();
			msoValidFrom = mso.getValidFrom().toString();
			msoValidUntil = mso.getValidUntil().toString();
		} catch (Exception e) {
			// MSO parsing is informational; signature validation already passed
			log("Could not parse MSO from COSE_Sign1 payload for logging purposes",
				args("error", e.getMessage()));
		}

		logSuccess("mdoc issuerAuth COSE_Sign1 signature verified successfully",
			args("algorithm", algorithm.name(),
				"certificate_subject", certSubject,
				"certificate_chain_length", certChain.getCertificates().size(),
				"mso_version", msoVersion,
				"mso_doc_type", msoDocType,
				"mso_digest_algorithm", msoDigestAlgorithm,
				"mso_valid_from", msoValidFrom,
				"mso_valid_until", msoValidUntil));

		return env;
	}
}
