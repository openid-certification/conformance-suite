package net.openid.conformance.vpid2verifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForInvalidCharsInNonce;
import net.openid.conformance.condition.as.CheckNonceMaximumLength;
import net.openid.conformance.condition.as.CheckNonceMinimumLength;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oid4vp-id2-verifier-happy-flow",
	displayName = "OID4VPID2 Verifier: Happy flow test ",
	summary = "Expects the verifier to make a valid OID4VP request that matches the configuration, creates and returns an SD-JWT VC credential using the https://example.bmi.bund.de/credential/pid/1.0 VCT.\n\nThe presentation_definition must contain only one input_descriptor.\n\nThe conformance suite acts as a mock web wallet. You must configure your verifier to use the authorization endpoint url below instead of 'openid4vp://' and then start the flow in your verifier as normal.",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.client_id",
		"credential.signing_jwk"
	}
)
public class VPID2VerifierHappyFlow extends AbstractVPID2VerifierTest {

	@Override
	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		super.extractNonceFromAuthorizationEndpointRequestParameters();

		callAndContinueOnFailure(CheckForInvalidCharsInNonce.class, ConditionResult.FAILURE, "OID4VP-ID2-5.2");
		callAndContinueOnFailure(CheckNonceMinimumLength.class, ConditionResult.WARNING);
		callAndContinueOnFailure(CheckNonceMaximumLength.class, ConditionResult.WARNING);
	}
}
