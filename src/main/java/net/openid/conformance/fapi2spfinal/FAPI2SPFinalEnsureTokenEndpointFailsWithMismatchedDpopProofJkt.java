package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-token-endpoint-fails-with-mismatched-dpop-proof-jkt",
	displayName = "FAPI2-Security-Profile-Final: ensure authorization request with mismatched DPoP authorization code binding proof keys fails at the token endpoint",
	summary = "This test makes an authentication request that sends DPOP proofs signed with different keys to the PAR and Token endpoint. The Token endpoint must reject the request.",
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
public class FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopProofJkt extends AbstractFAPI2SPFinalPerformTokenEndpoint {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
	}

	@Override
	protected void createDpopForTokenEndpoint() {
		// Generate a new key to overwrite the key created during the PAR endpoint
		callAndStopOnFailure(GenerateDpopKey.class);
		super.createDpopForTokenEndpoint();
	}


	@Override
	protected void processTokenEndpointResponse() {
		callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError.class, Condition.ConditionResult.FAILURE, "DPOP-10.1");
		fireTestFinished();
	}

}
