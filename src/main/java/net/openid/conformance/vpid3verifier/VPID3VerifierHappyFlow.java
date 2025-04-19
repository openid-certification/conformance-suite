package net.openid.conformance.vpid3verifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckNonceMaximumLength;
import net.openid.conformance.condition.as.CheckNonceMinimumLength;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oid4vp-id3-verifier-happy-flow",
	displayName = "OID4VPID3+draft24 Verifier: Happy flow test",
	summary = "Expects the verifier to make a valid OID4VP request that matches the configuration, creates and returns an SD-JWT VC credential with a vct of urn:eudi:pid:1 as defined in ARF 1.8 ( https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/latest/annexes/annex-3/annex-3.01-pid-rulebook/#5-sd-jwt-vc-based-encoding-of-pid ).\n\nThe presentation_definition or DCQL must contain only one input_descriptor.\n\nThe conformance suite acts as a mock web wallet. You must configure your verifier to use the authorization endpoint url below instead of 'openid4vp://' and then start the flow in your verifier as normal.",
	profile = "OID4VP-ID3",
	configurationFields = {
		"client.client_id",
		"credential.signing_jwk"
	}
)
public class VPID3VerifierHappyFlow extends AbstractVPID3VerifierTest {

	@Override
	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		super.extractNonceFromAuthorizationEndpointRequestParameters();

		callAndContinueOnFailure(CheckForInvalidCharsInNonce.class, ConditionResult.FAILURE, "OID4VP-ID2-5.2");
		callAndContinueOnFailure(CheckNonceMinimumLength.class, ConditionResult.WARNING);
		callAndContinueOnFailure(CheckNonceMaximumLength.class, ConditionResult.WARNING);
	}
}
