package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.X509CertChain;

import java.util.Map;
import java.util.Set;

/**
 * Validates the COSE_Sign1 envelope of the registered RICAL as per ISO/IEC 18013-5 second
 * edition draft Annex F.3.2: the alg element must be in the protected header and one of the
 * algorithms the annex names (ES256/ES384/ES512/EdDSA), the RICAL signer certificate must be
 * present as an x5chain element in the protected header, and the signature must verify against
 * the public key of the leaf certificate.
 */
public class ValidateRicalSignature extends AbstractRicalCondition {

	// Annex F.3.2: the RICAL provider should use one of these signature algorithms
	private static final Set<String> RECOMMENDED_ALGORITHMS = Set.of("ES256", "ES384", "ES512", "EDDSA");

	// Fully-specified COSE algorithms (which fix the curve as well as the hash) mapped to the
	// Annex F.3.2 name covering the same curve and hash, per the curve pairings of ISO/IEC
	// 18013-5 12.3.4. They are not in Annex F's list, so a reader implementing that list
	// literally may still reject them; the mapping only makes the finding say which listed
	// algorithm the tester most likely meant.
	private static final Map<String, String> FULLY_SPECIFIED_EQUIVALENTS = Map.of(
		"ESP256", "ES256",
		"ESB256", "ES256",
		"ESP384", "ES384",
		"ESB320", "ES384",
		"ESB384", "ES384",
		"ESP512", "ES512",
		"ESB512", "ES512",
		"ED25519", "EdDSA",
		"ED448", "EdDSA");

	@Override
	@PreEnvironment(required = "rical")
	public Environment evaluate(Environment env) {

		byte[] ricalBytes = getRicalBytes(env);

		CoseSign1 coseSign1 = getRicalCoseSign1(ricalBytes);

		CoseNumberLabel algLabel = new CoseNumberLabel(Cose.COSE_LABEL_ALG);
		DataItem algItem = coseSign1.getProtectedHeaders().get(algLabel);
		if (algItem == null) {
			throw error("RICAL COSE_Sign1 protected headers do not contain the algorithm (label 1)");
		}

		Algorithm algorithm;
		try {
			algorithm = Algorithm.Companion.fromCoseAlgorithmIdentifier((int) algItem.getAsNumber());
		} catch (Exception e) {
			throw error("Failed to resolve the RICAL COSE algorithm identifier", e,
				args("cose_alg_id", algItem.getAsNumber()));
		}

		X509CertChain certChain = getRicalSignerCertChain(coseSign1, true);

		EcPublicKey publicKey;
		String certSubject;
		try {
			publicKey = certChain.getCertificates().get(0).getEcPublicKey();
			certSubject = certChain.getCertificates().get(0).getSubject().getName();
		} catch (Exception e) {
			throw error("Failed to extract the public key from the RICAL signer certificate", e);
		}

		try {
			kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Cose.INSTANCE.coseSign1Check(publicKey, null, coseSign1, algorithm, continuation)
			);
		} catch (Exception e) {
			throw error("RICAL COSE_Sign1 signature verification failed", e,
				args("algorithm", algorithm.name(),
					"signer_certificate_subject", certSubject));
		}

		// verified but with an algorithm outside the annex's 'should' list: report it,
		// since a reader-side implementation limited to the listed algorithms would reject
		// this RICAL
		if (!RECOMMENDED_ALGORITHMS.contains(algorithm.name().toUpperCase(java.util.Locale.ROOT))) {
			String listedEquivalent = FULLY_SPECIFIED_EQUIVALENTS.get(algorithm.name());
			String detail = listedEquivalent == null
				? ""
				: ". " + algorithm.name() + " (COSE " + algorithm.getCoseAlgorithmIdentifier()
					+ ") is the fully-specified form of " + listedEquivalent
					+ " but is not in Annex F's list, so a reader accepting only the listed identifiers may reject it";
			throw error("The RICAL signature verified, but uses an algorithm outside the ES256/ES384/ES512/EdDSA set ISO/IEC 18013-5 Annex F says RICAL providers should use" + detail,
				args("algorithm", algorithm.name(),
					"cose_algorithm_identifier", algorithm.getCoseAlgorithmIdentifier(),
					"annex_f_equivalent", listedEquivalent,
					"signer_certificate_subject", certSubject));
		}

		logSuccess("RICAL COSE_Sign1 signature verified",
			args("algorithm", algorithm.name(),
				"signer_certificate_subject", certSubject));

		return env;
	}
}
