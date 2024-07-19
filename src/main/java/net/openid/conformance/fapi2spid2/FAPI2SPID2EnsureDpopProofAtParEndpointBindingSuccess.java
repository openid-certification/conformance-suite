package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-dpopproof-at-par-endpoint-binding-success",
	displayName = "FAPI2-Security-Profile-ID2: ensure authorization request with a DPoP proof sent to the PAR endpoint which matches the DPOP proof thumbprint at token endpoint requests succeeds",
	summary = "This test makes an authentication request that sends a DPOP proof to the PAR endpoint. The same JWK which signed the PAR DPOP Proof is used to sign the DPOP proof sent to the token endpoint to ensure authorization code binding is working correctly.",
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
public class FAPI2SPID2EnsureDpopProofAtParEndpointBindingSuccess extends AbstractFAPI2SPID2ServerTestModule {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		useDpopAuthCodeBinding = true;
	}
}
