package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.AddAudValueAsArrayToIdToken;
import net.openid.conformance.condition.as.SignIdTokenBypassingNimbusChecks;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-valid-aud-as-array",
	displayName = "FAPI1-Advanced-Final: client test - valid aud in id_token as data type array",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
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
@VariantNotApplicable(parameter = FAPIClientType.class, values = "plain_oauth")
public class FAPI1AdvancedFinalClientTestValidAudAsArray extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){

		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class,"OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}
}
