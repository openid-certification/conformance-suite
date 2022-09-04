package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.RemoveSHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-invalid-missing-shash",
	displayName = "FAPI1-Advanced-Final: client test - missing shash value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the shash in the id_token from the authorization_endpoint is missing",
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
@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {"jarm"})
public class FAPI1AdvancedFinalClientTestInvalidMissingSHash extends AbstractFAPI1AdvancedFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void endTestIfRequiredParametersAreMissing() {

		String shash = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);
		if (shash == null) {
			// This throws an exception: the test will stop here
			fireTestSkipped("This test is being skipped as it relies on the client supplying a state value - since none is supplied, this can not be tested.");
		}
	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveSHashFromIdToken.class, "FAPI1-ADV-5.2.2.1-5");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing s_hash value";
	}
}
