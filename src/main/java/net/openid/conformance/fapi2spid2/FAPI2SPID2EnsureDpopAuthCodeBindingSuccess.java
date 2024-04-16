package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddDpopJktToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-dpop-auth-code-binding-success",
	displayName = "FAPI2-Security-Profile-ID2: ensure authorization request with a 'dpop_jkt' value that  matches the DPoP proof sent with the PAR and token endpoint requests succeeds",
	summary = "This test makes an authentication request that includes a 'dpop_jkt' which matches the DPOP proof's JWK sent with the PAR and token endpoints to ensure authorization code binding is working correctly.",
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
public class FAPI2SPID2EnsureDpopAuthCodeBindingSuccess extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
		callAndStopOnFailure(GenerateDpopKey.class);
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.then(condition(AddDpopJktToAuthorizationEndpointRequest.class));
	}

}
