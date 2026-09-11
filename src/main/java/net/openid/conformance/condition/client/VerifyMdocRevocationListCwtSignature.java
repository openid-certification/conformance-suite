package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

/**
 * Verifies the COSE_Sign1 signature on the MSO revocation list using the public key of the leaf
 * certificate of the x5chain in its protected header (ISO/IEC 18013-5 12.3.6.3, for both
 * revocation mechanisms: "The CWT shall contain the x5chain in the protected header that
 * contains the certificate or chain of certificates used to verify the signature of the MSO
 * revocation list").
 */
public class VerifyMdocRevocationListCwtSignature extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_TOKEN })
	public Environment evaluate(Environment env) {
		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);
		CoseSign1 coseSign1 = parsed.coseSign1();

		X509CertChain x5chain = requireProtectedX5chain(coseSign1, "its signature cannot be verified");

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

		X509Cert leaf = x5chain.getCertificates().get(0);
		EcPublicKey publicKey;
		String certSubject;
		try {
			publicKey = leaf.getEcPublicKey();
			certSubject = leaf.getSubject().getName();
		} catch (Exception e) {
			throw error("Failed to extract the public key from the leaf certificate of the MSO"
				+ " revocation list's x5chain", e);
		}

		// ISO/IEC 18013-5 12.3.6.3 pairs each permitted algorithm with the curves it shall be used
		// with; a signature that verifies under another pairing is still non-conformant
		String curveMismatch = MdocUtil.describeCurveMismatch(algorithm, publicKey.getCurve());
		if (curveMismatch != null) {
			throw error("The MSO revocation list's signature algorithm is not permitted with its signer"
				+ " certificate's key: " + curveMismatch,
				args("algorithm", algorithm.name(),
					"curve", MdocUtil.isoCurveName(publicKey.getCurve()),
					"certificate_subject", certSubject));
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
