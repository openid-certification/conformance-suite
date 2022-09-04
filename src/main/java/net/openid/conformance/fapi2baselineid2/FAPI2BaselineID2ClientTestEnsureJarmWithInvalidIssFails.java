package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidIssValueToJarm;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-ensure-jarm-with-invalid-iss-fails",
	displayName = "FAPI2-Baseline-ID2: sends a JARM response with an invalid iss claim.",
	summary = "This test should end with the client displaying an error message that the JARM response has an invalid iss claim",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = { "plain_response" })
public class FAPI2BaselineID2ClientTestEnsureJarmWithInvalidIssFails extends AbstractFAPI2BaselineID2ClientExpectNothingAfterAuthorizationResponse {


	@Override
	protected void addCustomValuesToJarmResponse() {
		callAndContinueOnFailure(AddInvalidIssValueToJarm.class, Condition.ConditionResult.FAILURE, "JARM-4.1");
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Added invalid iss to JARM response";
	}
}
