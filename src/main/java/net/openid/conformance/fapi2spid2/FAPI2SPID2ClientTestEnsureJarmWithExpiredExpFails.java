package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidExpiredExpValueToJarm;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-ensure-jarm-with-expired-exp-fails",
	displayName = "FAPI2-Security-Profile-ID2: sends a JARM response with an expired exp claim.",
	summary = "This test should end with the client displaying an error message that the JARM response has an invalid/expired exp claim",
	profile = "FAPI2-Security-Profile-ID2",
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

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = { "plain_response" })
public class FAPI2SPID2ClientTestEnsureJarmWithExpiredExpFails extends AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse {


	@Override
	protected void addCustomValuesToJarmResponse() {
		callAndContinueOnFailure(AddInvalidExpiredExpValueToJarm.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Added expired exp to JARM response";
	}
}
