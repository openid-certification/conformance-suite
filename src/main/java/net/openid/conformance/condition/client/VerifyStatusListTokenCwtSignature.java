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
 * Verifies the COSE_Sign1 signature on the MSO revocation list using the public key of the leaf
 * certificate of the x5chain in its protected header (ISO/IEC 18013-5 12.3.6.3: "The CWT shall
 * contain the x5chain in the protected header that contains the certificate or chain of
 * certificates used to verify the signature of the MSO revocation list").
 */
public class VerifyStatusListTokenCwtSignature extends AbstractStatusListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_STATUS_LIST_TOKEN })
	public Environment evaluate(Environment env) {
		ParsedStatusListCwt parsed = parseStatusListCwt(env);
		CoseSign1 coseSign1 = parsed.coseSign1();

		X509CertChain x5chain = getProtectedX5chain(coseSign1);
		if (x5chain == null || x5chain.getCertificates().isEmpty()) {
			throw error("The MSO revocation list does not contain an x5chain in its protected header,"
				+ " so its signature cannot be verified");
		}

		DataItem algItem = coseSign1.getProtectedHeaders().get(new CoseNumberLabel(Cose.COSE_LABEL_ALG));
		if (algItem == null) {
			throw error("The MSO revocation list's COSE_Sign1 protected header does not contain an algorithm");
		}

		Algorithm algorithm;
		try {
			algorithm = Algorithm.Companion.fromCoseAlgorithmIdentifier((int) algItem.getAsNumber());
		} catch (Exception e) {
			throw error("Failed to resolve the MSO revocation list's COSE algorithm identifier", e,
				args("cose_alg_id", algItem.getAsNumber()));
		}

		EcPublicKey publicKey;
		String certSubject;
		try {
			publicKey = x5chain.getCertificates().get(0).getEcPublicKey();
			certSubject = x5chain.getCertificates().get(0).getSubject().getName();
		} catch (Exception e) {
			throw error("Failed to extract the public key from the leaf certificate of the MSO"
				+ " revocation list's x5chain", e);
		}

		try {
			kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) ->
					Cose.INSTANCE.coseSign1Check(publicKey, null, coseSign1, algorithm, continuation)
			);
		} catch (Exception e) {
			throw error("The signature on the MSO revocation list could not be verified using the"
				+ " leaf certificate of its x5chain", e,
				args("algorithm", algorithm.name(), "certificate_subject", certSubject));
		}

		logSuccess("Verified the signature on the MSO revocation list",
			args("algorithm", algorithm.name(),
				"certificate_subject", certSubject,
				"chain_length", x5chain.getCertificates().size()));
		return env;
	}
}
