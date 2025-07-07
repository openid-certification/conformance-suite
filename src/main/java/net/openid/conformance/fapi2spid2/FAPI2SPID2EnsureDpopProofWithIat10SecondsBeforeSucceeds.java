package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.SetDpopIatTo10SecondsInPast;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-dpopproof-with-iat-10seconds-before-succeeds",
	displayName = "FAPI2-Security-Profile-ID2: ensure DPoP proofs with 'iat' claim of 10 seconds in the past succeed",
	summary = "This test makes an authentication request using DPOP proofs with 'iat' claim value of 10 seconds in the past to the PAR/token/resource endpoints are accepted.",
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
public class FAPI2SPID2EnsureDpopProofWithIat10SecondsBeforeSucceeds extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
	}

	@Override
	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		createDpopForParEndpointSteps = () -> CreateDpopProofSteps.createParEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo10SecondsInPast.class).requirement("FAPI2-SP-ID2-5.3.2.1-14"));
		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo10SecondsInPast.class).requirement("FAPI2-SP-ID2-5.3.2.1-14"));
		createDpopForResourceEndpointSteps = () -> CreateDpopProofSteps.createResourceEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo10SecondsInPast.class).requirement("FAPI2-SP-ID2-5.3.2.1-14"));

	}

}
