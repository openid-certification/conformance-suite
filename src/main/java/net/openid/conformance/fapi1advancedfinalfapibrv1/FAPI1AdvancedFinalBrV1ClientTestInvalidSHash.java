package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.condition.as.AddInvalidSHashValueToIdToken;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-br-v1-client-test-invalid-shash",
	displayName = "FAPI1-Advanced-Final-Br-v1: client test - invalid s_hash in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the s_hash value in the id_token does not match the state the client sent",
	profile = "FAPI1-Advanced-Final-Br-v1",
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
public class FAPI1AdvancedFinalBrV1ClientTestInvalidSHash extends AbstractFAPI1AdvancedFinalBrV1ClientExpectNothingAfterIdTokenIssued {

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
		callAndStopOnFailure(AddInvalidSHashValueToIdToken.class, "FAPI1-ADV-5.2.2.1-5");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid s_hash value";
	}
}
