package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddInvalidDpopJktToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-token-endpoint-fails-with-mismatched-dpop-jkt",
	displayName = "FAPI2-Security-Profile-ID2: ensure authorization request with mismatched DPoP authorization code binding fails at the token endpoint",
	summary = "This test makes an authentication request that includes a 'dpop_jkt' that does not match the DPOP Proof key sent to the token endpoint. The Token endpoint must reject the request.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values = { "mtls" })
public class FAPI2SPID2EnsureTokenEndpointFailsWithMismatchedDpopJkt extends AbstractFAPI2SPID2PerformTokenEndpoint {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		// Generate DPOP key for use by AddInvalidDpopJktToAuthorizationEndpointRequest
		callAndStopOnFailure(GenerateDpopKey.class);
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.then(condition(AddInvalidDpopJktToAuthorizationEndpointRequest.class));
	}

	@Override
	protected void processTokenEndpointResponse() {
		callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError.class, Condition.ConditionResult.FAILURE, "DPOP-10.1");
		fireTestFinished();
	}
}
