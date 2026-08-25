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

/**
 * Validates the COSE_Sign1 envelope of the registered VICAL as per ISO/IEC 18013-5 Annex C.1.7.1:
 * the alg element must be in the protected header, the VICAL signer certificate must be present as
 * an x5chain element in the unprotected header, and the signature must verify against the public
 * key of the leaf certificate.
 */
public class ValidateVicalSignature extends AbstractVicalCondition {

	@Override
	@PreEnvironment(required = "vical")
	public Environment evaluate(Environment env) {

		byte[] vicalBytes = getVicalBytes(env);

		CoseSign1 coseSign1 = getVicalCoseSign1(vicalBytes);

		CoseNumberLabel algLabel = new CoseNumberLabel(Cose.COSE_LABEL_ALG);
		DataItem algItem = coseSign1.getProtectedHeaders().get(algLabel);
		if (algItem == null) {
			throw error("VICAL COSE_Sign1 protected headers do not contain the algorithm (label 1)");
		}

		Algorithm algorithm;
		try {
			algorithm = Algorithm.Companion.fromCoseAlgorithmIdentifier((int) algItem.getAsNumber());
		} catch (Exception e) {
			throw error("Failed to resolve the VICAL COSE algorithm identifier", e,
				args("cose_alg_id", algItem.getAsNumber()));
		}

		X509CertChain certChain = getVicalSignerCertChain(coseSign1, true);

		EcPublicKey publicKey;
		String certSubject;
		try {
			publicKey = certChain.getCertificates().get(0).getEcPublicKey();
			certSubject = certChain.getCertificates().get(0).getSubject().getName();
		} catch (Exception e) {
			throw error("Failed to extract the public key from the VICAL signer certificate", e);
		}

		try {
			kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Cose.INSTANCE.coseSign1Check(publicKey, null, coseSign1, algorithm, continuation)
			);
		} catch (Exception e) {
			throw error("VICAL COSE_Sign1 signature verification failed", e,
				args("algorithm", algorithm.name(),
					"signer_certificate_subject", certSubject));
		}

		logSuccess("VICAL COSE_Sign1 signature verified",
			args("algorithm", algorithm.name(),
				"signer_certificate_subject", certSubject));

		return env;
	}
}
