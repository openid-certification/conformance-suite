package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckNonceMaximumLength;
import net.openid.conformance.condition.as.CheckNonceMinimumLength;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oid4vp-1final-verifier-happy-flow",
	displayName = "OID4VP-1.0-FINAL Verifier: Happy flow test",
	summary = """
		Expects the verifier to make a valid OID4VP request that matches the configuration. The test creates and returns a valid credential that the verifier should accept.

		Depending on the test configuration the credential may be:
			* SD-JWT VC credential with a vct of urn:eudi:pid:1 as defined in ARF 1.8 ( https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/latest/annexes/annex-3/annex-3.01-pid-rulebook/#5-sd-jwt-vc-based-encoding-of-pid ).
			* An mdl as per ISO 18013-5

		The DCQL query must request only a single credential.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use the authorization endpoint url below instead of 'openid4vp://' and then start the flow in your verifier as normal.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierHappyFlow extends AbstractVP1FinalVerifierTest {

	@Override
	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		super.extractNonceFromAuthorizationEndpointRequestParameters();

		callAndContinueOnFailure(CheckForInvalidCharsInNonce.class, ConditionResult.FAILURE, "OID4VP-ID2-5.2");
		callAndContinueOnFailure(CheckNonceMinimumLength.class, ConditionResult.WARNING);
		callAndContinueOnFailure(CheckNonceMaximumLength.class, ConditionResult.WARNING);
	}
}
