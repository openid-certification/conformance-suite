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
			throw error("The RICAL signature verified, but uses an algorithm outside the ES256/ES384/ES512/EdDSA set ISO/IEC 18013-5 Annex F says RICAL providers should use",
				args("algorithm", algorithm.name(),
					"signer_certificate_subject", certSubject));
		}

		logSuccess("RICAL COSE_Sign1 signature verified",
			args("algorithm", algorithm.name(),
				"signer_certificate_subject", certSubject));

		return env;
	}
}
