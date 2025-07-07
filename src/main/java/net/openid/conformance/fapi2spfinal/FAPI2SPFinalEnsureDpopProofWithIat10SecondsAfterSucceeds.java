package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.SetDpopIatTo8SecondsInFuture;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-dpopproof-with-iat-10seconds-after-succeeds",
	displayName = "FAPI2-Security-Profile-Final: ensure DPoP proofs with 'iat' claim of 10 seconds in the future succeed",
	summary = "This test makes an authentication request using DPOP proofs with 'iat' claim value of 10 seconds in the future to the PAR/token/resource endpoints are accepted.",
	profile = "FAPI2-Security-Profile-Final",
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
public class FAPI2SPFinalEnsureDpopProofWithIat10SecondsAfterSucceeds extends AbstractFAPI2SPFinalServerTestModule {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
	}

	@Override
	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		createDpopForParEndpointSteps = () -> CreateDpopProofSteps.createParEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo8SecondsInFuture.class).requirement("FAPI2-SP-FINAL-5.3.2.1-13"));
		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo8SecondsInFuture.class).requirement("FAPI2-SP-FINAL-5.3.2.1-13"));
		createDpopForResourceEndpointSteps = () -> CreateDpopProofSteps.createResourceEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, condition(SetDpopIatTo8SecondsInFuture.class).requirement("FAPI2-SP-FINAL-5.3.2.1-13"));
	}


}
