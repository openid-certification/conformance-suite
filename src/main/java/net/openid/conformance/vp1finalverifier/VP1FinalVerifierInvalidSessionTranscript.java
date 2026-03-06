package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.InvalidateNonce;
import net.openid.conformance.condition.as.VP1FinalEncryptVPResponse;
import net.openid.conformance.condition.client.BuildUnsignedRequestToDirectPostEndpoint;
import net.openid.conformance.condition.client.CallDirectPostEndpoint;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-session-transcript",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid session transcript",
	summary = """
		Creates a credential with a session transcript that uses the wrong nonce value. \
		The verifier must reject this credential because the mdoc device signature will not \
		verify against the expected session transcript.

		This test is only applicable for the ISO mDL credential format.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
@VariantNotApplicable(parameter = VP1FinalVerifierCredentialFormat.class, values = {"sd_jwt_vc"})
public class VP1FinalVerifierInvalidSessionTranscript extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createIsoMdlSessionTranscript() {
		callAndStopOnFailure(InvalidateNonce.class);
		super.createIsoMdlSessionTranscript();
	}

	@Override
	protected void sendAuthorizationResponseToResponseUri() {
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(BuildUnsignedRequestToDirectPostEndpoint.class);
				break;
			case DIRECT_POST_JWT:
				callAndStopOnFailure(VP1FinalEncryptVPResponse.class);
				break;
		}
		callAndStopOnFailure(CallDirectPostEndpoint.class);

		call(exec().mapKey("endpoint_response", "direct_post_response"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
	}
}
