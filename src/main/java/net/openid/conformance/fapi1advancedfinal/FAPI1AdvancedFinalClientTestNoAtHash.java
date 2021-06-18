package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.RemoveAtHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-missing-athash",
	displayName = "FAPI1-Advanced-Final: client test - id_token without an at_hash value from the authorization_endpoint, should be rejected",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = { "plain_fapi" })
public class FAPI1AdvancedFinalClientTestNoAtHash extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}
	//TODO should not the client stop after receiving an invalid id token? this test should start waiting for timeout?
}
