package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.RemoveSHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-missing-shash",
	displayName = "FAPI2-Baseline-ID2: client test - missing shash value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the shash in the id_token from the authorization_endpoint is missing",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)
@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {"jarm"})
public class FAPI2BaselineID2ClientTestInvalidMissingSHash extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

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