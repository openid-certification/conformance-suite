package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.RemoveAtHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-missing-athash",
	displayName = "FAPI1-Advanced-Final: client test - a happy flow test where the returned id_token will not have an at_hash value",
	summary = "The returned id_token will not have an at_hash value. at_hash is optional as an access token is not being returned from the authorization endpoint, so the flow should succeed even though at_hash is absent.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)
@VariantNotApplicable(parameter = FAPIClientType.class, values = { "plain_oauth" })
public class FAPI1AdvancedFinalClientTestNoAtHash extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}

}
