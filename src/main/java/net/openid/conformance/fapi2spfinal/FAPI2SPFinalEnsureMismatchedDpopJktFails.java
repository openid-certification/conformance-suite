package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddInvalidDpopJktToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidDpopProof;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-mismatched-dpop-jkt-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure authorization request with mismatched DPoP authorization code binding fails at the PAR endpoint",
	summary = "This test makes an authentication request that includes a 'dpop_jkt' that does not match the DPOP proof's JWK sent with the PAR request. The PAR endpoint must reject the request.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values = { "mtls" })
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })
public class FAPI2SPFinalEnsureMismatchedDpopJktFails extends AbstractFAPI2SPFinalServerTestModule {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
		// Generate DPOP key for use by AddInvalidDpopJktToAuthorizationEndpointRequest
		callAndStopOnFailure(GenerateDpopKey.class);
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.then(condition(AddInvalidDpopJktToAuthorizationEndpointRequest.class));
	}


	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", "pushed_authorization_endpoint_response");
		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidDpopProof.class, Condition.ConditionResult.FAILURE, "DPOP-10.1");
		fireTestFinished();
	}
}
