package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.AddAudValueAsArrayToIdToken;
import net.openid.conformance.condition.as.SignIdTokenBypassingNimbusChecks;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-valid-aud-as-array",
	displayName = "FAPI2-Security-Profile-Final: client test - valid aud in id_token as data type array",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = FAPIClientType.class, values = "plain_oauth")
public class FAPI2SPFinalClientTestValidAudAsArray extends AbstractFAPI2SPFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){

		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class,"OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}
}
