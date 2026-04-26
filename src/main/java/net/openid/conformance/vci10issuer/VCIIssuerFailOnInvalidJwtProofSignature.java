package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.VCIGenerateJwtProof;
import net.openid.conformance.vci10issuer.condition.VCIInvalidateJwtProofSignature;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialErrorResponse;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

import java.util.List;

/**
 * Negative test that verifies the issuer properly rejects JWT proofs with invalid signatures.
 * This test invalidates the signature on the JWT proof and expects the issuer to
 * respond with an invalid_proof error.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7.2.1">OID4VCI Section 7.2.1 - JWT Proof Type</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-jwt-proof-signature",
	displayName = "OID4VCI 1.0: Issuer fail on invalid JWT proof signature",
	summary = """
		This test case checks for proper error handling when a JWT proof with an invalid signature is submitted. \
		The test sends a credential request with a JWT proof where the signature has been modified to be invalid. \
		The issuer must reject this request with an invalid_proof error. \
		Note: This test only applies when using jwt proof type. For attestation proof type, the test will be skipped.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidJwtProofSignature extends AbstractVCIIssuerTestModule {

	@Override
	protected List<String> getRequiredProofTypes() {
		return List.of("jwt");
	}

	@Override
	public void start() {
		// Skip this test if the credential configuration doesn't require cryptographic binding
		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			fireTestSkipped("This test requires a credential configuration with cryptographic binding (proof). " +
				"The selected credential configuration does not require proof, so JWT proof signature validation cannot be tested.");
			return;
		}

		// Check if the proof type is jwt
		String proofTypeKey = env.getString("vci_proof_type_key");
		if (!"jwt".equals(proofTypeKey)) {
			fireTestSkipped("This test requires jwt proof type. " +
				"The selected credential configuration uses '" + proofTypeKey + "' proof type.");
			return;
		}

		super.start();
	}

	@Override
	protected ConditionSequence makeGenerateKeyAttestationAndProofSteps() {
		return super.makeGenerateKeyAttestationAndProofSteps()
			.insertAfter(VCIGenerateJwtProof.class,
				condition(VCIInvalidateJwtProofSignature.class)
					.onFail(Condition.ConditionResult.INFO)
					.requirements("OID4VCI-1FINAL-7.2.1")
					.dontStopOnFailure());
	}

	@Override
	protected void verifyCredentialIssuerCredentialResponse() {
		verifyCredentialIssuerCredentialErrorResponse();
		// Expect an error response when JWT proof signature is invalid
		callAndStopOnFailure(new VCIValidateCredentialErrorResponse(VciErrorCode.INVALID_PROOF), "OID4VCI-1FINAL-8.3.1");
	}
}
