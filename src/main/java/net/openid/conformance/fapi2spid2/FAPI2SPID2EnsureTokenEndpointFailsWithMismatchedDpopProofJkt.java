package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-token-endpoint-fails-with-mismatched-dpop-proof-jkt",
	displayName = "FAPI2-Security-Profile-ID2: ensure authorization request with mismatched DPoP authorization code binding proof keys fails at the token endpoint",
	summary = "This test makes an authentication request that sends DPOP proofs signed with different keys to the PAR and Token endpoint. The Token endpoint must reject the request.",
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
public class FAPI2SPID2EnsureTokenEndpointFailsWithMismatchedDpopProofJkt extends AbstractFAPI2SPID2PerformTokenEndpoint {

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
